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
package org.sakaiproject.nakamura.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class WidgetizeServletTest {

  private WidgetizeServlet servlet;
  private String path;
  private StringWriter stringWriter;
  private PrintWriter printWriter;

  @Mock
  private ResourceResolver resolver;
  @Mock
  private SlingHttpServletResponse response;
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private CacheManagerService cacheManagerService;

  @Before
  public void setUp() throws IOException {
    // Initialize servlet
    servlet = new WidgetizeServlet();
    Map<String, String[]> properties = new HashMap<String, String[]>();
    properties.put(WidgetizeServlet.BATCH_IGNORE_NAMES, new String[] { "bundles" });
    properties.put(WidgetizeServlet.BATCH_VALID_MIMETYPES, new String[] { "text/plain",
        "text/css", "text/html", "application/json", "application/xml" });

    servlet.activate(properties);

    // Init mocks
    MockitoAnnotations.initMocks(this);

    servlet.cacheManagerService = cacheManagerService;

    when(request.getResourceResolver()).thenReturn(resolver);

    // For test cases we will always assume that a locale of nl_NL is request.
    RequestParameter localeParam = mock(RequestParameter.class);
    when(localeParam.getString()).thenReturn("nl_NL");
    when(request.getRequestParameter("locale")).thenReturn(localeParam);

    // Mock the response print writer.
    stringWriter = new StringWriter();
    printWriter = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(printWriter);
  }

  private void setupGoodWidget() {
    // Mock the resource
    path = "/widgets/twitter";
    File file = new File(getClass().getResource(path).getPath());
    Resource resource = mockWidgetFile(path, file);

    when(request.getResource()).thenReturn(resource);
  }

  private void setupBadWidget() {
    // Mock the resource
    path = "/widgets/badwidget";
    File file = new File(getClass().getResource(path).getPath());
    Resource resource = mockWidgetFile(path, file);

    when(request.getResource()).thenReturn(resource);
  }

  /**
   * 
   */
  private Resource mockWidgetFile(String path, File file) {
    // Get the inputstream for this file (null if directory.)
    InputStream in = getStream(file);

    // Mock the resource
    Resource resource = mock(Resource.class);
    when(resource.adaptTo(InputStream.class)).thenReturn(in);
    when(resource.adaptTo(File.class)).thenReturn(file);
    when(resource.getResourceResolver()).thenReturn(resolver);
    when(resource.getPath()).thenReturn(path);

    // Add the resource to the resource resolver.
    when(resolver.getResource(path)).thenReturn(resource);

    // Mock all the children
    List<Resource> resources = mockFileChildren(path, file);
    when(resolver.listChildren(resource)).thenReturn(resources.iterator());
    return resource;
  }

  /**
   * @param file
   */
  private List<Resource> mockFileChildren(String path, File file) {
    List<Resource> resources = new ArrayList<Resource>();
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      for (File child : children) {
        Resource resource = mockWidgetFile(path + "/" + child.getName(), child);
        resources.add(resource);
      }
    }
    return resources;
  }

  /**
   * @param file
   * @return
   */
  protected InputStream getStream(File file) {
    if (!file.isDirectory() && file.canRead()) {
      try {
        return new FileInputStream(file);
      } catch (IOException ioe) {
        // Swallow it
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGoodWidgetUncached() throws Exception {
    // Always return null for cached content.
    Cache<Object> cache = mock(Cache.class);
    when(
        cacheManagerService
            .getCache(Mockito.anyString(), Mockito.eq(CacheScope.INSTANCE))).thenReturn(
        cache);

    setupGoodWidget();

    servlet.doGet(request, response);

    printWriter.flush();
    JSONObject json = new JSONObject(stringWriter.toString());

    // Assert the responses
    verify(response).setCharacterEncoding("UTF-8");
    verify(response).setContentType("application/json");

    // Check if the correct languages get loaded.
    assertNotNull(json.get("bundles"));
    assertNotNull(json.getJSONObject("bundles").get("default"));
    assertNotNull(json.getJSONObject("bundles").get("nl_NL"));
    String def = json.getJSONObject("bundles").getJSONObject("default").getString(
        "YOUR_STATUS_HAS_BEEN_SUCCESSFULLY_UPDATED");
    assertEquals("Your status has been succesfully updated", def);
    String dutch = json.getJSONObject("bundles").getJSONObject("nl_NL").getString(
        "YOUR_STATUS_HAS_BEEN_SUCCESSFULLY_UPDATED");
    assertEquals("Uw status is geupdated", dutch);

    // Check if the all the files get loaded.
    assertNotNull(json.getJSONObject("css").getJSONObject("twitter.css"));
    assertNotNull(json.getJSONObject("javascript").getJSONObject("twitter.js"));
    assertNotNull(json.getJSONObject("twitter.html"));

    // Make sure that the content is there.
    assertEquals(String.class, json.getJSONObject("css").getJSONObject("twitter.css")
        .get("content").getClass());
    assertEquals(String.class, json.getJSONObject("javascript").getJSONObject(
        "twitter.js").get("content").getClass());
    assertEquals(String.class, json.getJSONObject("twitter.html").get("content")
        .getClass());

    // Make sure that images don't get loaded.
    assertEquals(false, json.getJSONObject("images").getJSONObject("twitter.png").get(
        "content"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBadWidget() throws Exception {
    // Always return null for cached content.
    Cache<Object> cache = mock(Cache.class);
    when(
        cacheManagerService
            .getCache(Mockito.anyString(), Mockito.eq(CacheScope.INSTANCE))).thenReturn(
        cache);

    setupBadWidget();

    servlet.doGet(request, response);

    verify(response).sendError(HttpServletResponse.SC_FORBIDDEN,
        "The current resource is not a widget.");

  }

}
