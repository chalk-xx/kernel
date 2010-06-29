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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheScope;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

/**
 *
 */
public class WidgetsServletTest extends AbstractWidgetServletTest {

  private WidgetsServlet servlet;

  @Before
  public void setUp() throws IOException {
    super.setUp();

    servlet = new WidgetsServlet();
    servlet.widgetService = widgetService;

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testListUncached() throws ServletException, IOException, JSONException {
    Cache<Object> cache = mock(Cache.class);

    when(
        cacheManagerService
            .getCache(Mockito.anyString(), Mockito.eq(CacheScope.INSTANCE))).thenReturn(
        cache);

    when(request.getRequestPathInfo()).thenReturn(getRequestPathInfo("json"));
    servlet.doGet(request, response);
    printWriter.flush();
    JSONObject json = new JSONObject(stringWriter.toString());
    assertNotNull(json.get("twitter"));
    assertNull(json.opt("badwidget"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testListCached() throws ServletException, IOException, JSONException {
    Cache<Object> cache = mock(Cache.class);

    when(
        cacheManagerService
            .getCache(Mockito.anyString(), Mockito.eq(CacheScope.INSTANCE))).thenReturn(
        cache);

    Map<String, ValueMap> map = new HashMap<String, ValueMap>();
    JsonValueMap jsonMap = new JsonValueMap("{'bar' : true}");
    map.put("foo", jsonMap);
    when(cache.get("configs")).thenReturn(map);

    when(request.getRequestPathInfo()).thenReturn(getRequestPathInfo("json"));

    servlet.doGet(request, response);
    printWriter.flush();
    JSONObject json = new JSONObject(stringWriter.toString());
    assertNotNull(json.get("foo"));
    assertTrue(json.getJSONObject("foo").getBoolean("bar"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testJSONP() throws ServletException, IOException {
    Cache<Object> cache = mock(Cache.class);

    when(
        cacheManagerService
            .getCache(Mockito.anyString(), Mockito.eq(CacheScope.INSTANCE))).thenReturn(
        cache);

    when(request.getRequestPathInfo()).thenReturn(getRequestPathInfo("jsonp"));
    servlet.doGet(request, response);
    printWriter.flush();
    String content = stringWriter.toString();
    assertTrue(content.startsWith("var Widgets={"));
    assertTrue(content.endsWith("};"));
  }

  public RequestPathInfo getRequestPathInfo(final String extension) {
    return new RequestPathInfo() {

      public String getSuffix() {
        return null;
      }

      public String[] getSelectors() {
        return null;
      }

      public String getSelectorString() {
        return null;
      }

      public String getResourcePath() {
        return null;
      }

      public String getExtension() {
        return extension;
      }
    };
  }
}
