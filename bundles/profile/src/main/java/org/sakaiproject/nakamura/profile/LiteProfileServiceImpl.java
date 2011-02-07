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
package org.sakaiproject.nakamura.profile;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.profile.LiteProfileService;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
*
*/
@Component
@Service
public class LiteProfileServiceImpl implements LiteProfileService {


  // TODO BL120 this implementation needs to be finished

  public static final Logger LOG = LoggerFactory.getLogger(LiteProfileServiceImpl.class);

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getCompactProfileMap(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable, org.sakaiproject.nakamura.api.lite.Session)
   */
  public ValueMap getCompactProfileMap(Authorizable authorizable) {
    // The map were we will stick the compact information in.
    ValueMap compactProfile = new ValueMapDecorator(new HashMap<String, Object>());

    Map<String, Object> props = authorizable.getSafeProperties();
    for (Entry<String, Object> prop : props.entrySet()) {
      compactProfile.put(prop.getKey(), StorageClientUtils.toString(prop.getValue()));
    }

    if (authorizable instanceof Group) {
      compactProfile.put("groupid", authorizable.getId());
      compactProfile.put("sakai:group-id", authorizable.getId());
    } else if (authorizable instanceof User) {
      // TODO BL120 this is not ultimately how we'll build a profile
      ValueMap basic = new ValueMapDecorator(new HashMap<String, Object>());
      ValueMap elements = new ValueMapDecorator(new HashMap<String, Object>());
      ValueMap firstName = new ValueMapDecorator(new HashMap<String, Object>());
      ValueMap lastName = new ValueMapDecorator(new HashMap<String, Object>());
      ValueMap email = new ValueMapDecorator(new HashMap<String, Object>());
      firstName.put("value", authorizable.getId());
      lastName.put("value", authorizable.getId());
      email.put("value", "unknown@example.com");
      elements.put("firstName", firstName);
      elements.put("lastName", lastName);
      elements.put("email", email);
      basic.put("elements", elements);
      compactProfile.put("basic", basic);
      compactProfile.put("rep:userId", authorizable.getId());
      compactProfile.put("userid", authorizable.getId());
    }

    return compactProfile;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getCompactProfileMap(org.sakaiproject.nakamura.api.lite.content.Content)
   */
  public ValueMap getCompactProfileMap(Content profile) {
    Map<String, Object> profileProps;
    if (profile == null) {
      profileProps = new HashMap<String, Object>();
    } else {
      profileProps = profile.getProperties();
    }
    return new ValueMapDecorator(profileProps);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getHomePath(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable)
   */
  public String getHomePath(Authorizable authorizable) {
    return LitePersonalUtils.getHomePath(authorizable.getId());
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getPrivatePath(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable)
   */
  public String getPrivatePath(Authorizable authorizable) {
    return LitePersonalUtils.getPrivatePath(authorizable.getId());
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getProfileMap(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable, org.sakaiproject.nakamura.api.lite.Session)
   */
  public ValueMap getProfileMap(Authorizable authorizable, Session session) {
    // TODO BL120 get the full profile once that becomes available
    return getCompactProfileMap(authorizable);
//    try {
//      return getProfileMap(session.getContentManager().get(getProfilePath(authorizable)));
//    } catch (StorageClientException e) {
//      LOG.error("failed to get profile map for authorizable " + authorizable.getId());
//    } catch (AccessDeniedException e) {
//      LOG.error("failed to get profile map for authorizable " + authorizable.getId());
//    }
//    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getProfileMap(org.sakaiproject.nakamura.api.lite.content.Content)
   */
  public ValueMap getProfileMap(Content profile) {
    final Map<String, Object> profileProps;
    if (profile == null) {
      profileProps = new HashMap<String, Object>();
    } else {
      profileProps = profile.getProperties();
    }
    return new ValueMapDecorator(profileProps);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getProfilePath(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable)
   */
  public String getProfilePath(Authorizable authorizable) {
    return LitePersonalUtils.getProfilePath(authorizable.getId());
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getPublicPath(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable)
   */
  public String getPublicPath(Authorizable authorizable) {
    return LitePersonalUtils.getPublicPath(authorizable.getId());
  }

}
