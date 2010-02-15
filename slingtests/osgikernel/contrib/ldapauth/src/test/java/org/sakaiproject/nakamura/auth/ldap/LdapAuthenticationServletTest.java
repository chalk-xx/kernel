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
package org.sakaiproject.nakamura.auth.ldap;

import static org.mockito.Matchers.anyBoolean;

import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapAuthenticationServletTest {

  private LdapAuthenticationServlet servlet;

  private SlingHttpServletRequest request;

  @Mock
  private SlingHttpServletResponse response;

  @Mock
  private HttpSession httpSession;

  @Before
  public void init() {
    servlet = new LdapAuthenticationServlet();
    SlingHttpServletRequest baseRequest = new AttributeSettableMockSlingServletRequest(
        null, null, null, null, null);
    request = spy(baseRequest);
  }

  @Test
  public void ensureWeCheckForAuthentication() throws Exception {
    // when
    servlet.doPost(request, response);

    // then
    verify(request).getAttribute(LdapAuthenticationHandler.USER_AUTH);
  }

  @Test
  public void whenAuthIsPresentSetItOnTheSession() throws Exception {
    // given
    aRequestThatCanGiveBackASession();
    aRequestThatHasAnAuthObject();

    // when
    servlet.doPost(request, response);

    // then
    verify(httpSession).setAttribute(eq(LdapAuthenticationServlet.USER_CREDENTIALS),
        any());
  }

  private void aRequestThatHasAnAuthObject() {
    when(request.getParameter(LdapAuthenticationHandler.PARAM_USERNAME)).thenReturn(
        "zach");
    when(request.getParameter(LdapAuthenticationHandler.PARAM_PASSWORD)).thenReturn(
        "secret");
    new LdapAuthenticationHandler().authenticate(request, response);
  }

  private void aRequestThatCanGiveBackASession() {
    when(request.getSession(anyBoolean())).thenReturn(httpSession);
    when(request.getSession()).thenReturn(httpSession);
  }

  private class AttributeSettableMockSlingServletRequest extends
      MockSlingHttpServletRequest {
    private Map<String, Object> attributes = new HashMap<String, Object>();

    public AttributeSettableMockSlingServletRequest(String resourcePath,
        String selectors, String extension, String suffix, String queryString) {
      super(resourcePath, selectors, extension, suffix, queryString);
    }

    @Override
    public void setAttribute(String name, Object obj) {
      attributes.put(name, obj);
    }

    @Override
    public Object getAttribute(String name) {
      return attributes.get(name);
    }

  }

}
