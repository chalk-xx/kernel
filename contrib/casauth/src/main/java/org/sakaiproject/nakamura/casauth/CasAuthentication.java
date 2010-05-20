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
package org.sakaiproject.nakamura.casauth;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public class CasAuthentication implements AuthenticationPlugin {
  private SlingRepository repository;

  private static final Logger LOGGER = LoggerFactory.getLogger(CasAuthentication.class);

  private Principal principal;

  public CasAuthentication(Principal principal, SlingRepository repository) {
    this.principal = principal;
    this.repository = repository;
  }

  public boolean authenticate(Credentials credentials) throws RepositoryException {
    final String principalName = principal.getName();
    if (credentials instanceof SimpleCredentials) {

      Session session = null;
      try {
        session = repository.loginAdministrative(null); // usage checked and ok KERN-577

        UserManager userManager = AccessControlUtil.getUserManager(session);
        Authorizable authorizable = userManager.getAuthorizable(principalName);

        if (authorizable == null) {
          // create user
          LOGGER.debug("Createing user {}", principalName);
          userManager.createUser(principalName, RandomStringUtils.random(32));
        }
      } catch (RepositoryException e) {
        LOGGER.error(e.getMessage(), e);
        throw (e);
      } finally {
        if (session != null) {
          session.logout();
        }
      }
    } else {
      throw new RepositoryException("Can't authenticate credentials of type: "
          + credentials.getClass());
    }
    return ((SimpleCredentials) credentials).getUserID().equals(principalName);
  }
}
