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

import org.apache.sling.engine.auth.AuthenticationHandler;
import org.apache.sling.engine.auth.AuthenticationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 
 * 
 * @scr.component immediate="false" label="%auth.http.name"
 *                description="%auth.http.description"
 * @scr.property name="service.description" value="Form Authentication Handler"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property nameRef="AuthenticationHandler.PATH_PROPERTY" value="/system/sling/formlogin"
 * @scr.service interface="org.apache.sling.engine.auth.AuthenticationHandler"
 */

public final class FormAuthenticationHandler implements AuthenticationHandler {

  public static final String FORCE_LOGOUT = "sakai_logout";
  public static final String TRY_LOGIN = "sakai_login";
  public static final String USERNAME = "un";
  public static final String PASSWORD = "pw";

  /**
   * 
   */
  final class FormAuthentication {

    /**
     *
     */
    private static final long serialVersionUID = 8019850707623762839L;
    private boolean forceLogout;
    private boolean valid;
    private SimpleCredentials credentials;

    /**
     * @param request
     */
    FormAuthentication(HttpServletRequest request) {
      forceLogout = false;
      valid = false;
      LOGGER.info("Checking Request");
      if ("POST".equals(request.getMethod())) {
        LOGGER.info("is a POST");
        if ("1".equals(request.getParameter(FORCE_LOGOUT))) {
          LOGGER.info(" logout");
          valid = false;
          forceLogout = true;
        } else if ("1".equals(request.getParameter(TRY_LOGIN))) {
          LOGGER.info(" login");
          String password = request.getParameter(PASSWORD);
          if (password == null) {
            credentials = new SimpleCredentials(request.getParameter(USERNAME),
                new char[0]);
          } else {
            credentials = new SimpleCredentials(request.getParameter(USERNAME), password
                .toCharArray());
          }
          valid = true;
        }
      }
    }

    /**
     * @return
     */
    boolean isValid() {
      return valid;
    }

    /**
     * @return
     */
    boolean isForceLogout() {
      return forceLogout;
    }

    /**
     * @return
     */
    Credentials getCredentials() {
      IllegalAccessError e = new IllegalAccessError("getCredentials() has been invoked from an invalid location ");
      StackTraceElement[] ste = e.getStackTrace();
      if ( FormAuthenticationHandler.class.getName().equals(ste[1].getClassName()) &&
          "authenticate".equals(ste[1].getMethodName()) ) {
        return credentials;
      }
      throw e;
    }
    
  }

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FormAuthenticationHandler.class);
  private static final String USER_CREDENTIALS = FormAuthentication.class.getName();
  public static final String SESSION_AUTH = FormAuthenticationHandler.class.getName();

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.engine.auth.AuthenticationHandler#authenticate(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public final AuthenticationInfo authenticate(HttpServletRequest request,
      HttpServletResponse response) {
    LOGGER.info("Traceback",new Exception());
    HttpSession session = request.getSession(false);

    // no session authentication info, try the request

    FormAuthentication authentication = new FormAuthentication(request);
    if (authentication.isValid()) {
      // authenticate
      session = request.getSession(true);
      LOGGER
          .warn("Saving a populated credentials object to session, this should not be "
              + "allowed to go into production as it may result in the password being stored");
      session.setAttribute(USER_CREDENTIALS, authentication);
      return new AuthenticationInfo(SESSION_AUTH, authentication.getCredentials());
    }
    if (authentication.isForceLogout()) {
      // force logout
      if (session != null) {
        session.removeAttribute(USER_CREDENTIALS);
      }
      // no auth info
      return null;
    }

    if (session != null) {
      FormAuthentication savedCredentials = (FormAuthentication) session
          .getAttribute(USER_CREDENTIALS);
      if (savedCredentials != null && savedCredentials.isValid()) {
        return new AuthenticationInfo(SESSION_AUTH, authentication.getCredentials());
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.engine.auth.AuthenticationHandler#requestAuthentication(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public boolean requestAuthentication(HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    return true;
  }

}
