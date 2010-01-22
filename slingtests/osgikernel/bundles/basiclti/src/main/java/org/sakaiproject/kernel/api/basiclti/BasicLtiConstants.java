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
package org.sakaiproject.kernel.api.basiclti;

public class BasicLtiConstants {
  // launch settings per spec - computed not stored
  public static final String USER_ID = "user_id";
  public static final String LIS_PERSON_NAME_FULL = "lis_person_name_full";
  public static final String LIS_PERSON_NAME_FAMILY = "lis_person_name_family";
  public static final String LIS_PERSON_CONTACT_EMAIL_PRIMARY = "lis_person_contact_email_primary";
  public static final String CONTEXT_ID = "context_id";
  public static final String CONTEXT_TITLE = "context_title";
  public static final String ROLES = "roles";

  // local semantics
  public static final String LTI_URL = "ltiurl";
  public static final String LTI_URL_LOCK = "ltiurl_lock";
  public static final String LTI_SECRET = "ltisecret";
  public static final String LTI_SECRET_LOCK = "ltisecret_lock";
  public static final String LTI_KEY = "ltikey";
  public static final String LTI_KEY_LOCK = "ltikey_lock";
  public static final String FRAME_HEIGHT = "frame_height";
  public static final String FRAME_HEIGHT_LOCK = "frame_height_lock";
  public static final String DEBUG = "debug";
  public static final String DEBUG_LOCK = "debug_lock";
  public static final String RELEASE_NAMES = "release_names";
  public static final String RELEASE_NAMES_LOCK = "release_names_lock";
  public static final String RELEASE_EMAIL = "release_email";
  public static final String RELEASE_EMAIL_LOCK = "release_email_lock";
  public static final String RELEASE_PRINCIPAL_NAME = "release_principal_name";
  public static final String RELEASE_PRINCIPAL_NAME_LOCK = "release_principal_name_lock";

  // global settings (need to be moved later)
  public static final String tool_consumer_instance_name = "tool_consumer_instance_name";
  public static final String tool_consumer_instance_description = "tool_consumer_instance_description";
  public static final String tool_consumer_instance_contact_email = "tool_consumer_instance_contact_email";
  public static final String tool_consumer_instance_guid = "tool_consumer_instance_guid";
  public static final String tool_consumer_instance_url = "tool_consumer_instance_url";
}
