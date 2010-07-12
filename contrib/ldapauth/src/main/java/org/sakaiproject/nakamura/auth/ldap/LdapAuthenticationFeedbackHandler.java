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
package org.sakaiproject.nakamura.auth.ldap;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.auth.spi.AuthenticationFeedbackHandler;
import org.apache.sling.commons.auth.spi.AuthenticationInfo;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Authentication feedback handler for provisioning a JCR system user after successful
 * authentication processing. Attempt to create user is only tried if no
 * {@link Authorizable} is found for the provided username.
 */
@Component
@Service
public class LdapAuthenticationFeedbackHandler implements AuthenticationFeedbackHandler {
  private static final Logger logger = LoggerFactory
      .getLogger(LdapAuthenticationFeedbackHandler.class);

  @Reference
  private AuthorizablePostProcessService authzPostProcessorService;

  @Reference
  private SlingRepository slingRepository;

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.commons.auth.spi.AuthenticationFeedbackHandler#authenticationFailed(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse,
   *      org.apache.sling.commons.auth.spi.AuthenticationInfo)
   */
  public void authenticationFailed(HttpServletRequest arg0, HttpServletResponse arg1,
      AuthenticationInfo arg2) {
    // Nothing for us to do.
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.commons.auth.spi.AuthenticationFeedbackHandler#authenticationSucceeded(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse,
   *      org.apache.sling.commons.auth.spi.AuthenticationInfo)
   */
  public boolean authenticationSucceeded(HttpServletRequest req,
      HttpServletResponse resp, AuthenticationInfo authInfo) {
    try {
      Session session = slingRepository.loginAdministrative(null);
      UserManager um = AccessControlUtil.getUserManager(session);
      Authorizable auth = um.getAuthorizable(authInfo.getUser());

      if (auth == null) {
        String password = RandomStringUtils.random(8);
        User user = um.createUser(authInfo.getUser(), password);

        String userPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX
            + user.getID();
        authzPostProcessorService
            .process(user, session, Modification.onCreated(userPath));
      }
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    return false;
  }

}
