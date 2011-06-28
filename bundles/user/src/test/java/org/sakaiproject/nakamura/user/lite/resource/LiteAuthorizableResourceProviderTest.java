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
package org.sakaiproject.nakamura.user.lite.resource;

import junit.framework.Assert;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.SessionAdaptable;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;

import java.io.IOException;
import java.util.Iterator;


/**
 */
public class LiteAuthorizableResourceProviderTest {
  
  private Repository repository;
  @Mock
  private ResourceResolver resourceResolver;


  public LiteAuthorizableResourceProviderTest() throws ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException, IOException {
    repository = RepositoryHelper.getRepository(new String[]{ "ieb"}, new String[]{"g-course101"} );
    MockitoAnnotations.initMocks(this);
  }

  @Before
  public void before() throws ClientPoolException, StorageClientException, AccessDeniedException {
    
    javax.jcr.Session jcrSession = Mockito.mock(javax.jcr.Session.class, Mockito.withSettings().extraInterfaces(SessionAdaptable.class));
    Mockito.when(((SessionAdaptable)jcrSession).getSession()).thenReturn(repository.loginAdministrative("ieb"));
    Mockito.when(resourceResolver.adaptTo(javax.jcr.Session.class)).thenReturn(jcrSession);

  }

  @Test
  public void testSyntheticResources() {


    LiteAuthorizableResourceProvider srp = new LiteAuthorizableResourceProvider();
    Resource resource = srp.getResource(resourceResolver,
        LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PATH);
    Assert.assertEquals("sparse/users", resource.getResourceType());
    resource = srp.getResource(resourceResolver,
        LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH);
    Assert.assertEquals("sparse/groups", resource.getResourceType());
    resource = srp.getResource(resourceResolver,
        LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_PATH);
    Assert.assertEquals("sparse/userManager", resource.getResourceType());

  }

  @Test
  public void testUserResolution() {
    LiteAuthorizableResourceProvider srp = new LiteAuthorizableResourceProvider();
    Resource resource = srp.getResource(resourceResolver,
        LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX + "ieb");
    Assert.assertNotNull(resource);
  }

  @Test
  public void testGroupResolution()  {
    LiteAuthorizableResourceProvider srp = new LiteAuthorizableResourceProvider();
    Resource resource = srp.getResource(resourceResolver,
        LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
            + "g-course101");
    Assert.assertNotNull(resource);
  }

  @Test
  public void testBadGroupResolution()  {
    LiteAuthorizableResourceProvider srp = new LiteAuthorizableResourceProvider();
    Resource resource = srp.getResource(resourceResolver,
        LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
            + "g-course101/ieb");
    Assert.assertNull(resource);
  }

  @Test
  public void testBadResolution()  {
    LiteAuthorizableResourceProvider srp = new LiteAuthorizableResourceProvider();
    Resource resource = srp.getResource(resourceResolver, "g-course101/ieb");
    Assert.assertNull(resource);
  }

  @Test
  public void testFailedGroupResolution() {
      LiteAuthorizableResourceProvider srp = new LiteAuthorizableResourceProvider();
      Assert.assertNull(srp.getResource(resourceResolver,
          LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
              + "g-course101failed"));
  }
  
  @Test
  public void testListAuthorizableChildren() {
    Resource parent = Mockito.mock(Resource.class);
    Mockito.when(parent.getPath()).thenReturn(LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_PATH);
    Mockito.when(parent.getResourceResolver()).thenReturn(resourceResolver);
    LiteAuthorizableResourceProvider srp = new LiteAuthorizableResourceProvider();
    Iterator<Resource> resources  = srp.listChildren(parent);
    int res = 0;
    int i = 0;
    for ( ; resources.hasNext(); ) {
      Resource resource = resources.next();
      if ( LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH.equals(resource.getPath()) ) {
        res = res | 1;
      }
      if ( LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PATH.equals(resource.getPath()) ) {
        res = res | 2;
      }
      i++;
    }
    Assert.assertEquals(2, i);
    Assert.assertEquals(3, res);
  }
  @Test
  public void testSearchUserChildren()  {
    Resource parent = Mockito.mock(Resource.class);
    
    Mockito.when(parent.getPath()).thenReturn(LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PATH);
    Mockito.when(parent.getResourceResolver()).thenReturn(resourceResolver);
 
    LiteAuthorizableResourceProvider srp = new LiteAuthorizableResourceProvider();
    Iterator<Resource> resources  = srp.listChildren(parent);
    Assert.assertNull(resources);
    
  }

  
  @Test
  public void testSearchGroupChildren()  {
    Resource parent = Mockito.mock(Resource.class);
    
    Mockito.when(parent.getPath()).thenReturn(LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH);
    Mockito.when(parent.getResourceResolver()).thenReturn(resourceResolver);
 
    
    LiteAuthorizableResourceProvider srp = new LiteAuthorizableResourceProvider();
    Iterator<Resource> resources  = srp.listChildren(parent);
    Assert.assertNull(resources);

  }
}
