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
package org.sakaiproject.nakamura.user;

import static org.sakaiproject.nakamura.api.user.UserConstants.USER_EMAIL_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_FIRSTNAME_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_IDENTIFIER_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_LASTNAME_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_PICTURE;

import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class BasicUserInfo provides basic user information to alleviate load from the
 * ProfileService.
 */
public class BasicUserInfo {

  public static final Logger LOG = LoggerFactory.getLogger(BasicUserInfo.class);

  private final String[] basicUserInfoElements = new String[] { USER_IDENTIFIER_PROPERTY, USER_FIRSTNAME_PROPERTY, USER_LASTNAME_PROPERTY, USER_EMAIL_PROPERTY, USER_PICTURE };

  public BasicUserInfo() {
  }

  /**
   * Generates a map of basic user properties.
   * 
   * @param authorizable
   *          the authorizable
   * @return the basic user properties
   */
  public Map<String, Object> getProperties(Authorizable authorizable) {
    Map<String, Object> basicInfoMap = new HashMap<String, Object>();
    for (String basicInfoElementName : basicUserInfoElements) {
      if (authorizable.hasProperty(basicInfoElementName)) {
        basicInfoMap.put(basicInfoElementName,
            authorizable.getProperty(basicInfoElementName));
      }
    }
    return basicInfoMap;
  }

  /**
   * Update basic user properties.
   * 
   * @param authorizable
   *          the authorizable
   * @param properties
   *          the properties to update
   */
  public void updateProperties(Authorizable authorizable, Map<String, Object> properties) {
    for (String basicInfoElementName : basicUserInfoElements) {
      if (authorizable.hasProperty(basicInfoElementName)) {
        Object obj = properties.get(basicInfoElementName);
        if (obj != null)
          authorizable.setProperty(basicInfoElementName, obj);
      }
    }
  }
}
