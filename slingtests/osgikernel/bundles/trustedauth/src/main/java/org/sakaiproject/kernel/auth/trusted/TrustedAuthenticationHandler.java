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
  static final String USER_CREDENTIALS = TrustedAuthentication.class.getName();

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
  public AuthenticationInfo authenticate(HttpServletRequest request, HttpServletResponse response) {
    HttpSession session = request.getSession(false);
    AuthenticationInfo authInfo = null;

    TrustedAuthentication notes = null;
    String user = null;

    // check for existing authentication already in session
    if (session != null && session.getAttribute(USER_CREDENTIALS) != null) {
      notes = (TrustedAuthentication) session.getAttribute(USER_CREDENTIALS);
      user = notes.getUser();
    }
    // nothing in session, get the user from the request
    else {
      if (request.getUserPrincipal() != null) {
        user = request.getUserPrincipal().getName();
      } else if (request.getRemoteUser() != null) {
        user = request.getRemoteUser();
      }
    }

    // if a user is available, construct the authentication info and store where
    // appropriate
    if (user != null) {
      SimpleCredentials cred = new SimpleCredentials(user, null);
      authInfo = new AuthenticationInfo(TRUSTED_AUTH, cred);
      // make sure the session is available and the authentication wasn't
      // previously retrieved from the session
      if (session != null && notes == null) {
        notes = new TrustedAuthentication(user);
        session.setAttribute(USER_CREDENTIALS, notes);
      }

      // if the authentication is available, store it on the request for
      // downstream use
      if (notes != null) {
        request.setAttribute(USER_CREDENTIALS, notes);
      }
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
  final class TrustedAuthentication {
    private final String user;

    private TrustedAuthentication(String user) {
      this.user = user;
    }

    String getUser() {
      return user;
    }
  }
}
