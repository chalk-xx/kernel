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

import static org.sakaiproject.nakamura.api.casauth.CasAuthConstants.CAS_LOGIN_PATH;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Helper servlet for CAS authentication. The servlet simply redirects to
 * the configured CAS server via AuthenticationHandler requestCredentials.
 * To avoid a loop, if the request is already authenticated, the servlet redirects to
 * the path specified by the request parameter "resource", or to the root
 * path.
 * <p>
 * Once all authentication modules use Sling's authtype approach to trigger
 * requestCredentials, it should also be possible to reach CAS through any servlet
 * (including sling.commons.auth's LoginServlet) by setting the
 * sling:authRequestLogin request parameter to "CAS".
 */
@ServiceDocumentation(name="CAS Login Servlet", shortDescription=
    "Redirects to the configured CAS server, which will return on successful authentication.",
    bindings = @ServiceBinding(type=BindingType.PATH, bindings=CAS_LOGIN_PATH),
    methods = {
    @ServiceMethod(name="GET, POST", parameters={
        @ServiceParameter(name="resource", description="The path to return to (default is /)")
    })})
@SlingServlet(paths = { CAS_LOGIN_PATH }, methods = { "GET", "POST" })
public class CasLoginServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = -1894135945816269913L;
  private static final Logger LOGGER = LoggerFactory.getLogger(CasLoginServlet.class);

  @Reference
  protected transient CasAuthenticationHandler casAuthenticationHandler;

  @Override
  protected void service(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    // Check for possible loop after authentication.
    if (request.getAuthType() != null) {
      String redirectTarget = casAuthenticationHandler.getReturnPath(request);
      if ((redirectTarget == null) || request.getRequestURI().equals(redirectTarget)) {
        redirectTarget = request.getContextPath() + "/";
      }
      LOGGER.info("Request already authenticated, redirecting to {}", redirectTarget);
      response.sendRedirect(redirectTarget);
      return;
    }

    // Pass control to the handler.
    if (!casAuthenticationHandler.requestCredentials(request, response)) {
      LOGGER.error("Unable to request credentials from handler");
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot login");
    }
  }
}
