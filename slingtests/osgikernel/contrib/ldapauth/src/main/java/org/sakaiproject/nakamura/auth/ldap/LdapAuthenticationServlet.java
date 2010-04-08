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
package org.sakaiproject.nakamura.auth.ldap;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Constants;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * Servlet for handling authentication requests for ldap authentication.
 */
@Component
@Service(value = Servlet.class)
public class LdapAuthenticationServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Property(value = "/system/sling/ldap/login")
  static final String SERVLET_PATH = "sling.servlet.paths";

  @Property(value = "Ldap Login Servlet")
  static final String DESCRIPTION = Constants.SERVICE_DESCRIPTION;

  @Property(value = "The Sakai Foundation")
  static final String VENDOR = Constants.SERVICE_VENDOR;

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");
  }
  
  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
  }
}
