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
  /**
   * resource_link_id=88391-e1919-bb3456
   * <p>
   * This is an opaque unique identifier that the TC guarantees will be unique
   * within the TC for every placement of the link. If the tool / activity is
   * placed multiple times in the same context, each of those placements will be
   * distinct. This value will also change if the item is exported from one
   * system or context and imported into another system or context. This
   * parameter is required.
   */
  public static final String RESOURCE_LINK_ID = "resource_link_id";
  /**
   * user_id=0ae836b9-7fc9-4060-006f-27b2066ac545
   * <p>
   * Uniquely identifies the user. This should not contain any identifying
   * information for the user. Best practice is that this field should be a
   * TC-generated long-term "primary key" to the user record - not the logical
   * key. This parameter is recommended.
   */
  public static final String USER_ID = "user_id";
  /**
   * roles=Instructor,Student
   * <p>
   * A comma-separated list of URN values for roles. If this list is non-empty,
   * it should contain at least one role from the LIS System Role, LIS
   * Institution Role, or LIS Context Role vocabularies (See Appendix A). The
   * assumed namespace of these URNs is the LIS vocabulary of LIS Context Roles
   * so TCs can use the handles when the intent is to refer to an LIS context
   * role. If the TC wants to include a role from another namespace, a
   * fully-qualified URN should be used. Usage of roles from non-LIS
   * vocabularies is discouraged as it may limit interoperability. This
   * parameter is recommended.
   */
  public static final String ROLES = "roles";
  /**
   * lis_person_name_given=Jane
   * <p>
   * These fields contain information about the user account that is performing
   * this launch. The names of these data items are taken from LIS. The precise
   * meaning of the content in these fields is defined by LIS.
   */
  public static final String LIS_PERSON_NAME_GIVEN = "lis_person_name_given";
  /**
   * lis_person_name_family=Public
   * <p>
   * These fields contain information about the user account that is performing
   * this launch. The names of these data items are taken from LIS. The precise
   * meaning of the content in these fields is defined by LIS.
   */
  public static final String LIS_PERSON_NAME_FAMILY = "lis_person_name_family";
  /**
   * lis_person_name_full=Jane Q. Public
   * <p>
   * These fields contain information about the user account that is performing
   * this launch. The names of these data items are taken from LIS. The precise
   * meaning of the content in these fields is defined by LIS.
   */
  public static final String LIS_PERSON_NAME_FULL = "lis_person_name_full";
  /**
   * lis_person_contact_email_primary=user@school.edu
   * <p>
   * These fields contain information about the user account that is performing
   * this launch. The names of these data items are taken from LIS. The precise
   * meaning of the content in these fields is defined by LIS.
   */
  public static final String LIS_PERSON_CONTACT_EMAIL_PRIMARY = "lis_person_contact_email_primary";
  /**
   * context_id=8213060-006f-27b2066ac545
   * <p>
   * This is an opaque identifier that uniquely identifies the context that
   * contains the link being launched.
   */
  public static final String CONTEXT_ID = "context_id";
  /**
   * context_type=CourseSection
   * <p>
   * This string is a comma-separated list of URN values that identify the type
   * of context. At a minimum, the list MUST include a URN value drawn from the
   * LIS vocabulary (see Appendix A). The assumed namespace of these URNs is the
   * LIS vocabulary so TCs can use the handles when the intent is to refer to an
   * LIS context type. If the TC wants to include a context type from another
   * namespace, a fully-qualified URN should be used.
   */
  public static final String CONTEXT_TYPE = "context_type";
  /**
   * context_title=Design of Personal Environments
   * <p>
   * A title of the context - it should be about the length of a line.
   */
  public static final String CONTEXT_TITLE = "context_title";
  /**
   * context_label=SI182
   * <p>
   * A label for the context - intended to fit in a column.
   */
  public static final String CONTEXT_LABEL = "context_label";
  /**
   * launch_presentation_locale=en_US_variant
   * <p>
   * Language, country and variant separated by underscores. Language is the
   * lower-case, two-letter code as defined by ISO-639 (list of codes available
   * at http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt). Country is the
   * upper-case, two-letter code as defined by ISO-3166 (list of codes available
   * at http://www.chemie.fu- berlin.de/diverse/doc/ISO_3166.html). Country and
   * variant codes are optional.
   */
  public static final String LAUNCH_PRESENTATION_LOCALE = "launch_presentation_locale";
  /**
   * launch_presentation_document_target=iframe
   * <p>
   * The value should be either 'frame', 'iframe' or 'window'. This field
   * communicates the kind of browser window/frame where the TC has launched the
   * tool.
   */
  public static final String LAUNCH_PRESENTATION_DOCUMENT_TARGET = "launch_presentation_document_target";

  // global settings
  /**
   * tool_consumer_instance_guid=lmsng.school.edu
   * <p>
   * This is a key to be used when setting a TC-wide password. The TP uses this
   * as a key to look up the TC-wide secret when validating a message. A common
   * practice is to use the DNS of the organization or the DNS of the TC
   * instance. If the organization has multiple TC instances, then the best
   * practice is to prefix the domain name with a locally unique identifier for
   * the TC instance. This parameter is recommended.
   */
  public static final String tool_consumer_instance_guid = "tool_consumer_instance_guid";
  /**
   * tool_consumer_instance_name=SchoolU
   * <p>
   * This is a user visible field - it should be about the length of a column.
   */
  public static final String tool_consumer_instance_name = "tool_consumer_instance_name";
  /**
   * tool_consumer_instance_description=University of School (LMSng)
   * <p>
   * This is a user visible field - it should be about the length of a line.
   */
  public static final String tool_consumer_instance_description = "tool_consumer_instance_description";
  /**
   * tool_consumer_instance_contact_email=System.Admin@school.edu
   * <p>
   * An email contact for the TC instance.
   */
  public static final String tool_consumer_instance_contact_email = "tool_consumer_instance_contact_email";
  /**
   * Missing from impl guide. Needs documentation. Not required, but "tasty".
   */
  public static final String tool_consumer_instance_url = "tool_consumer_instance_url";

  // local service/widget semantics
  public static final String LTI_VTOOL_ID = "lti_virtual_tool_id";
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
}
