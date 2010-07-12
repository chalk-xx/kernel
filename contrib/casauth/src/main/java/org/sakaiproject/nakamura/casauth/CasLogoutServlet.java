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
package org.sakaiproject.nakamura.casauth;

import static org.sakaiproject.nakamura.api.casauth.CasAuthConstants.CAS_LOGOUT_PATH;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.auth.Authenticator;
import org.sakaiproject.nakamura.api.casauth.CasAuthConstants;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * Helper servlet for CAS logout. First, the authentication handler
 * is given a chance to perform any special logout handling (notably
 * a redirect to log the user out of the SSO system). If the
 * handler passes, then the servlet initiates the standard Sling
 * logout logic (as in the sling.commons.auth LogoutServlet).
 */
@ServiceDocumentation(name="CAS Logout Servlet", shortDescription=
    "Log out of local authentication and then redirect to log out of the CAS server.",
    bindings = @ServiceBinding(type=BindingType.PATH, bindings=CasAuthConstants.CAS_LOGOUT_PATH),
    methods = {@ServiceMethod(name="GET, POST")})
@SlingServlet(paths = { CAS_LOGOUT_PATH }, methods = { "GET", "POST" })
public class CasLogoutServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = -8413852795940695719L;
  private static final Logger LOGGER = LoggerFactory.getLogger(CasLogoutServlet.class);

  @Reference
  protected transient CasAuthenticationHandler casAuthenticationHandler;

  @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY, policy=ReferencePolicy.DYNAMIC)
  private Authenticator authenticator;

  @Override
  protected void service(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    if (!casAuthenticationHandler.casLogout(request, response)) {
      final Authenticator authenticator = this.authenticator;
      if (authenticator != null) {
        try {
          authenticator.logout(request, response);
        } catch (IllegalStateException ise) {
          LOGGER.error("service: Response already committed, cannot logout");
          return;
        }
      }
    }
  }
}
