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

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.auth.spi.AuthenticationFeedbackHandler;
import org.apache.sling.commons.auth.spi.AuthenticationHandler;
import org.apache.sling.commons.auth.spi.AuthenticationInfo;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;

/**
 * Authentication handler for trusted authentication sources. These sources will
 * authenticate users externally and eventually pass through this handler to
 * establish a trusted relationship continuing into the container.
 */
@Component(metatype = true)
@Service
public class LdapAuthenticationHandler implements AuthenticationHandler {
  /** Authentication type name */
  public static final String LDAP_AUTH = LdapAuthenticationHandler.class.getName();

  /** Attribute name for storage of authentication info on the request */
  public static final String USER_AUTH = LdapAuthentication.class.getName();

  /** Field name to expect username */
  public static final String PARAM_USERNAME = "sakaiauth:un";

  /** Field name to expect password */
  public static final String PARAM_PASSWORD = "sakaiauth:pw";

  /** Path on which this authentication should be activated. */
  @Property(value = "/")
  static final String PATH_PROPERTY = AuthenticationHandler.PATH_PROPERTY;

  @Property(value = "LDAP Authentication Handler")
  static final String DESCRIPTION_PROPERTY = "service.description";

  @Property(value = "The Sakai Foundation")
  static final String VENDOR_PROPERTY = "service.vendor";

  @Reference
  private LdapAuthenticationPlugin ldapAuthenticationPlugin;

  /**
   * {@inheritDoc}
   * 
   * @param req
   * @param resp
   * @return
   * @see org.apache.sling.commons.auth.spi.AuthenticationHandler#extractCredentials(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public AuthenticationInfo extractCredentials(HttpServletRequest req, HttpServletResponse resp) {
    // 1. try credentials from POST'ed request parameters
    AuthenticationInfo info = extractRequestParameterAuthentication(req);

    // 2. try credentials from the cookie or session
    if (info == null) {
      // TODO get info from cookie/token storage
//      String authData = authStorage.extractAuthenticationInfo(request);
//      if (authData != null) {
//          if (tokenStore.isValid(authData)) {
//              info = createAuthInfo(authData);
//          } else {
//              // signal the requestCredentials method a previous login failure
//              request.setAttribute(PAR_J_REASON, FormReason.TIMEOUT);
//          }
//      }
      // the old stuff.  here till I figure out what is going on.
//      req.setAttribute(USER_AUTH, info);
//      info = new AuthenticationInfo(LDAP_AUTH);
//      info.put(AuthenticationInfo.CREDENTIALS, info.getCredentials());
    }

    return info;
  }

  /**
   * {@inheritDoc}
   * 
   * @param hsr
   * @param hsr1
   * @return
   * @throws IOException
   * @see org.apache.sling.commons.auth.spi.AuthenticationHandler#requestCredentials(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public boolean requestCredentials(HttpServletRequest hsr, HttpServletResponse hsr1) throws IOException {
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @param hsr
   * @param hsr1
   * @throws IOException
   * @see org.apache.sling.commons.auth.spi.AuthenticationHandler#dropCredentials(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void dropCredentials(HttpServletRequest hsr, HttpServletResponse hsr1) throws IOException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Authentication information for storage in session and/or request.<br/>
   * <br/>
   * By being an inner, static class with a private constructor, it is harder
   * for an external source to inject into the authentication chain.
   */
  private AuthenticationInfo extractRequestParameterAuthentication(HttpServletRequest req) {
    AuthenticationInfo info = null;

    if ("POST".equals(req.getMethod())) {
      String username = req.getParameter(PARAM_USERNAME);
      String password = req.getParameter(PARAM_PASSWORD);
      if (username != null && password != null) {
        info = new AuthenticationInfo(LDAP_AUTH, username, password.toCharArray());
      }
    }

    return info;
  }
}
