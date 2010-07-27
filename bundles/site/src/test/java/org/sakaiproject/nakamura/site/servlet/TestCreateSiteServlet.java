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

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.site.SiteService;

import javax.jcr.Session;

/**
 *
 */
@RunWith(value = MockitoJUnitRunner.class)
public class TestCreateSiteServlet {

  private CreateSiteServlet servlet;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SlingHttpServletRequest request;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SlingHttpServletResponse response;

  @Mock
  private SiteService siteService;

  @Mock
  private SlingRepository slingRepository;

  @Mock
  private ResourceResolver resourceResolver;

  @Mock
  private JackrabbitSession session;

  @Mock
  private UserManager userManager;

  @Mock
  private Authorizable authorizable;

  @Before
  public void setUp() throws Exception {
    servlet = new CreateSiteServlet();
    servlet.bindSiteService(siteService);
    servlet.bindSlingRepository(slingRepository);

    when(request.getResourceResolver()).thenReturn(resourceResolver);
    when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    when(session.getUserManager()).thenReturn(userManager);
  }

  @After
  public void tearDown() throws Exception {
    servlet.unbindSiteService(siteService);
    servlet.unbindSlingRepository(slingRepository);
  }

  @Test
  public void test() throws Exception {
    when(userManager.getAuthorizable(anyString())).thenReturn(authorizable);
    servlet.doPost(request, response);
  }
}
