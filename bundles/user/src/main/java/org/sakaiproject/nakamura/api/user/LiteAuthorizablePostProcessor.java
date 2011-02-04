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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.Modification;

import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;

import java.util.Map;


/**
 *
 */
public interface LiteAuthorizablePostProcessor {

  /**
   * Method which will be called after a Sakai user or group has been created or modified,
   * and before the Sakai user or group is deleted.
   * @param request 
   *
   * @param authorizable
   * @param session
   * @param change describes what sort of change has occurred (or is about to occur)
   * @param parameters a map of non-persisted optional properties for whatever use
   *        the processing service sees fit
   * @throws Exception
   */
  void process(SlingHttpServletRequest request, Authorizable authorizable, Session session, Modification change, Map<String, Object[]> parameters)
      throws Exception;

}
