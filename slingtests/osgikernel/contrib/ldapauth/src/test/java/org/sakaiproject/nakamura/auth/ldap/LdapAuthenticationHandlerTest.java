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

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import org.apache.sling.engine.auth.AuthenticationInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.Principal;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RunWith(MockitoJUnitRunner.class)
public class LdapAuthenticationHandlerTest {

  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private HttpSession httpSession;

  @Mock
  private CallbackHandler callbackHandler;

  @Mock
  private Session jcrSession;

  @SuppressWarnings("unchecked")
  @Mock
  private Map options;

  private LdapAuthenticationHandler authHandler;

  @Before
  public void setup() {
    authHandler = new LdapAuthenticationHandler();
  }

  @Test
  public void canGetAuthenticationInfoFromASuitableRequest() {
    // given
    aRequestWithUsernameAndPasswordInIt();
    aRequestThatCanReturnASessionObject();

    // when
    AuthenticationInfo authInfo = authHandler.authenticate(request, response);

    // then
    assertEquals(LdapAuthenticationHandler.class.getName(), authInfo.getAuthType());
    verify(request).setAttribute(eq(LdapAuthenticationHandler.USER_AUTH), any());
  }

  @Test
  public void authenticationRequestDenied() throws Exception {
    assertFalse(authHandler.requestAuthentication(request, response));
  }

  @Test
  public void initDoesNothing() throws Exception {
    // when
    authHandler.doInit(callbackHandler, jcrSession, options);

    // then
    verifyZeroInteractions(callbackHandler, jcrSession, options);
  }
  
  @Test
  public void returnsPrincipalWithSameNameAsCredentials() throws Exception {
    // given
    String name = "joe";
    char[] password = {'f','o','o'};
    Credentials credentials = new SimpleCredentials(name, password);
    // when
    Principal principal = authHandler.getPrincipal(credentials);
    // then
    assertEquals(principal.getName(), name);
  }

  private void aRequestWithUsernameAndPasswordInIt() {
    when(request.getParameter(LdapAuthenticationHandler.PARAM_USERNAME)).thenReturn(
        "zach");
    when(request.getParameter(LdapAuthenticationHandler.PARAM_PASSWORD)).thenReturn(
        "secret");
  }

  private void aRequestThatCanReturnASessionObject() {
    when(request.getSession(anyBoolean())).thenReturn(httpSession);
    when(request.getSession()).thenReturn(httpSession);
  }

}
