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

import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

public class SparseUser extends SparseAuthorizable implements User {

  private static final Logger LOGGER = LoggerFactory.getLogger(SparseUser.class);
  private String oldPassword;

  public SparseUser(org.sakaiproject.nakamura.api.lite.authorizable.User user,
      AuthorizableManager authorizableManager, AccessControlManager accessControlManager,
      ValueFactory valueFactory) {
    super(user, authorizableManager, accessControlManager, valueFactory);
    String userId = user.getId();
    if (org.sakaiproject.nakamura.api.lite.authorizable.User.ADMIN_USER.equals(userId)) {
      this.principal = new AdminPrincipal(userId);
    } else if (org.sakaiproject.nakamura.api.lite.authorizable.User.ANON_USER
        .equals(userId)) {
      this.principal = new AnonymousPrincipal();
    } else if (org.sakaiproject.nakamura.api.lite.authorizable.User.SYSTEM_USER
        .equals(userId)) {
      this.principal = new SystemPrincipal();
    } else {
      this.principal = new SparsePrincipal(userId, this.getClass().getName(),
          SparseMapUserManager.USERS_PATH);
    }

  }

  public boolean isAdmin() {
    return getSparseUser().isAdmin();
  }

  org.sakaiproject.nakamura.api.lite.authorizable.User getSparseUser() {
    return (org.sakaiproject.nakamura.api.lite.authorizable.User) sparseAuthorizable;
  }

  public Credentials getCredentials() throws RepositoryException {
    try {
      return new SparseCredentials(getSparseUser());
    } catch (NoSuchAlgorithmException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (UnsupportedEncodingException e) {
      LOGGER.error(e.getMessage(), e);
    }
    return null;
  }

  @Override
  public void setProperty(String name, Value value) throws RepositoryException {
    if (":oldpassword".equals(name)) {
      oldPassword = value.getString();
    } else {
      super.setProperty(name, value);
    }
  }

  public Impersonation getImpersonation() throws RepositoryException {
    return new SparseImpersonationImpl(this);
  }

  public void changePassword(String password) throws RepositoryException {
    try {
      authorizableManager.changePassword(getSparseUser(), password, oldPassword);
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new javax.jcr.AccessDeniedException(e.getMessage(), e);
    } finally {
      oldPassword = null;
    }
  }

}
