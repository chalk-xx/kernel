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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class TrustedAuthServlet extends HttpServlet {

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
    // if authentication missing or invalid, send user to authentication
    // authority
    else {
      // @TODO send to authentication authority
    }
  }
}
