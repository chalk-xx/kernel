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

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.kahadb.util.ByteArrayInputStream;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import javax.servlet.ServletException;

public class CreateContentPoolServletTest {

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
  private Node parentNode;
  @Mock
  private Node resourceNode;
  @Mock
  private AccessControlManager accessControlManager;
  @Mock
  private Privilege allPrivilege;
  @Mock
  private AccessControlList accessControlList;
  @Mock
  private ValueFactory valueFactory;
  @Mock
  private Binary binary;
  @Mock
  private ClusterTrackingService clusterTrackingService;
  @Mock
  private ComponentContext componentContext;
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private SlingHttpServletResponse response;
  @Mock
  private RequestParameterMap requestParameterMap;
  @Mock
  private RequestParameter requestParameter1;
  @Mock
  private RequestParameter requestParameter2;
  @Mock
  private RequestParameter requestParameterNot;

  public CreateContentPoolServletTest() {
    MockitoAnnotations.initMocks(this);
  }


  
  @Test
  public void testCreate() throws RepositoryException, ServletException, IOException, JSONException {

    // activate
    Mockito.when(clusterTrackingService.getCurrentServerId()).thenReturn("serverID");
    Mockito.when(slingRepository.loginAdministrative(null)).thenReturn(adminSession);
    Mockito.when(request.adaptTo(ResourceResolver.class)).thenReturn(resourceResolver);
    Mockito.when(resourceResolver.adaptTo(Session.class)).thenReturn(userSession);
    
    Mockito.when(userSession.getPrincipalManager()).thenReturn(principalManager);
    Mockito.when(adminSession.getAccessControlManager()).thenReturn(accessControlManager);
    Mockito.when(userSession.getUserID()).thenReturn("ieb");
    Mockito.when(principalManager.getPrincipal("ieb")).thenReturn(iebPrincipal);
    
    
    
    Mockito.when(request.getRequestParameterMap()).thenReturn(requestParameterMap);
    Map<String, RequestParameter[]> map = new HashMap<String, RequestParameter[]>();
    
    RequestParameter[] requestParameters = new RequestParameter[] {
        requestParameter1,
        requestParameterNot,
        requestParameter2,
    };
    map.put("files", requestParameters);
   
    Mockito.when(requestParameterMap.entrySet()).thenReturn(map.entrySet());
    
    Mockito.when(requestParameter1.isFormField()).thenReturn(false);
    Mockito.when(requestParameter1.getContentType()).thenReturn("application/pdf");
    Mockito.when(requestParameter1.getFileName()).thenReturn("testfilename.pdf");
    InputStream input1 = new ByteArrayInputStream(new byte[10]);
    Mockito.when(requestParameter1.getInputStream()).thenReturn(input1);
    
    Mockito.when(requestParameter2.isFormField()).thenReturn(false);
    Mockito.when(requestParameter2.getContentType()).thenReturn("text/html");
    Mockito.when(requestParameter2.getFileName()).thenReturn("index.html");
    InputStream input2 = new ByteArrayInputStream(new byte[10]);
    Mockito.when(requestParameter2.getInputStream()).thenReturn(input2);

    Mockito.when(requestParameterNot.isFormField()).thenReturn(true);

    
    
    
    // deep create
    Mockito.when(adminSession.itemExists(Mockito.anyString())).thenReturn(true);
    Mockito.when(adminSession.getItem(Mockito.anyString())).thenReturn(parentNode,resourceNode);
    Mockito.when(parentNode.getPath()).thenReturn("/_p/hashedpath/id");
    Mockito.when(resourceNode.getPath()).thenReturn("/_p/hashedpath/id/"+JcrConstants.JCR_CONTENT);
    Mockito.when(adminSession.getValueFactory()).thenReturn(valueFactory);
    Mockito.when(valueFactory.createBinary(Mockito.any(InputStream.class))).thenReturn(binary);
    
    // access control utils
    Mockito.when(accessControlManager.privilegeFromName(Mockito.anyString())).thenReturn(allPrivilege);
    AccessControlPolicy[] acp = new AccessControlPolicy[] {accessControlList};
    Mockito.when(accessControlManager.getPolicies(Mockito.anyString())).thenReturn(acp);
    AccessControlEntry[] ace = new AccessControlEntry[0];
    Mockito.when(accessControlList.getAccessControlEntries()).thenReturn(ace);
    
    // saving
    Mockito.when(adminSession.hasPendingChanges()).thenReturn(true);
    
    StringWriter stringWriter = new StringWriter();
    Mockito.when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));
    
   

    
    CreateContentPoolServlet cp = new CreateContentPoolServlet();
    cp.clusterTrackingService = clusterTrackingService;
    cp.slingRepository = slingRepository;
    cp.activate(componentContext);
    
    cp.doPost(request, response);
    
    JSONObject jsonObject = new JSONObject(stringWriter.toString());
    Assert.assertNotNull(jsonObject.getString("testfilename.pdf"));
    Assert.assertNotNull(jsonObject.getString("index.html"));
    Assert.assertEquals(2, jsonObject.length());
    
    

  }



}
