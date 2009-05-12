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
package org.sakaiproject.kernel.formauth;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.engine.auth.Authenticator;
import org.apache.sling.engine.auth.NoAuthenticationHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>FormLoginServlet</code> provides an end point to login against, accepts post operations
 * 
 * @scr.component metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description" value="Form Login Servlet"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.paths" value="/system/sling/formlogin"
 * @scr.property name="sling.servlet.methods" values.0="GET" values.1="POST" 
 */
public class FormLoginServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = -6303432993222973296L;

  private static final Logger LOGGER = LoggerFactory.getLogger(FormLoginServlet.class);
  
  /**
   * @scr.reference cardinality="0..1" policy="dynamic" 
   */
  private Authenticator authenticator;

  
  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    response.getWriter().write(request.getRemoteUser());
  }
  
  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Authenticator authenticator = this.authenticator;
    if (authenticator != null) {
        try {
            authenticator.login(request, response);
            response.reset();
            response.sendRedirect(request.getRequestURI());
            return;
        } catch (IllegalStateException ise) {
            LOGGER.error("doPOST: Response already committed, cannot login");
            return;
        } catch (NoAuthenticationHandlerException nahe) {
            LOGGER.error("doPOST: No AuthenticationHandler to login registered");
        }
    } else {
        LOGGER.error("doPOST: Authenticator service missing, cannot request authentication");
    }

    // fall back to forbid access
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot login");  }

}
