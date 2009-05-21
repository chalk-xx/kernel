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
 * Authentication handler for internally trusted users.
 *
 * @scr.component immediate="false" label="%auth.http.name"
 *                description="%auth.http.description"
 * @scr.service interface="org.apache.sling.engine.auth.AuthenticationHandler"
 */
public class TrustedAuthenticationHandler implements AuthenticationHandler {
  public static final String TRUSTED_AUTH = TrustedAuthenticationHandler.class.getName();

  /** @scr.property value="/" */
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
    HttpSession session = request.getSession();
    AuthenticationInfo authInfo = null;

    if (session.getAttribute("") != null) {
      authInfo = (AuthenticationInfo) session.getAttribute("");
    } else {
      String user = null;

      if (request.getUserPrincipal() != null) {
        user = request.getUserPrincipal().getName();
      } else if (request.getRemoteUser() != null) {
        user = request.getRemoteUser();
      }

      if (user != null) {
        request.getSession().setAttribute("trusted", Boolean.TRUE);
        SimpleCredentials cred = new SimpleCredentials(user, null);
        authInfo = new AuthenticationInfo(TRUSTED_AUTH, cred);
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

}
