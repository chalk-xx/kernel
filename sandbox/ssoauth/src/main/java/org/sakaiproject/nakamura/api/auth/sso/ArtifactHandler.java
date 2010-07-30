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
package org.sakaiproject.nakamura.api.auth.sso;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
public interface ArtifactHandler {
  String HANDLER_NAME = "auth.sso.name";

  String LOGIN_URL = "auth.sso.url.login";
  String LOGOUT_URL = "auth.sso.url.logout";
  String SERVER_URL = "auth.sso.url.server";

  /**
   * Get the name of the artifact that is looked for by this handler. This is to make sure
   * URLs are filtered properly in the authentication handler.
   */
  String getArtifactName();
  String getArtifact(HttpServletRequest request);

  /**
   * Extract the credentials (ie. username) from a request. This is called after a
   * positive response from {@link #isValid(String)}.
   *
   * @param artifact
   * @param responseBody
   * @param request
   * @return
   */
  String extractCredentials(String artifact, String responseBody,
      HttpServletRequest request);

  /**
   * Get the URL the user should be directed to for logging in.
   *
   * @param reqeust
   * @return
   */
  String getLoginUrl(String returnUrl, HttpServletRequest reqeust);

  /**
   *
   * @param request
   * @return
   */
  String getLogoutUrl(HttpServletRequest request);

  /**
   * Decorate the URL used to validate an artifact.
   *
   * @param request
   * @return
   */
  String getValidateUrl(String artifact, HttpServletRequest request);
}
