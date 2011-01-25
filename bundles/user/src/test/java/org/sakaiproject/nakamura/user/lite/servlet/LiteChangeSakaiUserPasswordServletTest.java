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
package org.sakaiproject.nakamura.user.lite.servlet;

import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.SessionAdaptable;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.user.lite.resource.LiteAuthorizableResourceProvider;
import org.sakaiproject.nakamura.user.lite.resource.RepositoryHelper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;


/**
 * We only test we can create the servlet since its fully tested by Sling already
 */
public class LiteChangeSakaiUserPasswordServletTest {

  
  @Mock
  private SlingHttpServletRequest request;
  
  @Mock
  private ResourceResolver resourceResolver;

  @Mock
  private SlingHttpServletResponse httpResponse;

  @Mock
  private Resource resource;

  private Repository repository;
  
  private Session session;



  public LiteChangeSakaiUserPasswordServletTest() throws ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException {
    repository = RepositoryHelper.getRepository(new String[]{ "ieb","jeff","joe"}, new String[]{"g-course101"} );
    MockitoAnnotations.initMocks(this);
  }

  @Before
  public void before() throws ClientPoolException, StorageClientException, AccessDeniedException {
    
    javax.jcr.Session jcrSession = Mockito.mock(javax.jcr.Session.class, Mockito.withSettings().extraInterfaces(SessionAdaptable.class));
    session = repository.loginAdministrative("ieb");
    Mockito.when(((SessionAdaptable)jcrSession).getSession()).thenReturn(session);
    Mockito.when(resourceResolver.adaptTo(javax.jcr.Session.class)).thenReturn(jcrSession);
    when(request.getRemoteUser()).thenReturn("ieb");
    when(request.getResourceResolver()).thenReturn(resourceResolver);

  }
  
  @Test
  public void test() throws ServletException, IOException {
    LiteChangeSakaiUserPasswordServlet servlet = new LiteChangeSakaiUserPasswordServlet();
    when(request.getParameter("oldPwd")).thenReturn("test");
    when(request.getParameter("newPwd")).thenReturn("test1");
    when(request.getParameter("newPwdConfirm")).thenReturn("test1");
    when(request.getResource()).thenReturn(resource);
    when(resource.getPath()).thenReturn(LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX+"ieb");
    when(httpResponse.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

    servlet.doPost(request, httpResponse);
    
  }
}
