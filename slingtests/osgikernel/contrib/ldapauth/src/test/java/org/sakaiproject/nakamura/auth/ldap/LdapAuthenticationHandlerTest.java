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

import org.apache.sling.commons.auth.spi.AuthenticationInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LdapAuthenticationHandlerTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private ComponentContext context;

  @Mock
  private BundleContext bundleContext;

  private LdapAuthenticationHandler authHandler;

  @Before
  public void setup() throws Exception {
    when(context.getProperties()).thenReturn(new Properties());
    when(context.getBundleContext()).thenReturn(bundleContext);
    when(bundleContext.getDataFile(isA(String.class))).thenReturn(
        File.createTempFile("test", "tmp"));
    
    authHandler = new LdapAuthenticationHandler();
    authHandler.activate(context);
  }

  @Test
  public void authenticationRequestDenied() throws Exception {
    assertTrue(authHandler.requestCredentials(request, response));
  }

  @Test
  public void canGetAuthenticationInfoFromPostFields() {
    // given
    when(request.getParameter(LdapAuthenticationHandler.PAR_USERNAME)).thenReturn(
        "zach");
    when(request.getParameter(LdapAuthenticationHandler.PAR_PASSWORD)).thenReturn(
        "secret");
    when(request.getMethod()).thenReturn("POST");
    when(request.getParameter(LdapAuthenticationHandler.REQUEST_LOGIN_PARAMETER)).thenReturn("1");

    // when
    AuthenticationInfo authInfo = authHandler.extractCredentials(request, response);

    // then
    assertEquals(LdapAuthenticationHandler.LDAP_AUTH, authInfo.getAuthType());
    assertEquals("zach", authInfo.getUser());
    assertEquals("secret", new String(authInfo.getPassword()));
  }
}
