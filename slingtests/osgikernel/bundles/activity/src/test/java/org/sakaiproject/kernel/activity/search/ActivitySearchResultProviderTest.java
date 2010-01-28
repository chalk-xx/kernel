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
package org.sakaiproject.kernel.activity.search;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Test;
import org.sakaiproject.kernel.api.activity.ActivityConstants;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ActivitySearchResultProviderTest extends AbstractEasyMockTest {

  @Test
  public void testLoadProperties() {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getRemoteUser()).andReturn("admin");
    addStringRequestParameter(request, "site", "/sites/mysite");

    replay();
    ActivitySearchPropertyProvider provider = new ActivitySearchPropertyProvider();
    Map<String, String> propertiesMap = new HashMap<String, String>();
    provider.loadUserProperties(request, propertiesMap);
    String actual = propertiesMap.get("_myFeed");
    String expected = ISO9075.encodePath("/_user/private/d0/33/e2/2a/admin/"
        + ActivityConstants.ACTIVITY_FEED_NAME);
    assertEquals(expected, actual);
    String siteFeed = propertiesMap.get("_siteFeed");
    assertEquals("/sites/mysite/" + ActivityConstants.ACTIVITY_FEED_NAME,
        siteFeed);
  }

  @Test
  public void testAnonLoadPRoperties() {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getRemoteUser()).andReturn("anon");

    replay();
    ActivitySearchPropertyProvider provider = new ActivitySearchPropertyProvider();
    Map<String, String> propertiesMap = new HashMap<String, String>();
    try {
      provider.loadUserProperties(request, propertiesMap);
      fail("Anonymous users can't request an activity feed.");
    } catch (IllegalStateException e) {

    }
  }

}
