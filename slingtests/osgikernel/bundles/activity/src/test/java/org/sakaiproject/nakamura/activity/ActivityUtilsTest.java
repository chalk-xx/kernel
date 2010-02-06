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
package org.sakaiproject.nakamura.activity;

import org.junit.Assert;
import org.junit.Test;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.activity.ActivityUtils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ActivityUtilsTest {

  @Test
  public void testUserFeed() {
    String user = "admin";
    String expected = "/_user/private/d0/33/e2/2a/admin/"
        + ActivityConstants.ACTIVITY_FEED_NAME;
    String result = ActivityUtils.getUserFeed(user);
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testCreateID() throws UnsupportedEncodingException,
      NoSuchAlgorithmException {
    List<String> ids = new ArrayList<String>();
    for (int i = 0; i < 1000; i++) {
      String s = ActivityUtils.createId();
      if (ids.contains(s)) {
        Assert.fail("This id is already in the list.");
      }
      ids.add(s);
    }
  }

  @Test
  public void testGetPath() {
    String id = "2010-01-22-09-ef12a1e112d21f31431b3c4535d1d3a13";
    String startPath = "/_user/private/activityFeed";
    String result = ActivityUtils.getPathFromId(id, startPath);
    Assert
        .assertEquals(
            "/_user/private/activityFeed/2010/01/22/09/2010-01-22-09-ef12a1e112d21f31431b3c4535d1d3a13",
            result);
  }

  @Test
  public void testGetPathNullStart() {
    String id = "2010-01-22-09-ef12a1e112d21f31431b3c4535d1d3a13";
    String result = ActivityUtils.getPathFromId(id, null);
    Assert.assertEquals(
        "/2010/01/22/09/2010-01-22-09-ef12a1e112d21f31431b3c4535d1d3a13",
        result);
  }

}
