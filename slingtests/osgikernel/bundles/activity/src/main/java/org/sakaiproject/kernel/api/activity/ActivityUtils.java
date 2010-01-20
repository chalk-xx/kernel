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
package org.sakaiproject.kernel.api.activity;

import static org.sakaiproject.kernel.api.activity.ActivityConstants.EVENT_TOPIC;

import org.osgi.service.event.Event;
import org.sakaiproject.kernel.api.personal.PersonalUtils;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 *
 */
public class ActivityUtils {

  @SuppressWarnings("unchecked")
  public static Event createEvent(String activityItemPath) {
    final Dictionary<String, String> map = new Hashtable(1);
    map.put(ActivityConstants.EVENT_PROP_PATH, activityItemPath);
    return new Event(EVENT_TOPIC, (Dictionary) map);
  }

  public static String getUserFeed(String user) {
    return PersonalUtils.getPrivatePath(user,
        ActivityConstants.ACTIVITY_FEED_NAME);
  }

}
