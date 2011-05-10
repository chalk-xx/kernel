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
package org.sakaiproject.nakamura.discussion;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.discussion.searchresults.DiscussionInitialPostPropertyProvider;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class InitialPostPropertyProviderTest extends AbstractEasyMockTest {

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest#setUp()
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testPath() {
    DiscussionInitialPostPropertyProvider provider = new DiscussionInitialPostPropertyProvider();

    Map<String, String> propertiesMap = new HashMap<String, String>();

    Resource resource = mock(Resource.class);
    ResourceResolver resolver = mock(ResourceResolver.class);
    Content messageContent = new Content("a:userIdHere", null);
    RequestParameter pathParam = mock(RequestParameter.class);
    when(pathParam.getString()).thenReturn("a:userIdHere");

    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(resolver.getResource(anyString())).thenReturn(resource);
    when(resource.adaptTo(Content.class)).thenReturn(messageContent);
    when(request.getRequestParameter("path")).thenReturn(pathParam);

    replay();
    provider.loadUserProperties(request, propertiesMap);

    // Proper escaping.
    assertEquals("a\\:userIdHere", propertiesMap.get("path"));
  }

}
