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
package org.sakaiproject.nakamura.trusted;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.auth.Authenticator;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenServiceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 *
 */
public abstract class AbstractAuthServlet extends SlingAllMethodsServlet {
  
  /**
   * 
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAuthServlet.class);
  private static final long serialVersionUID = 6272030379108477062L;

  public static final String TRY_LOGIN = "sakaiauth:login";

  public static final String PARAM_DESTINATION = "d";
  public static final String FORCE_LOGOUT = "sakaiauth:logout";


  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");

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
    try {
      if ("1".equals(request.getParameter(FORCE_LOGOUT))) {
        LOGGER.debug("logout");
        
        // TODO This closely mimics Sling's LogoutServlet. Once all clients switch their
        // logout links to "/system/sling/logout", we can remove this code.
        final Authenticator authenticator = getAuthenticator();
        if (authenticator != null) {
            try {
                final String resourcePath = request.getParameter(PARAM_DESTINATION);
                if (resourcePath != null) {
                  request.setAttribute(Authenticator.LOGIN_RESOURCE, resourcePath);
                }
                authenticator.logout(request, response);
                return;
            } catch (IllegalStateException ise) {
              LOGGER.error("service: Response already committed, cannot logout");
              return;
            }
        }
        LOGGER.error("service: Authenticator service missing, cannot logout");
      } else {

        // was the request just authenticated ?
        // If the Formauthentication object got to this point, a session was created and
        // logged in, therefore the
        // username and password have been checked by logging into the JCR. We can safely
        // capture the FormAuthentication
        // object in session. (or we could use the secure token at this point to avoid
        // session usage.)
        AbstractAuthentication authenticaton = (AbstractAuthentication) request
            .getAttribute(AbstractAuthenticationHandler.AUTHENTICATION_OBJECT);
        if (authenticaton != null) {
          if (authenticaton.isValid()) {

            // the request has now been authenticated, hence its valid.
            // just check that session userID is the same as the login user ID.
            Session session = request.getResourceResolver().adaptTo(Session.class);
            String userId = session.getUserID();
            String authUser = authenticaton.getUserId();
            if (userId.equals(authUser)) {
              TrustedTokenServiceWrapper trustedTokenServiceWrapper = getTokenWrapper();
              trustedTokenServiceWrapper.addToken(request, response);
            } else {
              LOGGER.warn("Authentication failed for {} session user was {}", authUser, userId);
              sendAuthenticationFailed(request, response);
              return;
            }
          }
          
          String destination = request.getParameter(PARAM_DESTINATION);

          if (destination != null) {
            // ensure that the redirect is safe and not susceptible to hacking
            response.sendRedirect(destination.replace('\n', ' ').replace('\r', ' '));
            return;
          }

        } else {
          LOGGER.debug("No Authentication Provided ");
        }
      }
      doGet(request, response);
      return;
    } catch (IllegalStateException ise) {
      LOGGER.error("doPOST: Response already committed, cannot login");
      return;
    }
  }


  /**
   * @return get the injected authenticator.
   */
  protected abstract Authenticator getAuthenticator();

  /**
   * Send a authentication failed message to the client
   * @param request
   * @param response
   */
  protected abstract void sendAuthenticationFailed(SlingHttpServletRequest request,
      SlingHttpServletResponse response);

  /**
   * Get the TokenWrapper used to inject a new trusted token into the request.
   * @param abstractAuthServlet
   * @return
   */
  protected abstract TrustedTokenServiceWrapper getTokenWrapper();


}
