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
package org.sakaiproject.kernel.auth.ldap;

import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.auth.ldap.LdapAuthenticationHandler.LdapAuthentication;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

/**
 * Servlet for handling authentication requests for ldap authentication.
 */
@Service
@SlingServlet(paths = "/system/sling/ldaplogin", methods = "POST")
public class LdapAuthenticationServlet extends SlingAllMethodsServlet {
  public static final String USER_CREDENTIALS = LdapAuthenticationServlet.class.getName();
  private static final long serialVersionUID = 1L;

  @Property(value = "Form Login Servlet")
  static final String DESCRIPTION = "service.description";

  @Property(value = "The Sakai Foundation")
  static final String VENDOR = "service.vendor";

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest req, SlingHttpServletResponse resp)
      throws ServletException, IOException {
    HttpSession session = req.getSession(true);

    LdapAuthentication auth = (LdapAuthentication) req
        .getAttribute(LdapAuthenticationHandler.USER_AUTH);
    // check for authentication on request. if found, store on request
    if (auth != null && auth.getCredentials() != null) {
      session.setAttribute(USER_CREDENTIALS, auth.getCredentials());

      // TODO Should this forward to a landing page or some other resource?
    }
  }
}
