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
package org.sakaiproject.kernel.auth.trusted;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.engine.auth.AuthenticationHandler;
import org.apache.sling.engine.auth.AuthenticationInfo;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.sakaiproject.kernel.auth.trusted.TrustedAuthenticationServlet.TrustedUser;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

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

/**
 * Authentication handler for trusted authentication sources. These sources will
 * authenticate users externally and eventually pass through this handler to
 * establish a trusted relationship continuing into the container.
 */
@Component(enabled = false)
@Service
public class TrustedAuthenticationHandler implements AuthenticationHandler, LoginModulePlugin {
  /**
   * Authentication type name
   */
  public static final String TRUSTED_AUTH = TrustedAuthenticationHandler.class.getName();

  /**
   * Attribute name for storage of authentication
   */
  public static final String USER_CREDENTIALS = TrustedAuthentication.class.getName();

  /**
   * Path on which this authentication should be activated.
   */
  @Property(value = "/")
  static final String PATH_PROPERTY = AuthenticationHandler.PATH_PROPERTY;

  @Property(value = "Trusted Authentication Handler")
  static final String DESCRIPTION_PROPERTY = "service.description";

  @Property(value = "The Sakai Foundation")
  static final String VENDOR_PROPERTY = "service.vendor";

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.engine.auth.AuthenticationHandler#authenticate(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public AuthenticationInfo authenticate(HttpServletRequest req, HttpServletResponse resp) {
    // check for existing authentication information in session
    TrustedAuthentication auth = new TrustedAuthentication(req);
    req.setAttribute(USER_CREDENTIALS, auth);

    // construct the authentication info and store credentials on the request
    Credentials cred = auth.getCredentials();
    AuthenticationInfo authInfo = new AuthenticationInfo(TRUSTED_AUTH, cred);

    return authInfo;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.engine.auth.AuthenticationHandler#requestAuthentication(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public boolean requestAuthentication(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#canHandle(javax.jcr.Credentials)
   */
  public boolean canHandle(Credentials cred) {
    boolean hasAttribute = false;

    if (cred != null && cred instanceof SimpleCredentials) {
      Object attr = ((SimpleCredentials) cred).getAttribute(getClass().getName());
      hasAttribute = (attr != null);
    }

    return hasAttribute;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#doInit(javax.security.auth.callback.CallbackHandler,
   *      javax.jcr.Session, java.util.Map)
   */
  @SuppressWarnings("unchecked")
  public void doInit(CallbackHandler callbackHandler, Session session, Map options)
      throws LoginException {
    return;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#getAuthentication(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public AuthenticationPlugin getAuthentication(Principal principal, Credentials creds)
      throws RepositoryException {
    return new TrustedAuthenticationPlugin(principal);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#getPrincipal(javax.jcr.Credentials)
   */
  public Principal getPrincipal(Credentials credentials) {
    Principal principal = null;
    if (credentials != null && credentials instanceof SimpleCredentials) {
      SimpleCredentials sc = (SimpleCredentials) credentials;
      TrustedUser user = (TrustedUser) sc.getAttribute(getClass().getName());
      if (user != null) {
        principal = new TrustedPrincipal(user);
      }
    }
    return principal;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin#impersonate(java.security.Principal,
   *      javax.jcr.Credentials)
   */
  public int impersonate(Principal principal, Credentials credentials) throws RepositoryException,
      FailedLoginException {
    return LoginModulePlugin.IMPERSONATION_DEFAULT;
  }

  /**
   * Authentication information for storage in session and/or request.<br/>
   * <br/>
   * By being an inner, static class with a private constructor, it is harder
   * for an external source to inject into the authentication chain.
   */
  static final class TrustedAuthentication {
    private final Credentials cred;

    private TrustedAuthentication(HttpServletRequest req) {
      HttpSession session = req.getSession(false);
      if (session != null) {
        cred = (Credentials) session.getAttribute(USER_CREDENTIALS);
      } else {
        cred = null;
      }
    }

    Credentials getCredentials() {
      return cred;
    }

    boolean isValid() {
      return cred != null;
    }
  }
}
