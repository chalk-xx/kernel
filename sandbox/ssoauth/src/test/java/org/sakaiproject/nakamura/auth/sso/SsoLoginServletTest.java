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
package org.sakaiproject.nakamura.auth.sso;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.auth.sso.SsoAuthConstants;

/**
 *
 */
@RunWith(value = MockitoJUnitRunner.class)
public class SsoLoginServletTest {
  SsoLoginServlet servlet;

  @Mock
  SsoAuthenticationHandler handler;

  @Mock
  SlingHttpServletRequest request;

  @Mock
  SlingHttpServletResponse response;

  @Before
  public void setUp() {
    // here to make coverage 100%
    new SsoLoginServlet();

    servlet = new SsoLoginServlet(handler);
  }

  @Test
  public void requestCredentials() throws Exception {
    servlet.service(request, response);
    verify(handler).requestCredentials(request, response);
  }

  @Test
  public void redirectWithoutTarget() throws Exception {
    when(request.getAuthType()).thenReturn(SsoAuthConstants.SSO_AUTH_TYPE);

    ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);

    when(handler.getReturnPath(request)).thenReturn(null);
    servlet.service(request, response);

    verify(response).sendRedirect(redirectCaptor.capture());
    verify(handler, Mockito.never()).requestCredentials(request, response);

    assertEquals("null/", redirectCaptor.getValue());
  }

  @Test
  public void redirectWithTarget() throws Exception {
    when(request.getAuthType()).thenReturn(SsoAuthConstants.SSO_AUTH_TYPE);

    ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);

    when(request.getRequestURI()).thenReturn("someURI");
    when(handler.getReturnPath(request)).thenReturn("greatplace");
    servlet.service(request, response);

    verify(response).sendRedirect(redirectCaptor.capture());
    verify(handler, Mockito.never()).requestCredentials(request, response);

    assertEquals("greatplace", redirectCaptor.getValue());
  }

  @Test
  public void redirectWithTargetEqualsRequestURI() throws Exception {
    when(request.getAuthType()).thenReturn(SsoAuthConstants.SSO_AUTH_TYPE);

    ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);

    when(request.getContextPath()).thenReturn("someContextPath");
    when(request.getRequestURI()).thenReturn("someURI");
    when(handler.getReturnPath(request)).thenReturn("someURI");
    servlet.service(request, response);

    verify(response).sendRedirect(redirectCaptor.capture());
    verify(handler, Mockito.never()).requestCredentials(request, response);

    assertEquals("someContextPath/", redirectCaptor.getValue());
  }
}
