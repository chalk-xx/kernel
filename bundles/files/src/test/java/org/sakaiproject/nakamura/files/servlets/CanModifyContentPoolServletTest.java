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
package org.sakaiproject.nakamura.files.servlets;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_MANAGER;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_VIEWER;

import com.google.common.collect.ImmutableMap;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;
import org.sakaiproject.nakamura.lite.RepositoryImpl;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class CanModifyContentPoolServletTest {
  private CanModifyContentPoolServlet servlet;
  private SlingHttpServletRequest request;
  private SlingHttpServletResponse response;
  private PrintWriter writer;
  private RequestParameter verbose;
  private Resource resource;
  private RequestPathInfo pathInfo;
  private String[] selectors;
  private RepositoryImpl sparseRepository;
  private Session sparseSession;
  private Content content;

  @Before
  public void setUp() throws  IOException, ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException {
    String path = "/p/qrstuv";
    servlet = new CanModifyContentPoolServlet();
    request = mock(SlingHttpServletRequest.class);
    response = mock(SlingHttpServletResponse.class);
    BaseMemoryRepository baseMemoryRepository = new BaseMemoryRepository();
    sparseRepository = baseMemoryRepository.getRepository();
    sparseSession = sparseRepository.loginAdministrative();
    sparseSession.getAuthorizableManager().createUser("ieb", "Ian Boston", "test",
        ImmutableMap.of("x", (Object) "y"));
    sparseSession.getContentManager().update(
        new Content("pooled-content-id", ImmutableMap.of("x", (Object) "y",
            POOLED_CONTENT_USER_MANAGER, "alice,ieb", POOLED_CONTENT_USER_VIEWER,
            "bob,mark,john")));
    sparseSession.getAccessControlManager().setAcl(
        Security.ZONE_CONTENT,
        "pooled-content-id",
        new AclModification[] { new AclModification(AclModification.grantKey("ieb"),
            Permissions.CAN_MANAGE.getPermission(), Operation.OP_REPLACE) });
    sparseSession.logout();
    sparseSession = sparseRepository.loginAdministrative("ieb");

    ContentManager contentManager = sparseSession.getContentManager();
    content = contentManager.get("pooled-content-id");
    resource = mock(Resource.class);
    when(request.getResource()).thenReturn(resource);
    when(resource.adaptTo(Session.class)).thenReturn(sparseSession);
    
    verbose = mock(RequestParameter.class);
    when(verbose.getString()).thenReturn("true");
    when(request.getRequestParameter("verbose")).thenReturn(verbose);



    pathInfo = mock(RequestPathInfo.class);
    when(request.getRequestPathInfo()).thenReturn(pathInfo);
    selectors = new String[] { "tidy" };
    when(pathInfo.getSelectors()).thenReturn(selectors);

    writer = mock(PrintWriter.class);
    when(response.getWriter()).thenReturn(writer);

    when(pathInfo.getResourcePath()).thenReturn(path);
  }

  @Test
  public void testDoGet() throws ServletException, IOException {
    when(resource.adaptTo(Content.class)).thenReturn(content);
    servlet.doGet(request, response);
    verify(response).setContentType("application/json");
    verify(writer, atLeastOnce()).write(anyString());
    verify(response).setStatus(HttpServletResponse.SC_OK);
  }

  @Test
  public void testNotFound() throws ServletException, IOException {
    when(resource.adaptTo(Content.class)).thenReturn(null);
    servlet.doGet(request, response);
    verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
  }

  @Test
  public void testDoGetTerse() throws ServletException, IOException {
    when(resource.adaptTo(Content.class)).thenReturn(content);
    when(verbose.getString()).thenReturn("false");
    servlet.doGet(request, response);
    verify(response).setContentType("application/json");
    verify(writer, atLeastOnce()).write(anyString());
    verify(response).setStatus(HttpServletResponse.SC_OK);
  }

  @Test
  public void testDoFalse() throws ServletException, IOException {
    when(resource.adaptTo(Content.class)).thenReturn(content);
    when(verbose.getString()).thenReturn("false");
    servlet.doGet(request, response);
    verify(response).setContentType("application/json");
    verify(writer, atLeastOnce()).write(anyString());
    verify(response).setStatus(HttpServletResponse.SC_OK);
  }


}
