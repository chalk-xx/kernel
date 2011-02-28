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
package org.sakaiproject.nakamura.api.user;

import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;

import java.util.Map;

/**
 * The Interface BasicUserInfoService is designed to provide basic user info so that the
 * profile service isn't hit so much.
 */
public interface BasicUserInfoService {

  /**
   * Generates a map of properties that represents the basic user.
   * 
   * @param authorizable
   *          the authorizable
   * @return the properties
   */
  public Map<String, Object> getProperties(Authorizable authorizable);

  /**
   * Update the user's properties.
   * 
   * @param authorizable
   *          the authorizable
   * @param properties
   *          the properties
   */
  public void updateProperties(Authorizable authorizable, Map<String, Object> properties);
}
