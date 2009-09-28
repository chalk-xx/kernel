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

/**
 * HTTP Stuff
 */
public interface ActivityConstants {
  public static final String REQUEST_PARAM_APPLICATION_ID = "applicationId";
  public static final String REQUEST_PARAM_TEMPLATE_ID = "templateId";
  public static final String ACTIVITY_STORE_NAME = "activity";

  /**
   * JCR folder name for templates.
   */
  public static final String TEMPLATE_ROOTFOLDER = "/var/activity/templates";
  public static final String ACTIVITY_STORE_RESOURCE_TYPE = "sakai/activityStore";
  public static final String ACTIVITY_FEED_RESOURCE_TYPE = "sakai/activityFeed";
  public static final String PROPERTY_ROOT = "sakaiActivityFeed";
  public static final String ACTOR_PROPERTY = PROPERTY_ROOT + "Actor";
  public static final String SOURCE_PROPERTY = PROPERTY_ROOT + "Source";

  /**
   * Events
   */
  public static final String EVENT_TOPIC = "org/sakaiproject/kernel/activity";

}
