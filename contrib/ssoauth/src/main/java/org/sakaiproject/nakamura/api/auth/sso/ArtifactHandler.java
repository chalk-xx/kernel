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
 * Handler for identifying and extracting an artifact from an SSO server, as well as
 * extracting the credentials associated to an artifact. Object is also responsible for
 * providing URLs for the server, logging in and logging out.
 */
public interface ArtifactHandler {
  String HANDLER_NAME = "sakai.auth.sso.name";
  String DEFAULT_HANDLER = "sakai.auth.sso.handler.default";

  String LOGIN_URL = "sakai.auth.sso.url.login";
  String LOGOUT_URL = "sakai.auth.sso.url.logout";
  String SERVER_URL = "sakai.auth.sso.url.server";

  /**
   * Get the name of the artifact that is extracted by this handler. This is to make sure
   * URLs are filtered properly in the authentication handler.
   */
  String getArtifactName();

  /**
   * Extract the artifact from the request.
   *
   * @param request
   * @return null if artifact can not be extracted by this handler. non-null if extracted
   *         by this handler. A non-null response will mark this handler for use later in
   *         the authentication process.
   */
  String extractArtifact(HttpServletRequest request);

  /**
   * Extract the credentials (ie. username) from a request, artifact and/or response. This
   * is called after a positive response from a GET call to
   * {@link #getValidateUrl(String, HttpServletRequest)}.
   *
   * @param artifact
   * @param responseBody
   * @param request
   * @return The username associated to the artifact/responseBody/request. null if the
   *         username can not be extracted (response is negative, etc).
   */
  String extractCredentials(String artifact, String responseBody,
      HttpServletRequest request);

  /**
   * Get the URL the user should be directed to for logging in.
   *
   * @param returnUrl
   * @param request
   * @return
   */
  String getLoginUrl(String returnUrl, HttpServletRequest request);

  /**
   * Get the URL the user should be directed to for logging out.
   *
   * @param request
   * @return
   */
  String getLogoutUrl(HttpServletRequest request);

  /**
   * Get the URL to be used to get a validation response for an artifact.
   *
   * @param artifact
   * @param service
   * @param request
   * @return
   */
  String getValidateUrl(String artifact, String service, HttpServletRequest request);
}
