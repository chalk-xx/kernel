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
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * The <code>FormLoginServlet</code> provides an end point to login against. On GET it
 * will response with the remote username of the logged in user or "anonymous" if there is
 * no logged in user. On POST, the FormAutenticationHandler will be invoked. see
 * {@link FormAuthenticationHandler} to see the parameters.
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
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    response.getWriter().write(request.getRemoteUser());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Authenticator authenticator = this.authenticator;
    if (authenticator != null) {
      try {
        HttpSession session = request.getSession(false);
        LOGGER.info("Servlet Session ID is     :"+session.getId());
        LOGGER.info("Servlet Session is New    :"+session.isNew());
        LOGGER.info("Servlet Session Created at:"+new Date(session.getCreationTime()));
        LOGGER.info("Servlet Session Last Accessed "+new Date(session.getLastAccessedTime()));
        LOGGER.info("Servlet Trace",new Exception("Servlet TRACEBACK IGNORE"));

        authenticator.login(request, response);
        
        doGet(request, response);
        return;
      } catch (IllegalStateException ise) {
        LOGGER.error("doPOST: Response already committed, cannot login");
        return;
      } catch (NoAuthenticationHandlerException nahe) {
        LOGGER.error("doPOST: No AuthenticationHandler to login registered");
      }
    } else {
      LOGGER
          .error("doPOST: Authenticator service missing, cannot request authentication");
    }

    // fall back to forbid access
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot login");
  }

}
