/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.sakaiproject.nakamura.files.pool;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import junit.framework.Assert;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;

import java.security.Principal;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

public class ContentPoolProviderTest {

  @Mock
  private ClusterTrackingService clusterTrackingService;
  @Mock
  private ComponentContext componentContext;
  @Mock
  private ResourceResolver resourceResolver;
  @Mock
  private Resource resource;
  @Mock
  private SlingRepository slingRepository;
  @Mock
  private JackrabbitSession adminSession;
  @Mock
  private JackrabbitSession userSession;
  @Mock
  private PrincipalManager principalManager;
  @Mock
  private Principal iebPrincipal;
  @Mock
  private Node genericNode;
  @Mock
  private AccessControlManager accessControlManager;
  @Mock
  private Privilege allPrivilege;
  @Mock
  private AccessControlList accessControlList;
  private String lastResolvedPath;

  public ContentPoolProviderTest() {
    MockitoAnnotations.initMocks(this);
  }

  @SuppressWarnings(value={"DLS_DEAD_LOCAL_STORE"},justification="Unit testing fail mode")
  @Test
  public void testNonExisting() {
    ContentPoolProvider cp = new ContentPoolProvider();
    cp.clusterTrackingService = clusterTrackingService;
    Mockito.when(clusterTrackingService.getCurrentServerId()).thenReturn("serverID");
    cp.activate(componentContext);
    Mockito.when(resourceResolver.resolve(Mockito.anyString())).thenReturn(resource);
    Mockito
    .when(resourceResolver.resolve(Mockito.eq("/_p/j/yy/qe/u1/nonexisting")))
    .thenReturn(new NonExistingResource(resourceResolver, "/_p/aa/bb/cc/nonexisting"));
    Mockito.when(resource.getPath()).thenReturn("/_p/AA/BB/CC/DD/testing");
    ResourceMetadata resourceMetadata = new ResourceMetadata();
    Mockito.when(resource.getResourceMetadata()).thenReturn(resourceMetadata);

    Resource result = cp.getResource(resourceResolver, "/");
    Assert.assertNull(result);
    result = cp.getResource(resourceResolver, "/_");
    Assert.assertNull(result);
    try {
      result = cp.getResource(resourceResolver, "/p/nonexisting");
      Assert.fail("Should have refused to create a none existing resource ");
    } catch (SlingException e) {

    }

  }

  
  @Test
  public void testCreate() throws RepositoryException {

    // activate
    Mockito.when(clusterTrackingService.getCurrentServerId()).thenReturn("serverID");

    // setup to create new node
    Mockito.when(slingRepository.loginAdministrative(null)).thenReturn(adminSession);
    Mockito.when(resourceResolver.adaptTo(Session.class)).thenReturn(userSession);
    Mockito.when(userSession.getUserID()).thenReturn("ieb");
    Mockito.when(userSession.getPrincipalManager()).thenReturn(principalManager);
    Mockito.when(adminSession.getAccessControlManager()).thenReturn(accessControlManager);
    Mockito.when(principalManager.getPrincipal("ieb")).thenReturn(iebPrincipal);
    
    // deep create
    Mockito.when(adminSession.itemExists(Mockito.anyString())).thenReturn(true);
    Mockito.when(adminSession.getItem(Mockito.anyString())).thenReturn(genericNode);
    
    // access control utils
    Mockito.when(accessControlManager.privilegeFromName(Mockito.anyString())).thenReturn(allPrivilege);
    AccessControlPolicy[] acp = new AccessControlPolicy[] {accessControlList};
    Mockito.when(accessControlManager.getPolicies(Mockito.anyString())).thenReturn(acp);
    AccessControlEntry[] ace = new AccessControlEntry[0];
    Mockito.when(accessControlList.getAccessControlEntries()).thenReturn(ace);
    
    // saving
    Mockito.when(adminSession.hasPendingChanges()).thenReturn(true);
    
   
    ArgumentMatcher<String> matcher = new ArgumentMatcher<String>() {
      public boolean matches(Object argument) {
        setLastResolvedPath((String) argument);
        return true;
      }
    };
    Mockito.when(resourceResolver.resolve(Mockito.argThat(matcher))).thenReturn(resource);
    Mockito.when(resource.getPath()).thenAnswer(new Answer<String>() {

      public String answer(InvocationOnMock invocation) throws Throwable {
        return lastResolvedPath;
      }
    });
    ResourceMetadata resourceMetadata = new ResourceMetadata();
    Mockito.when(resource.getResourceMetadata()).thenReturn(resourceMetadata);

    
    ContentPoolProvider cp = new ContentPoolProvider();
    cp.clusterTrackingService = clusterTrackingService;
    cp.slingRepository = slingRepository;
    cp.activate(componentContext);
    Resource result = cp.getResource(resourceResolver, "/p");
    
    
    Assert.assertEquals(resource, result);

  }

  protected void setLastResolvedPath(String argument) {
    this.lastResolvedPath = argument;
  }

  @Test
  public void testNonMatching() {
    ContentPoolProvider cp = new ContentPoolProvider();
    cp.clusterTrackingService = clusterTrackingService;
    Mockito.when(clusterTrackingService.getCurrentServerId()).thenReturn("serverID");
    cp.activate(componentContext);
    Mockito.when(resourceResolver.resolve(Mockito.anyString())).thenReturn(resource);
    Mockito.when(resource.getPath()).thenReturn("/_p/AA/BB/CC/DD/testing");
    ResourceMetadata resourceMetadata = new ResourceMetadata();
    Mockito.when(resource.getResourceMetadata()).thenReturn(resourceMetadata);

    Resource result = cp.getResource(resourceResolver, "/");
    Assert.assertNull(result);
    result = cp.getResource(resourceResolver, "/_");
    Assert.assertNull(result);
    result = cp.getResource(resourceResolver, "/p/");
    Assert.assertNull(result);

  }

  public void testProviderWithId(String selectors, String extra) {
    ContentPoolProvider cp = new ContentPoolProvider();
    cp.clusterTrackingService = clusterTrackingService;
    Mockito.when(clusterTrackingService.getCurrentServerId()).thenReturn("serverID");
    cp.activate(componentContext);

    Mockito.when(resourceResolver.resolve(Mockito.matches("/_p/.*/testing" + selectors)))
        .thenReturn(resource);
    Mockito.when(resource.getPath()).thenReturn("/_p/AA/BB/CC/DD/testing");
    ResourceMetadata resourceMetadata = new ResourceMetadata();
    Mockito.when(resource.getResourceMetadata()).thenReturn(resourceMetadata);

    Resource result = cp.getResource(resourceResolver, "/p/testing" + selectors + extra);
    Assert.assertEquals(resource, result);
  }

  @Test
  public void testProviderBlank() {
    testProviderWithId("", "");

  }

  @Test
  public void testProviderExt() {
    testProviderWithId(".json", "");

  }

  @Test
  public void testProviderExtAndSelector() {
    testProviderWithId(".tidy.json", "");

  }

  @Test
  public void testProviderExtAndSelectorAndExtra() {
    testProviderWithId(".tidy.json", "/some/other/path/with.dots.in.it.pdf");
  }

}
