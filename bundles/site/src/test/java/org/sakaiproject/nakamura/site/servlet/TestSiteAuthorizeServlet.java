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
package org.sakaiproject.nakamura.site.servlet;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.site.SiteService;

import javax.jcr.Node;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@RunWith(value = MockitoJUnitRunner.class)
public class TestSiteAuthorizeServlet {
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private SlingHttpServletRequest request;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private SlingHttpServletResponse response;

  @Mock
  private Resource resource;

  @Mock
  private Node node;

  @Mock
  private JackrabbitSession jackRabbitSession;

  @Mock
  private SiteService siteService;

  private SiteAuthorizeServlet servlet;

  @Before
  public void setUp() throws Exception {
    servlet = new SiteAuthorizeServlet();
    servlet.bindSiteService(siteService);

    when(request.getResource()).thenReturn(resource);
    when(resource.adaptTo(Node.class)).thenReturn(node);
    when(node.getSession()).thenReturn(jackRabbitSession);
  }

  @After
  public void teardown() {
    servlet.unbindSiteService(siteService);
  }

  @Test
  public void nodeIsNotASite() throws Exception {
    servlet.doPost(request, response);

    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
        "Location does not represent site ");
  }

  @Test
  public void nodeIsASite() throws Exception {
    when(siteService.isSite(node)).thenReturn(true);

    servlet.doPost(request, response);
  }
}
