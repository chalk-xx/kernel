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

import com.google.common.collect.ImmutableMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
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
  
  @Reference
  protected Repository sparseRepository;


  // TODO BL120 this implementation needs to be finished

  public static final Logger LOG = LoggerFactory.getLogger(LiteProfileServiceImpl.class);

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getCompactProfileMap(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable, org.sakaiproject.nakamura.api.lite.Session)
   */
  public ValueMap getCompactProfileMap(Authorizable authorizable) {
    ValueMap compactProfile = new ValueMapDecorator(new HashMap<String, Object>());
    Session session = null;
    try {
      session = sparseRepository.loginAdministrative();
      // The map were we will stick the compact information in.
      ValueMap fullProfile = getProfileMap(authorizable, session);

      Map<String, Object> props = authorizable.getSafeProperties();
      for (Entry<String, Object> prop : props.entrySet()) {
        compactProfile.put(prop.getKey(), prop.getValue());
      }

      if (authorizable instanceof Group) {
        compactProfile.put("groupid", authorizable.getId());
        compactProfile.put("sakai:group-id", authorizable.getId());
      } else if (authorizable instanceof User) {
        compactProfile.put("rep:userId", authorizable.getId());
        compactProfile.put("userid", authorizable.getId());
      }
      

      if (fullProfile.containsKey("basic")) {
        compactProfile.put("basic", fullProfile.get("basic"));
      }

      
    } catch (ClientPoolException e) {
      LOG.error(e.getLocalizedMessage(), e);
    } catch (StorageClientException e) {
      LOG.error(e.getLocalizedMessage(), e);
    } catch (AccessDeniedException e) {
      LOG.error(e.getLocalizedMessage(), e);
    } finally {
      if (session != null) {
        try {
          session.logout();
        } catch (ClientPoolException e) {
          LOG.error(e.getLocalizedMessage(), e);
        }
      }
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
    if (User.ANON_USER.equals(authorizable.getId())) {
      return anonymousProfile();
    }
    ValueMap profileMap = null;
    try {
      Content profileNode = session.getContentManager().get(getProfilePath(authorizable));
      if (profileNode != null) {
        profileMap = getProfileMap(profileNode);
        if (authorizable instanceof Group) {
          profileMap.put("groupid", authorizable.getId());
          profileMap.put("sakai:group-id", authorizable.getId());
        } else if (authorizable instanceof User) {
          profileMap.put("rep:userId", authorizable.getId());
          profileMap.put("userid", authorizable.getId());
        }
      }
    } catch (StorageClientException e) {
      LOG.error(e.getLocalizedMessage(), e);
    } catch (AccessDeniedException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
    if (profileMap == null) {
      profileMap = new ValueMapDecorator(new HashMap<String, Object>());
    }
    // we'll tack on all the authorizable's properties
    profileMap.putAll(authorizable.getSafeProperties());
    return profileMap;
  }

  private ValueMap anonymousProfile() {
//    {
//      "sakai:search-exclude-tree": true,
//      "firstName": "Anonymous",
//      "rep:userId": "anonymous",
//      "sling:resourceType": "sakai/user-profile",
//      "jcr:uuid": "e7fc5988-1269-4c7b-bdc8-8248ad633f97",
//      "jcr:mixinTypes": ["mix:referenceable"],
//      "email": "anon@sakai.invalid",
//      "path": "/a/an/anonymous",
//      "lastName": "User",
//      "jcr:primaryType": "nt:unstructured"
//  }
    ValueMap rv = new ValueMapDecorator(new HashMap<String, Object>());
    rv.put("rep:userId", "anonymous");
    ValueMap basic = new ValueMapDecorator(new HashMap<String, Object>());
    ValueMap elements = new ValueMapDecorator(new HashMap<String, Object>());
    ValueMap firstName = new ValueMapDecorator(ImmutableMap.of("value", (Object)"Anonymous"));
    ValueMap lastName = new ValueMapDecorator(ImmutableMap.of("value", (Object)"User"));
    ValueMap email = new ValueMapDecorator(ImmutableMap.of("value", (Object)"anon@sakai.invalid"));
    elements.put("firstName", firstName);
    elements.put("lastName", lastName);
    elements.put("email", email);
    basic.put("elements", elements);
    rv.put("basic", basic);
    return rv;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getProfileMap(org.sakaiproject.nakamura.api.lite.content.Content)
   */
  public ValueMap getProfileMap(Content profile) {
    return getContentMap(profile);
  }

  private ValueMap getContentMap(Content content) {
    ValueMap rv = new ValueMapDecorator(new HashMap<String, Object>());
    for (String propName : content.getProperties().keySet()) {
      ValueMap value = new ValueMapDecorator(new HashMap<String, Object>());
      value.put("value", content.getProperty(propName));
      rv.put(propName, value);
    }
    
    for (Content childContent : content.listChildren()) {
      rv.put(childContent.getPath().substring(childContent.getPath().lastIndexOf("/") + 1), getContentMap(childContent));
    }
    return rv;
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
