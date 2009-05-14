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
 * <p>
 * This is the Form Based Login Authenticator, its mounted at / and is invoked via the
 * OSGi HttpService handleSecurity call on the context. It is also invoked explicitly via
 * the FormLoginServlet via that authenticator service.
 * </p>
 * <p>
 * When the request is invoked, if the method is a POST, the authentication mechanism will
 * be invoked. If the method is not a post an attempt to get credentials out of the
 * session will be made, if they are found, then the session stored credentials will be
 * used to authenticate the request against the JCR.
 * </p>
 * <p>
 * On POST, if sakaiauth:login = 1, authentication will take place. If
 * sakaiauth:logout = 1, then logout will take place. If sakaiauth:login = 1
 * then the request parameter "sakaiauth:un" will be used for the username and
 * "sakaiauth:pw" for the password.
 * </p>
 * <p>
 * Login should be attempted at /system/sling/formlogin where there is a servlet mounted
 * to handle the request, performing this operation anywhere else will result in a
 * resource creation, as POST creates a resource in the default SLing POST mechanism.
 * </p>
 * <p>
 * See {@link FormLoginServlet} to see a description of POST and GET to that servlet.
 * </p>
 * 
 * @scr.component immediate="false" label="%auth.http.name"
 *                description="%auth.http.description"
 * @scr.property name="service.description" value="Form Authentication Handler"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property nameRef="AuthenticationHandler.PATH_PROPERTY" value="/"
 * @scr.service interface="org.apache.sling.engine.auth.AuthenticationHandler"
 */

public final class FormAuthenticationHandler implements AuthenticationHandler {

  public static final String FORCE_LOGOUT = "sakaiauth:logout";
  public static final String TRY_LOGIN = "sakaiauth:login";
  public static final String USERNAME = "sakaiauth:un";
  public static final String PASSWORD = "sakaiauth:pw";

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
          LOGGER.info(" login as "+request.getParameter(USERNAME));
          String password = request.getParameter(PASSWORD);
          if (password == null) {
            credentials = new SimpleCredentials(request.getParameter(USERNAME),
                new char[0]);
          } else {
            credentials = new SimpleCredentials(request.getParameter(USERNAME), password
                .toCharArray());
          }
          valid = true;
        } else {
          LOGGER.info("Login was not requested ");
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
      IllegalAccessError e = new IllegalAccessError(
          "getCredentials() has been invoked from an invalid location ");
      StackTraceElement[] ste = e.getStackTrace();
      if (FormAuthenticationHandler.class.getName().equals(ste[1].getClassName())
          && "authenticate".equals(ste[1].getMethodName())) {
        return credentials;
      }
      throw e;
    }

    /**
     * @return
     */
    public String getUserId() {
      return credentials.getUserID();
    }

  }

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FormAuthenticationHandler.class);
  static final String USER_CREDENTIALS = FormAuthentication.class.getName();
  public static final String SESSION_AUTH = FormAuthenticationHandler.class.getName();

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.engine.auth.AuthenticationHandler#authenticate(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public final AuthenticationInfo authenticate(HttpServletRequest request,
      HttpServletResponse response) {

    // no session authentication info, try the request

    FormAuthentication authentication = new FormAuthentication(request);
    if (authentication.isValid()) {
      // authenticate
      request.setAttribute(USER_CREDENTIALS, authentication);
      return new AuthenticationInfo(SESSION_AUTH, authentication.getCredentials());
    }
    
    HttpSession session = request.getSession(false);
    if (authentication.isForceLogout()) {
      // force logout
      if (session != null) {
        session.removeAttribute(USER_CREDENTIALS);
      }
      // no auth info
      return null;
    }

    if (session != null) {
      LOGGER.info("SessionAuth: Has session {} ",session.getId());
      FormAuthentication savedCredentials = (FormAuthentication) session
          .getAttribute(USER_CREDENTIALS);
      LOGGER.info("SessionAuth: Has session {} with credentials {} ",session.getId(),savedCredentials);
      if (savedCredentials != null && savedCredentials.isValid()) {
        LOGGER.info("SessionAuth: User ID {} ",savedCredentials.getUserId());
        return new AuthenticationInfo(SESSION_AUTH, savedCredentials.getCredentials());
      } else {
        LOGGER.info("SessionAuth: Saved credentials are not valid ");
      }
    } else {
      LOGGER.info("SessionAuth: No Session");
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
