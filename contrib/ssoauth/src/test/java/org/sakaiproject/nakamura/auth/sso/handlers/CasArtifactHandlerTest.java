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
package org.sakaiproject.nakamura.auth.sso.handlers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler;

import java.net.URL;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
@RunWith(value = MockitoJUnitRunner.class)
public class CasArtifactHandlerTest {
  CasArtifactHandler handler;

  HashMap<String, Object> props = new HashMap<String, Object>();
  String ARTIFACT = "some-great-token-id";
  String SERVICE_URL = "http://localhost:8080/system/sling/sso/login%3Fsakaiauth:login%3D2%26resource%3D/dev/my_sakai.html";

  @Mock
  HttpServletRequest request;

  @Before
  public void setUp() {
    handler = new CasArtifactHandler();
    handler.activate(props);
  }

  @Test
  public void extractArtifactWithoutArtifact() throws Exception {
    assertNull(handler.extractArtifact(request));
  }

  @Test
  public void extractArtifact() throws Exception {
    when(request.getParameter(handler.getArtifactName())).thenReturn(ARTIFACT);

    assertEquals(ARTIFACT, handler.extractArtifact(request));
  }

  @Test
  public void getLoginUrlDefaultParams() throws Exception {
    String url = handler.getLoginUrl(SERVICE_URL, request);
    new URL(url);
    assertTrue(url.startsWith(CasArtifactHandler.DEFAULT_LOGIN_URL));
    assertTrue(url.endsWith(SERVICE_URL));
    assertFalse(url.contains("gateway=" + CasArtifactHandler.DEFAULT_GATEWAY));
    assertFalse(url.contains("renew=" + CasArtifactHandler.DEFAULT_RENEW));

    String otherServer = "http://someotherserver/sso/UI/Login";
    props.put(ArtifactHandler.LOGIN_URL, otherServer);
    handler.modified(props);

    url = handler.getLoginUrl(SERVICE_URL, request);
    new URL(url);
    assertTrue(url.startsWith(otherServer));
    assertFalse(url.contains("gateway=" + CasArtifactHandler.DEFAULT_GATEWAY));
    assertFalse(url.contains("renew=" + CasArtifactHandler.DEFAULT_RENEW));
  }

  @Test
  public void getLoginUrlWithRenew() throws Exception {
    when(request.getParameter("renew")).thenReturn("true").thenReturn(
        "false");
    String url = handler.getLoginUrl(SERVICE_URL, request);
    assertTrue(url.startsWith(CasArtifactHandler.DEFAULT_LOGIN_URL));
    assertTrue(url.contains("renew=true"));

    url = handler.getLoginUrl(SERVICE_URL, request);
    assertTrue(url.startsWith(CasArtifactHandler.DEFAULT_LOGIN_URL));
    assertFalse(url.contains("renew=false"));

    props.put(CasArtifactHandler.RENEW, false);
    url = handler.getLoginUrl(SERVICE_URL, request);
    assertTrue(url.startsWith(CasArtifactHandler.DEFAULT_LOGIN_URL));
    assertFalse(url.contains("renew=false"));
  }

  @Test
  public void getLoginUrlWithGateway() throws Exception {
    when(request.getParameter("gateway")).thenReturn("true").thenReturn(
        "false");
    String url = handler.getLoginUrl(SERVICE_URL, request);
    assertTrue(url.startsWith(CasArtifactHandler.DEFAULT_LOGIN_URL));
    assertTrue(url.contains("gateway=true"));

    url = handler.getLoginUrl(SERVICE_URL, request);
    assertTrue(url.startsWith(CasArtifactHandler.DEFAULT_LOGIN_URL));
    assertFalse(url.contains("gateway=false"));

    props.put(CasArtifactHandler.GATEWAY, false);
    url = handler.getLoginUrl(SERVICE_URL, request);
    assertTrue(url.startsWith(CasArtifactHandler.DEFAULT_LOGIN_URL));
    assertFalse(url.contains("renew=false"));
  }

  @Test
  public void getValidateUrl() throws Exception {
    String url = handler.getValidateUrl(ARTIFACT, "service", request);
    new URL(url);
    assertTrue(url.startsWith(CasArtifactHandler.DEFAULT_SERVER_URL));

    String otherServer = "http://someotherserver";
    props.put(ArtifactHandler.SERVER_URL, otherServer);
    handler.modified(props);

    url = handler.getValidateUrl(ARTIFACT, "service", request);
    new URL(url);
    assertTrue(url.startsWith(otherServer));
  }

  @Test
  public void getLogoutUrl() throws Exception {
    String url = handler.getLogoutUrl(request);
    new URL(url);
    assertTrue(url.startsWith(CasArtifactHandler.DEFAULT_LOGOUT_URL));

    String otherServer = "http://someotherserver/sso/UI/Logout";
    props.put(ArtifactHandler.LOGOUT_URL, otherServer);
    handler.modified(props);

    url = handler.getLogoutUrl(request);
    new URL(url);
    assertTrue(url.startsWith(otherServer));
  }

  @Test
  public void extractCredentialsInvalidResponse() throws Exception {
    String credentials = handler.extractCredentials(ARTIFACT, "", request);
    assertNull(credentials);
  }

  @Test
  public void extractCredentialsFailureRespnose() throws Exception {
    String response = " <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>\n"
        + "  <cas:authenticationFailure code='INVALID_REQUEST'>\n"
        + "    &#039;service&#039; and &#039;ticket&#039; parameters are both required\n"
        + "  </cas:authenticationFailure>\n"
        + "</cas:serviceResponse>\n";
    String credentials = handler.extractCredentials(ARTIFACT, response, request);
    assertNull(credentials);
  }

  @Test
  public void extractCredentialsSuccessfulResponse() {
    String response = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>\n"
        + "  <cas:authenticationSuccess>\n"
        + "    <cas:user>NetID</cas:user>\n"
        + "  </cas:authenticationSuccess>\n"
        + "</cas:serviceResponse>\n";
    String credentials = handler.extractCredentials(ARTIFACT, response, request);
    assertEquals("NetID", credentials);
  }
}
