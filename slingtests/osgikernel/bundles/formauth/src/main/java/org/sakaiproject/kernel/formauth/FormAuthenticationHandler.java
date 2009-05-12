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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
 * @scr.property nameRef="AuthenticationHandler.PATH_PROPERTY" value="/"
 * @scr.service
 */

public class FormAuthenticationHandler implements AuthenticationHandler {

  public static final String FORCE_LOGOUT = "sakai:logout";
  public static final String TRY_LOGIN = "sakai:login";
  public static final String USERNAME = "un";
  public static final String PASSWORD = "pw";

  /**
   * 
   */
  class FormAuthentication {

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
      if ("POST".equals(request.getMethod())) {
        if ("1".equals(request.getParameter(FORCE_LOGOUT))) {
          valid = false;
          forceLogout = true;
        } else if ("1".equals(request.getParameter(TRY_LOGIN))) {
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
          "extractAuthentication".equals(ste[1].getMethodName()) ) {
        return credentials;
      }
      throw e;
    }
    
  }

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FormAuthenticationHandler.class);
  private static final String USER_CREDENTIALS = FormAuthentication.class.getName();
  public static final String SESSION_AUTH = FormAuthenticationHandler.class.getName();
  private static final String LOGIN_FORM_TEMPLATE = "LoginFormTemplate.html";
  private String loginFormTemplate;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.engine.auth.AuthenticationHandler#authenticate(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public AuthenticationInfo authenticate(HttpServletRequest request,
      HttpServletResponse response) {
    // extract credentials and return
    AuthenticationInfo info = this.extractAuthentication(request);
    if (info != null) {
      return info;
    }

    // no special header, so we will not authenticate here
    return null;
  }

  /**
   * @param request
   * @return
   */
  private AuthenticationInfo extractAuthentication(HttpServletRequest request) {
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

    // if the response is already committed, we have a problem !!
    if (!response.isCommitted()) {

      // reset the response
      response.reset();
      response.setStatus(HttpServletResponse.SC_OK);

      String form = getLoginForm();

      if (form != null) {

        form = replaceVariables(form, "@@contextPath@@", request.getContextPath(), "/");
        form = replaceVariables(form, "@@authType@@", request.getAuthType(), "");
        form = replaceVariables(form, "@@user@@", request.getRemoteUser(), "");

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(form);

      } else {

        // have no form, so just send 401/UNATHORIZED for simple login
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

      }

    } else {

      LOGGER
          .error("requestAuthentication: Response is committed, cannot request authentication");

    }

    return true;
  }

  /**
   * Returns the login form template as a string or <code>null</code> if it cannot be
   * read. Failure to read the template is logged.
   */
  private String getLoginForm() {
    if (loginFormTemplate == null) {
      InputStream ins = getClass().getResourceAsStream(LOGIN_FORM_TEMPLATE);
      if (ins != null) {
        try {

          ByteArrayOutputStream out = new ByteArrayOutputStream();
          byte[] buf = new byte[3000];
          int bytes = 0;
          while ((bytes = ins.read(buf)) >= 0) {
            out.write(buf, 0, bytes);
          }
          out.close();
          loginFormTemplate = new String(out.toByteArray(), "UTF-8");

        } catch (IOException ioe) {

          LOGGER.error("getLoginForm: Failure reading login form template", ioe);

        } finally {

          try {
            ins.close();
          } catch (IOException ignore) {
          }

        }

      } else {

        LOGGER.error("getLoginForm: Cannot access login form template at "
            + LOGIN_FORM_TEMPLATE);

      }
    }

    return loginFormTemplate;
  }

  /**
   * Replaces all occurrences in the <code>template</code> of the <code>key</code> (a
   * regular expression) by the <code>value</code> or <code>defaultValue</code>.
   * 
   * @param template
   *          The template to replace occurrences of key
   * @param key
   *          The regular expression of the key to replace
   * @param value
   *          The replacement value
   * @param defaultValue
   *          The replacement value to use if the value is null or an empty string.
   * @return the template with the key values replaced.
   */
  private String replaceVariables(String template, String key, String value,
      String defaultValue) {
    if (value == null || value.length() == 0) {
      value = defaultValue;
    }
    return template.replaceAll(key, value);
  }

}
