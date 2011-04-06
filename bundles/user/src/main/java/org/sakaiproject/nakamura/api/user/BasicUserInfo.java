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

import com.google.common.collect.ImmutableMap;

import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_DESCRIPTION_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_TITLE_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.PREFERRED_NAME;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_COLLEGE;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_DATEOFBIRTH;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_DEPARTMENT;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_EMAIL_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_FIRSTNAME_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_LASTNAME_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_PICTURE;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_ROLE;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_BASIC;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * The Class BasicUserInfo provides basic user information to alleviate load from the
 * ProfileService.
 */
public class BasicUserInfo {

  public static final Logger LOG = LoggerFactory.getLogger(BasicUserInfo.class);

  private final String[] basicUserInfoElements = new String[] { USER_FIRSTNAME_PROPERTY,
      USER_LASTNAME_PROPERTY, USER_EMAIL_PROPERTY, USER_PICTURE, PREFERRED_NAME,
      USER_ROLE, USER_DEPARTMENT, USER_COLLEGE, USER_DATEOFBIRTH };

  public BasicUserInfo() {
  }
  
  public Map<String, Object> getProperties(org.apache.jackrabbit.api.security.user.Authorizable authorizable, javax.jcr.Session jcrSession) throws RepositoryException {
    try {
      Session sparseSession = StorageClientUtils.adaptToSession(jcrSession);
      return getProperties(sparseSession.getAuthorizableManager().findAuthorizable(authorizable.getID()));
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new RepositoryException(e.getMessage(), e);
    }
  }

  /**
   * Generates a map of basic user properties.
   * 
   * @param authorizable
   *          the authorizable
   * @return the basic user properties
   */
  public Map<String, Object> getProperties(Authorizable authorizable) {
    if (authorizable == null || User.ANON_USER.equals(authorizable.getId())) {
      return anonymousUser();
    }

    Map<String, Object> basicInfoMap = new HashMap<String, Object>();

    Map<String, String> basicInfoProps = new HashMap<String, String>();
    for (String basicInfoElementName : basicUserInfoElements) {
      /* --- DEBUG --- */
      LOG.debug("Looking for key: [" + basicInfoElementName + "]");
      /* --- DEBUG --- */

      if (authorizable.hasProperty(basicInfoElementName)) {
        basicInfoProps.put(basicInfoElementName,
            String.valueOf(authorizable.getProperty(basicInfoElementName)));
        LOG.debug("Found [" + basicInfoElementName + "], value: ["
            + authorizable.getProperty(basicInfoElementName) + "]");
      }
    }
    ValueMap basicProfile = basicProfile(basicInfoProps);

    if (authorizable.hasProperty("access")) {
      basicProfile.put("access", authorizable.getProperty("access"));
    } else {
      basicProfile.put(UserConstants.USER_BASIC_ACCESS,
          UserConstants.EVERYBODY_ACCESS_VALUE);
    }

    basicInfoMap.put(USER_BASIC, basicProfile);

    if (authorizable.isGroup()) {
      addGroupProperties(authorizable, basicInfoMap);
    } else {
      addUserProperties(authorizable, basicInfoMap);
    }
    return basicInfoMap;
  }

  /**
   * @param elementsMap
   * @return
   */
  private ValueMap basicProfile(Map<String, String> elementsMap) {
    ValueMap basic = new ValueMapDecorator(new HashMap<String, Object>());
    ValueMap elements = new ValueMapDecorator(new HashMap<String, Object>());
    for (String key : elementsMap.keySet()) {
      elements.put(key,
          new ValueMapDecorator(ImmutableMap.of("value", (Object) elementsMap.get(key))));
    }
    basic.put("elements", elements);
    return basic;
  }

  /**
   * Anonymous user.
   * 
   * @return the map
   */
  private Map<String, Object> anonymousUser() {
    Map<String, Object> rv = new HashMap<String, Object>();
    rv.put("rep:userId", User.ANON_USER);
    rv.put("userid", User.ANON_USER);
    rv.put(USER_FIRSTNAME_PROPERTY, "Anonymous");
    rv.put(USER_LASTNAME_PROPERTY, "User");
    rv.put(USER_EMAIL_PROPERTY, "anon@sakai.invalid");
    rv.put(UserConstants.USER_BASIC_ACCESS, UserConstants.EVERYBODY_ACCESS_VALUE);
    return rv;
  }

  /**
   * Adds the user properties.
   * 
   * @param user
   *          the user
   * @param propertyMap
   *          the property map
   */
  private void addUserProperties(Authorizable user, Map<String, Object> propertyMap) {
    // Backward compatible reasons.
    propertyMap.put("rep:userId", user.getId());
    propertyMap.put("userid", user.getId());
    propertyMap.put("hash", user.getId());
  }

  /**
   * Adds the group properties.
   * 
   * @param group
   *          the group
   * @param propertyMap
   *          the profile map
   */
  private void addGroupProperties(Authorizable group, Map<String, Object> propertyMap) {
    // For a group we just dump it's title and description.
    propertyMap.put("groupid", group.getId());
    propertyMap.put("sakai:group-id", group.getId());
    propertyMap.put(GROUP_TITLE_PROPERTY, group.getProperty(GROUP_TITLE_PROPERTY));
    propertyMap.put(GROUP_DESCRIPTION_PROPERTY,
        group.getProperty(GROUP_DESCRIPTION_PROPERTY));
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
