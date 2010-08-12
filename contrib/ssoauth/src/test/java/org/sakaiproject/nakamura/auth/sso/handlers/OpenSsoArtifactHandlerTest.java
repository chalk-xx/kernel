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
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;

import junit.framework.Assert;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.auth.sso.ArtifactHandler;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 *
 */
@RunWith(value = MockitoJUnitRunner.class)
public class OpenSsoArtifactHandlerTest {
  OpenSsoArtifactHandler handler;

  @Mock
  HttpServletRequest request;

  static final String ARTIFACT = "some-great-token-id";
  static final String SERVICE_URL = "http://localhost:8080/system/sling/sso/login%3Fsakaiauth:login%3D2%26resource%3D/dev/my_sakai.html";

  HashMap<String, String> props = new HashMap<String, String>();
  LocalTestServer server;

  @Before
  public void setUp() {
    handler = new OpenSsoArtifactHandler();
    handler.activate(props);
  }

  @After
  public void tearDown() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void getLoginUrl() throws Exception {
    String url = handler.getLoginUrl(SERVICE_URL, request);
    new URL(url);
    assertTrue(url.startsWith(OpenSsoArtifactHandler.DEFAULT_LOGIN_URL));

    String otherServer = "http://someotherserver/sso/UI/Login";
    props.put(ArtifactHandler.LOGIN_URL, otherServer);
    handler.modified(props);

    url = handler.getLoginUrl(SERVICE_URL, request);
    new URL(url);
    assertTrue(url.startsWith(otherServer));
  }

  @Test
  public void getValidateUrl() throws Exception {
    String url = handler.getValidateUrl(ARTIFACT, "service", request);
    new URL(url);
    assertTrue(url.startsWith(OpenSsoArtifactHandler.DEFAULT_SERVER_URL));

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
    assertTrue(url.startsWith(OpenSsoArtifactHandler.DEFAULT_LOGOUT_URL));

    String otherServer = "http://someotherserver/sso/UI/Logout";
    props.put(ArtifactHandler.LOGOUT_URL, otherServer);
    handler.modified(props);

    url = handler.getLogoutUrl(request);
    new URL(url);
    assertTrue(url.startsWith(otherServer));
  }

  @Test
  public void extractArtifactWithoutArtifact() throws Exception {
    String artifact = handler.extractArtifact(request);
    assertNull(artifact);
  }

  @Test
  public void extractArtifact() throws Exception {
    Cookie[] cookies = new Cookie[2];
    cookies[0] = new Cookie("some-other-cookie", "nothing-great");
    cookies[1] = new Cookie(handler.getArtifactName(), ARTIFACT);
    when(request.getCookies()).thenReturn(cookies);
    String artifact = handler.extractArtifact(request);
    assertNotNull(artifact);
    Assert.assertEquals("some-great-token-id", artifact);
  }

  @Test
  public void extractCredentialsNegativeResponse() throws Exception {
    String credentials = handler.extractCredentials(ARTIFACT, "boolean=false\n", request);
    assertNull(credentials);
  }

  @Test
  public void extractCredentialsAttrsServerUnreachable() throws Exception {
    String credentials = handler.extractCredentials(ARTIFACT,
        OpenSsoArtifactHandler.DEFAULT_SUCCESSFUL_BODY, request);
    assertNull(credentials);
  }

  @Test
  public void extractCredentialsAttrsServerErrors() throws Exception {
    setupLocalServer();

//    int start = OpenSsoArtifactHandler.ATTRS_URL_TMPL.indexOf('/');
//    int end = OpenSsoArtifactHandler.ATTRS_URL_TMPL.indexOf('?');
//    String url = OpenSsoArtifactHandler.ATTRS_URL_TMPL.substring(start, end);

    server.register("*", new HttpRequestHandler() {

      public void handle(HttpRequest request, HttpResponse response, HttpContext context)
          throws HttpException, IOException {
        response.setStatusCode(404);
      }
    });

    String credentials = handler.extractCredentials(ARTIFACT,
        OpenSsoArtifactHandler.DEFAULT_SUCCESSFUL_BODY, request);
    assertNull(credentials);
  }

  @Test
  public void extractCredentialsAttrsServerBadResponse() throws Exception {
    setupLocalServer();

    server.register("*", new HttpRequestHandler() {

      public void handle(HttpRequest request, HttpResponse response, HttpContext context)
          throws HttpException, IOException {
        response.setStatusCode(200);
        String output = "Random unexpected line of text\n";
        response.setEntity(new StringEntity(output));
      }
    });

    String credentials = handler.extractCredentials(ARTIFACT,
        OpenSsoArtifactHandler.DEFAULT_SUCCESSFUL_BODY, request);
    assertNull(credentials);
  }

  @Test
  public void extractCredentials() throws Exception {
    setupLocalServer();

    server.register("*", new HttpRequestHandler() {

      public void handle(HttpRequest request, HttpResponse response, HttpContext context)
          throws HttpException, IOException {
        response.setStatusCode(200);
        String output = "Random Opening Line That Shouldn't Matter\n"
            + OpenSsoArtifactHandler.USRDTLS_ATTR_NAME_STUB
            + OpenSsoArtifactHandler.DEFAULT_ATTRIBUTE_NAME + "\n"
            + OpenSsoArtifactHandler.USRDTLS_ATTR_VAL_STUB + "someUserId";
        response.setEntity(new StringEntity(output));
      }
    });

    String credentials = handler.extractCredentials(ARTIFACT,
        OpenSsoArtifactHandler.DEFAULT_SUCCESSFUL_BODY, request);
    assertEquals("someUserId", credentials);
  }

  private void setupLocalServer() throws Exception {
    // test for an open port; check the "dynamic, private or ephemeral ports"
    server = new LocalTestServer(null, null);
    server.start();

    String serverUrl = "http://" + server.getServiceHostName() + ":" + server.getServicePort();
    props.put(ArtifactHandler.SERVER_URL, serverUrl);
    handler.modified(props);
  }
}
