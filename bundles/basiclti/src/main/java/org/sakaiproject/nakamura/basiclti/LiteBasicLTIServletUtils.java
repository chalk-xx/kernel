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
package org.sakaiproject.nakamura.basiclti;

import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_KEY;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_SECRET;

import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permission;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LiteBasicLTIServletUtils {
  /**
   * The keys that must be specially secured from normal Sling operation.
   */
  protected static Set<String> sensitiveKeys;
  /**
   * The keys which we cannot set on a Node due to JCR semantics.
   */
  protected static Set<String> unsupportedKeys;

  static {
    sensitiveKeys = new HashSet<String>(2);
    sensitiveKeys.add(LTI_KEY);
    sensitiveKeys.add(LTI_SECRET);
    sensitiveKeys = Collections.unmodifiableSet(sensitiveKeys);

    unsupportedKeys = new HashSet<String>(5);
    unsupportedKeys.add("jcr:primaryType");
    unsupportedKeys.add("jcr:created");
    unsupportedKeys.add("jcr:createdBy");
    unsupportedKeys.add(":operation");
    unsupportedKeys.add("_MODIFIERS"); // TrimPath stuff
    unsupportedKeys = Collections.unmodifiableSet(unsupportedKeys);
  }

  /**
   * Helper method to return a Set of Privileges that a normal user <i>should not</i> have
   * on a sensitive Node.
   * 
   * @param acm
   * @return
   */
  protected static Set<Permission> getInvalidUserPrivileges(final AccessControlManager acm) {
    Set<Permission> invalidUserPrivileges = new HashSet<Permission>(10);
    invalidUserPrivileges.add(Permissions.ALL);
    invalidUserPrivileges.add(Permissions.CAN_ANYTHING);
    invalidUserPrivileges.add(Permissions.CAN_ANYTHING_ACL);
    invalidUserPrivileges.add(Permissions.CAN_DELETE);
    invalidUserPrivileges.add(Permissions.CAN_DELETE_ACL);
    invalidUserPrivileges.add(Permissions.CAN_MANAGE);
    invalidUserPrivileges.add(Permissions.CAN_READ);
    invalidUserPrivileges.add(Permissions.CAN_READ_ACL);
    invalidUserPrivileges.add(Permissions.CAN_WRITE);
    invalidUserPrivileges.add(Permissions.CAN_WRITE_ACL);
    return invalidUserPrivileges;
  }

  /**
   * Checks to see if the current user is a member of the administrators group.
   * 
   * @param session
   * @return
   */
  protected static boolean isAdminUser(final Session session) {
    // would be nice to support multiple admin users
    return User.ADMIN_USER.equals(session.getUserId());
  }

  /**
   * Quietly removes a Property on a Node if it exists.
   * 
   * @param node
   * @param property
   */
  protected static void removeProperty(final Content node, final String property) {
    if (node.hasProperty(property)) {
      node.removeProperty(property);
    }
  }

}
