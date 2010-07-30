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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
public interface ArtifactHandler {
  final String HANDLER_NAME = "auth.sso.name";

  /**
   * Get the name of the artifact that is looked for by this handler. This is to make sure
   * URLs are filtered properly in the authentication handler.
   */
  String getArtifactName();

  /**
   * Verify this handler can handle the request.
   *
   * @param request
   * @return
   */
  boolean canHandle(HttpServletRequest request);

  /**
   * Get the username associated to an artifact.
   *
   * @param request
   * @return
   */
  String getUsername(HttpServletRequest request);

  /**
   * Construct the URL to redirect the client to the login page.
   *
   * @param options
   * @return
   */
  String constructRedirectUrl(Map<String, Object> options);
}
