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

public interface SsoAuthConstants {
  /**
   * Identification of this authentication handler. This value is set by the
   * handler as the authentication type of the <code>AuthenticationInfo</code>
   * object returned from the <code>extractCredentials</code> method.
   * <p>
   * To explicitly request SSO authentication handling, this should be used
   * as the value of the <code>sling:authRequestLogin</code> request
   * parameter.
   */
  String SSO_AUTH_TYPE = "SSO";

  /**
   * The login path used to reach this authentication handler.
   */
  String SSO_LOGIN_PATH = "/system/sling/sso/login";
}
