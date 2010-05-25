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
package org.sakaiproject.nakamura.authz.servlets;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.authz.servlets.CreateRuleAclServlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.ServletException;

/**
 *
 */
public class CreateRuleAclServletTest {

  
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private SlingHttpServletResponse httpResponse;
  @Mock
  private Resource resource;
  @Mock
  private ResourceResolver resourceResolver;
  @Mock
  private JackrabbitSession session;
  @Mock
  private PrincipalManager principalManager;
  @Mock
  private Principal principal;
  @Mock
  private Item node;
  
  
  private CreateRuleAclServlet servlet;
  private StringWriter stringWriter;

  public CreateRuleAclServletTest() {
    MockitoAnnotations.initMocks(this);
  }
 
  @Before
  public void setup() throws IOException, RepositoryException {
    // basic setup
    servlet = new CreateRuleAclServlet();
    Mockito.when(request.getResource()).thenReturn(resource);
    Mockito.when(resource.getPath()).thenReturn("/mytestace-path");
    Mockito.when(request.getResourceResolver()).thenReturn(resourceResolver);
    Mockito.when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    Mockito.when(session.getPrincipalManager()).thenReturn(principalManager);
    stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    Mockito.when(httpResponse.getWriter()).thenReturn(printWriter);
    
  }
 
  @Test
  public void testCreateACE() throws ServletException, IOException {
    
    Mockito.when(request.getParameter("principalId")).thenReturn("ieb");
    Mockito.when(principalManager.getPrincipal("ieb")).thenReturn(principal);
    Mockito.when(principal.getName()).thenReturn("ieb");
    Mockito.when(resource.adaptTo(Item.class)).thenReturn(node);
    
    servlet.doPost(request, httpResponse);
    
    System.err.println(stringWriter.toString());
  }
}
