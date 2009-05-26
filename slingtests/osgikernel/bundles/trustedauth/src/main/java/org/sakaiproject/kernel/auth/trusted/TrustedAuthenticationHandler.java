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

import org.apache.sling.engine.auth.AuthenticationHandler;
import org.apache.sling.engine.auth.AuthenticationInfo;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Authentication handler for trusted authentication sources. These sources will
 * authenticate users externally and eventually pass through this handler to
 * establish a trusted relationship continuing into the container.
 *
 * @scr.component immediate="false" label="%auth.http.name"
 *                description="%auth.http.description"
 * @scr.service interface="org.apache.sling.engine.auth.AuthenticationHandler"
 */
public class TrustedAuthenticationHandler implements AuthenticationHandler {
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
   *
   * @scr.property value="/trusted"
   */
  static final String PATH_PROPERTY = AuthenticationHandler.PATH_PROPERTY;

  /** @scr.property value="Trusted Authentication Handler" */
  static final String DESCRIPTION_PROPERTY = "service.description";

  /** @scr.property value="The Sakai Foundation" */
  static final String VENDOR_PROPERTY = "service.vendor";

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.engine.auth.AuthenticationHandler#authenticate(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public AuthenticationInfo authenticate(HttpServletRequest req, HttpServletResponse resp) {
    AuthenticationInfo authInfo = null;
    Credentials cred = null;

    // check for existing authentication already in session
    HttpSession session = req.getSession(false);
    if (session != null && session.getAttribute(USER_CREDENTIALS) != null) {
      cred = (Credentials) session.getAttribute(USER_CREDENTIALS);
    } else {
      TrustedAuthentication auth = new TrustedAuthentication(req);
      req.setAttribute(USER_CREDENTIALS, auth);
      if (auth.isValid()) {
        cred = auth.getCredentials();
      }
    }

    // if a user is available, construct the authentication info and store
    // credentials on the request
    if (cred != null) {
      authInfo = new AuthenticationInfo(TRUSTED_AUTH, cred);
    }

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
   * Authentication information for storage in session and/or request. By being
   * an inner, non-static class, it is harder for an external source to inject
   * into the authentication chain.
   */
  protected static final class TrustedAuthentication {
    private final boolean valid;
    private final Credentials cred;

    TrustedAuthentication(HttpServletRequest req) {
      String user = getUser(req);
      if (user != null) {
        cred = new SimpleCredentials(user, null);
        valid = true;
      } else {
        cred = null;
        valid = false;
      }
    }

    Credentials getCredentials() {
      return cred;
    }

    boolean isValid() {
      return valid;
    }

    private String getUser(HttpServletRequest req) {
      String user = null;
      if (req.getUserPrincipal() != null) {
        user = req.getUserPrincipal().getName();
      } else if (req.getRemoteUser() != null) {
        user = req.getRemoteUser();
      }
      return user;
    }
  }
}
