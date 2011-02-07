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
package org.sakaiproject.nakamura.lite.jackrabbit;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.core.security.principal.PrincipalIteratorAdapter;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.util.Iterables;
import org.sakaiproject.nakamura.api.lite.util.PreemptiveIterator;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.security.auth.Subject;

public class SparseImpersonationImpl implements Impersonation {

  private SparseUser sparseUser;

  public SparseImpersonationImpl(SparseUser sparseUser) {
    this.sparseUser = sparseUser;
  }

  @SuppressWarnings("unchecked")
  public PrincipalIterator getImpersonators() throws RepositoryException {
    User u = sparseUser.getSparseUser();
    String impersonators = (String) u
        .getProperty(User.PRINCIPALS_FIELD);
    if (impersonators == null) {
      return new PrincipalIteratorAdapter(Collections.EMPTY_LIST);
    }
    final Iterator<String> imperson = Iterables.of(StringUtils.split(impersonators, ';'))
        .iterator();
    return new PrincipalIteratorAdapter(new PreemptiveIterator<Principal>() {

      private Principal principal;

      protected boolean internalHasNext() {
        if (imperson.hasNext()) {
          String userId = imperson.next();
          if (User.ADMIN_USER.equals(userId)) {
            principal = new AdminPrincipal(userId);
          } else if (User.SYSTEM_USER.equals(userId)) {
            principal = new SystemPrincipal();
          } else {
            principal = new SparsePrincipal(imperson.next(), this.getClass().getName(),
                SparseMapUserManager.USERS_PATH);
          }
          return true;
        }
        principal = null;
        return false;
      }

      protected Principal internalNext() {
        return principal;
      }

    });
  }

  public boolean grantImpersonation(Principal principal) throws RepositoryException {
    if (principal instanceof AdminPrincipal || principal instanceof SystemPrincipal) {
      return false;
    }
    User u = sparseUser.getSparseUser();
    String impersonators = (String) u
        .getProperty(User.IMPERSONATORS_FIELD);
    Set<String> imp = new HashSet<String>();
    Collections.addAll(imp, StringUtils.split(impersonators, ';'));
    String name = principal.getName();
    if (!imp.contains(name)) {
      imp.add(name);
      u.setProperty(User.PRINCIPALS_FIELD,StringUtils.join(imp, ';'));
      sparseUser.save();
      return true;
    }
    return false;
  }

  public boolean revokeImpersonation(Principal principal) throws RepositoryException {
    if (principal instanceof AdminPrincipal || principal instanceof SystemPrincipal) {
      return false;
    }
    User u = sparseUser.getSparseUser();
    String impersonators = (String) u
        .getProperty(User.IMPERSONATORS_FIELD);
    Set<String> imp = new HashSet<String>();
    Collections.addAll(imp, StringUtils.split(impersonators, ';'));
    String name = principal.getName();
    if (imp.contains(name)) {
      imp.remove(name);
      u.setProperty(User.PRINCIPALS_FIELD,
          StringUtils.join(imp, ';'));
      sparseUser.save();
      return true;
    }
    return false;
  }

  public boolean allows(Subject subject) throws RepositoryException {
    if (!subject.getPrincipals(AdminPrincipal.class).isEmpty()
        || !subject.getPrincipals(SystemPrincipal.class).isEmpty()) {
      return true;
    }
    User u = sparseUser.getSparseUser();
    String impersonators = (String) u
        .getProperty(User.IMPERSONATORS_FIELD);
    Set<String> imp = new HashSet<String>();
    Collections.addAll(imp, StringUtils.split(impersonators, ';'));
    for (Principal p : subject.getPrincipals()) {
      if (imp.contains(p.getName())) {
        return true;
      }
    }
    return false;
  }

}
