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
package org.sakaiproject.nakamura.files.search;

import static org.sakaiproject.nakamura.files.search.ByTagSearchPropertyProvider.PROP_TYPE;

import static org.sakaiproject.nakamura.files.search.ByTagSearchPropertyProvider.PROP_RESOURCE_TYPE;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.files.search.ByTagSearchPropertyProvider.DEFAULT_FILTER_QUERY;

import com.google.common.collect.Maps;

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ByTagSearchPropertyProviderTest {
  private ByTagSearchPropertyProvider propProvider;

  @Mock
  SlingHttpServletRequest request;

  @Before
  public void setUp() {
    propProvider = new ByTagSearchPropertyProvider();
  }

  @Test
  public void testUnknownType() {
    when(request.getParameter("type")).thenReturn("random");

    Map<String, String> props = Maps.newHashMap();
    propProvider.loadUserProperties(request, props);
    assertTrue(props.containsKey(PROP_RESOURCE_TYPE));
    assertEquals(DEFAULT_FILTER_QUERY, props.get(PROP_RESOURCE_TYPE));
    assertEquals("", props.get(PROP_TYPE));
  }

  @Test
  public void testJustAuthorizables() {
    when(request.getParameter("type")).thenReturn("u").thenReturn("g")
        .thenReturn("u,g");

    Map<String, String> props = Maps.newHashMap();
    propProvider.loadUserProperties(request, props);
    assertTrue(props.containsKey(PROP_RESOURCE_TYPE));
    assertEquals("authorizable", props.get(PROP_RESOURCE_TYPE));
    assertEquals(" AND type:u", props.get(PROP_TYPE));

    props.clear();
    propProvider.loadUserProperties(request, props);
    assertTrue(props.containsKey(PROP_RESOURCE_TYPE));
    assertEquals("authorizable", props.get(PROP_RESOURCE_TYPE));
    assertEquals(" AND type:g", props.get(PROP_TYPE));

    props.clear();
    propProvider.loadUserProperties(request, props);
    assertTrue(props.containsKey(PROP_RESOURCE_TYPE));
    assertEquals("authorizable", props.get(PROP_RESOURCE_TYPE));
    assertEquals("", props.get(PROP_TYPE));
  }

  @Test
  public void testExpectedDefaultResponses() {
    when(request.getParameter("type")).thenReturn("u,g,c")
        .thenReturn("u,c").thenReturn("c,g");

    Map<String, String> props = Maps.newHashMap();
    propProvider.loadUserProperties(request, props);
    assertTrue(props.containsKey(PROP_RESOURCE_TYPE));
    assertEquals(DEFAULT_FILTER_QUERY, props.get(PROP_RESOURCE_TYPE));
    assertEquals("", props.get(PROP_TYPE));

    props.clear();
    propProvider.loadUserProperties(request, props);
    assertTrue(props.containsKey(PROP_RESOURCE_TYPE));
    assertEquals(DEFAULT_FILTER_QUERY, props.get(PROP_RESOURCE_TYPE));
    assertEquals(" AND type:u", props.get(PROP_TYPE));

    props.clear();
    propProvider.loadUserProperties(request, props);
    assertTrue(props.containsKey(PROP_RESOURCE_TYPE));
    assertEquals(DEFAULT_FILTER_QUERY, props.get(PROP_RESOURCE_TYPE));
    assertEquals(" AND type:g", props.get(PROP_TYPE));
  }

  @Test
  public void testContent() {
    when(request.getParameter("type")).thenReturn("c");

    Map<String, String> props = Maps.newHashMap();
    propProvider.loadUserProperties(request, props);
    assertTrue(props.containsKey(PROP_RESOURCE_TYPE));
    assertEquals("sakai/pooled-content", props.get(PROP_RESOURCE_TYPE));
    assertEquals("", props.get(PROP_TYPE));
  }

  @Test
  public void testCombinations() {
    when(request.getParameter("type")).thenReturn("u,random,c")
        .thenReturn("random,c").thenReturn("random,g");

    Map<String, String> props = Maps.newHashMap();
    propProvider.loadUserProperties(request, props);
    assertTrue(props.containsKey(PROP_RESOURCE_TYPE));
    assertEquals(DEFAULT_FILTER_QUERY, props.get(PROP_RESOURCE_TYPE));
    assertEquals(" AND type:u", props.get(PROP_TYPE));

    props.clear();
    propProvider.loadUserProperties(request, props);
    assertTrue(props.containsKey(PROP_RESOURCE_TYPE));
    assertEquals("sakai/pooled-content", props.get(PROP_RESOURCE_TYPE));
    assertEquals("", props.get(PROP_TYPE));

    props.clear();
    propProvider.loadUserProperties(request, props);
    assertTrue(props.containsKey(PROP_RESOURCE_TYPE));
    assertEquals("authorizable", props.get(PROP_RESOURCE_TYPE));
    assertEquals(" AND type:g", props.get(PROP_TYPE));
  }
}
