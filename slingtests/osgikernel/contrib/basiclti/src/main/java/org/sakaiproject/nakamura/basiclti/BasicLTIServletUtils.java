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

import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.LTI_KEY;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.LTI_SECRET;

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.sling.jcr.base.util.AccessControlUtil;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

public class BasicLTIServletUtils {
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
   * Helper method to return a Set of Privileges that a normal user <i>should
   * not</i> have on a sensitive Node.
   * 
   * @param acm
   * @return
   * @throws AccessControlException
   * @throws RepositoryException
   */
  protected static Set<Privilege> getInvalidUserPrivileges(
      final AccessControlManager acm) throws AccessControlException,
      RepositoryException {
    Set<Privilege> invalidUserPrivileges = new HashSet<Privilege>(9);
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_ALL));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_READ));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_WRITE));
    invalidUserPrivileges.add(acm
        .privilegeFromName(Privilege.JCR_MODIFY_PROPERTIES));
    invalidUserPrivileges.add(acm
        .privilegeFromName(Privilege.JCR_READ_ACCESS_CONTROL));
    invalidUserPrivileges.add(acm
        .privilegeFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL));
    invalidUserPrivileges.add(acm
        .privilegeFromName(Privilege.JCR_ADD_CHILD_NODES));
    invalidUserPrivileges.add(acm
        .privilegeFromName(Privilege.JCR_REMOVE_CHILD_NODES));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_REMOVE_NODE));
    return invalidUserPrivileges;
  }

  /**
   * Checks to see if the current user is a member of the administrators group.
   * 
   * @param session
   * @return
   * @throws UnsupportedRepositoryOperationException
   * @throws RepositoryException
   */
  protected static boolean isAdminUser(final Session session)
      throws UnsupportedRepositoryOperationException, RepositoryException {
    final UserManager userManager = AccessControlUtil.getUserManager(session);
    final Authorizable authorizable = userManager.getAuthorizable(session
        .getUserID());
    boolean isAdmin = false;
    if (authorizable != null) {
      final Principal principal = authorizable.getPrincipal();
      if (principal != null) {
        final PrincipalManager principalManager = AccessControlUtil
            .getPrincipalManager(session);
        final PrincipalIterator it = principalManager
            .getGroupMembership(principal);
        while (it.hasNext()) {
          if (SecurityConstants.ADMINISTRATORS_NAME.equals(it.nextPrincipal()
              .getName())) {
            isAdmin = true;
            break;
          }
        }
      }
    }
    return isAdmin;
  }

}
