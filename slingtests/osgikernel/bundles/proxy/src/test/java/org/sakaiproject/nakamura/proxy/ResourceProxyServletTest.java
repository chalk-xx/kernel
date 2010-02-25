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
package org.sakaiproject.nakamura.proxy;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.proxy.ProxyClientService;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceProxyServletTest {

  private ResourceProxyServlet servlet;

  @Mock
  private SlingHttpServletRequest request;

  @Mock
  private SlingHttpServletResponse response;

  @Mock
  private Resource resource;

  @Mock
  private Node node;

  private ConcurrentHashMap<String, String> headerNames;

  private ConcurrentHashMap<String, String> parameterNames;

  @Mock
  private ProxyClientService proxyClientService;

  @Mock
  private ProxyResponse proxyResponse;

  @Mock
  private ServletOutputStream responseOutputStream;

  @Before
  public void setup() {
    servlet = new ResourceProxyServlet();
    headerNames = new ConcurrentHashMap<String, String>();
    parameterNames = new ConcurrentHashMap<String, String>();
  }

  @Test
  public void rejectsRequestsNotOnTheRightPath() throws Exception {
    // given
    requestReturnsAResource();
    resourceWithFooPath();

    // when
    servlet.doGet(request, response);

    // then
    verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
  }

  @Test
  public void returnsAProxiedGet() throws Exception {
    // given
    requestReturnsAResource();
    resourceWithTwitterPath();
    resourceReturnsANode();
    requestReturnsHeaderNames();
    requestReturnsParameterNames();
    proxyClientServiceReturnsAProxyResponse();
    proxyResponseHasHelloWorldInputStream();
    slingResponseHasOutputStream();
    servlet.proxyClientService = proxyClientService;

    // when
    servlet.doGet(request, response);
  }

  @Test
  public void canDoBasicAuth() throws Exception {
    // given
    requestReturnsAResource();
    resourceWithTwitterPath();
    resourceReturnsANode();
    requestReturnsHeaderNames();
    requestReturnsParameterNames();
    requestHasBasicAuthHeaders();
    proxyClientServiceReturnsAProxyResponse();
    proxyResponseHasHelloWorldInputStream();
    slingResponseHasOutputStream();
    servlet.proxyClientService = proxyClientService;

    // when
    servlet.doGet(request, response);
  }

  private void requestHasBasicAuthHeaders() {
    headerNames.put(":basic-user", "");
    headerNames.put(":basic-password", "");
    when(request.getHeader(":basic-user")).thenReturn("zach");
    when(request.getHeader(":basic-password")).thenReturn("secret");
  }

  private void slingResponseHasOutputStream() throws Exception {
    when(response.getOutputStream()).thenReturn(responseOutputStream);
  }

  private void proxyResponseHasHelloWorldInputStream() throws Exception {
    when(proxyResponse.getResponseBodyAsInputStream()).thenReturn(
        new ByteArrayInputStream("Hello, world.".getBytes("UTF-8")));
  }

  @SuppressWarnings("unchecked")
  private void proxyClientServiceReturnsAProxyResponse() throws Exception {
    when(
        proxyClientService.executeCall((Node) any(), (Map<String, String>) any(),
            (Map<String, Object>) any(), (InputStream) any(), anyLong(), anyString()))
        .thenReturn(proxyResponse);
  }

  private void requestReturnsParameterNames() {
    when(request.getParameterNames()).thenReturn(parameterNames.keys());
  }

  private void requestReturnsHeaderNames() {
    when(request.getHeaderNames()).thenReturn(headerNames.keys());
  }

  private void resourceReturnsANode() {
    when(resource.adaptTo(Node.class)).thenReturn(node);
  }

  private void requestReturnsAResource() {
    when(request.getResource()).thenReturn(resource);
  }

  private void resourceWithFooPath() {
    when(resource.getPath()).thenReturn("/foo/bar/_dostuff");
  }

  private void resourceWithTwitterPath() {
    when(resource.getPath()).thenReturn(
        ResourceProxyServlet.PROXY_PATH_PREFIX + "twitter");
  }

}
