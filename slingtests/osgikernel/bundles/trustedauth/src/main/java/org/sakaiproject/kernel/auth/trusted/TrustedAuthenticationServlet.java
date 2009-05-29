/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.kernel.auth.trusted;

import org.sakaiproject.kernel.auth.trusted.TrustedAuthenticationHandler.TrustedAuthentication;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet for handling authentication requests from trusted mechanisms.
 */
public class TrustedAuthenticationServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    HttpSession session = req.getSession(true);

    TrustedAuthentication auth = (TrustedAuthentication) req
        .getAttribute(TrustedAuthenticationHandler.USER_CREDENTIALS);
    // check for authentication on request. if found, store on request
    if (auth != null && auth.isValid()) {
      session.setAttribute(TrustedAuthenticationHandler.USER_CREDENTIALS, auth.getCredentials());
    }
    // if authentication missing or invalid in session, get the information from
    // the request.
    else {
      Credentials cred = getCredentials(req);
      session.setAttribute(TrustedAuthenticationHandler.USER_CREDENTIALS, cred);
    }
  }

  /**
   * Get the user ID from the request. Currently checks getRemoteUser() and
   * getUserPrincipal() but may need to change based on how the external
   * authentication passes the user information back. Once the user is
   * determined, {@link Credentials} are constructed with the user and a trusted
   * attribute.
   *
   * @param req
   *          The request to sniff for a user.
   * @return
   */
  private Credentials getCredentials(HttpServletRequest req) {
    String userId = null;
    if (req.getUserPrincipal() != null) {
      userId = req.getUserPrincipal().getName();
    } else if (req.getRemoteUser() != null) {
      userId = req.getRemoteUser();
    }
    SimpleCredentials sc = new SimpleCredentials(userId, null);
    TrustedUser user = new TrustedUser(userId);
    sc.setAttribute(TrustedAuthenticationHandler.class.getName(), user);
    return sc;
  }

  /**
   * "Trusted" inner class for passing the user on to the authentication
   * handler.<br/>
   * <br/>
   * By being a static, inner class with a private constructor, it is harder for
   * an external source to inject into the authentication chain.
   */
  static final class TrustedUser {
    private final String user;

    /**
     * Constructor.
     *
     * @param user
     *          The user to represent.
     */
    private TrustedUser(String user) {
      this.user = user;
    }

    /**
     * Get the user that is being represented.
     *
     * @return The represented user.
     */
    String getUser() {
      return user;
    }
  }
}
