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
package org.sakaiproject.nakamura.message.internal;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.sling.api.resource.ValueMap;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.profile.LiteProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


// TODO BL120 This is just a temporary class to satisfy a dependency while we wait for a real port of the ProfileService to sparse
@Component(immediate = true, label = "LiteProfileServiceImpl", description = "Handler for internally delivered messages.")
@Services(value = { @Service(value = LiteProfileService.class) })
public class TempLiteProfileServiceImpl implements LiteProfileService {
  
  private static Logger LOG = LoggerFactory.getLogger(TempLiteProfileServiceImpl.class);

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getHomePath(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable)
   */
  public String getHomePath(Authorizable authorizable) {
    String id = authorizable.getId();
    return MessageConstants.SAKAI_MESSAGE_PATH_PREFIX + id;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getPublicPath(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable)
   */
  public String getPublicPath(Authorizable authorizable) {
    String id = authorizable.getId();
    return MessageConstants.SAKAI_MESSAGE_PATH_PREFIX + id + "/public";
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getPrivatePath(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable)
   */
  public String getPrivatePath(Authorizable authorizable) {
    String id = authorizable.getId();
    return MessageConstants.SAKAI_MESSAGE_PATH_PREFIX + id + "/private";
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getProfilePath(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable)
   */
  public String getProfilePath(Authorizable authorizable) {
    return getPublicPath(authorizable) + "/profile";
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getProfileMap(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable, org.sakaiproject.nakamura.api.lite.Session)
   */
  public ValueMap getProfileMap(Authorizable authorizable, Session session) {
    Content profileContent = null;
    try {
      profileContent = session.getContentManager().get(getProfilePath(authorizable));
    } catch (StorageClientException e) {
      LOG.error(e.getLocalizedMessage());
    } catch (AccessDeniedException e) {
      LOG.error(e.getLocalizedMessage());
    }
    return getProfileMap(profileContent);
    
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getProfileMap(org.sakaiproject.nakamura.api.lite.content.Content)
   */
  public ValueMap getProfileMap(Content profile) {
    return valueMapForContent(profile);
  }

  private ValueMap valueMapForContent(Content profile) {
    final Content profileContent = profile;
    return new ValueMap(){

      public void clear() {
        // TODO Auto-generated method stub
        
      }

      public boolean containsKey(Object key) {
        return profileContent.hasProperty(key.toString());
      }

      public boolean containsValue(Object value) {
        // TODO Auto-generated method stub
        return false;
      }

      public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return profileContent.getProperties().entrySet();
      }

      public Object get(Object key) {
        return profileContent.getProperty(key.toString());
      }

      public boolean isEmpty() {
        return profileContent.getProperties().isEmpty();
      }

      public Set<String> keySet() {
        return profileContent.getProperties().keySet();
      }

      public Object put(String key, Object value) {
        profileContent.setProperty(key, StorageClientUtils.toStore(value));
        return value;
      }

      public void putAll(Map<? extends String, ? extends Object> m) {
        profileContent.getProperties().putAll(m);
      }

      public Object remove(Object key) {
        return profileContent.getProperties().remove(key);
      }

      public int size() {
        return profileContent.getProperties().size();
      }

      public Collection<Object> values() {
        return profileContent.getProperties().values();
      }

      public <T> T get(String name, Class<T> type) {
        // TODO Auto-generated method stub
        return null;
      }

      @SuppressWarnings("unchecked")
      public <T> T get(String name, T defaultValue) {
        if (profileContent.hasProperty(name)) {
          return (T) profileContent.getProperty(name);
        } else {
          return defaultValue;
        }
      }};
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getCompactProfileMap(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable, org.sakaiproject.nakamura.api.lite.Session)
   */
  public ValueMap getCompactProfileMap(Authorizable authorizable, Session session) {
    return getProfileMap(authorizable, session);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getCompactProfileMap(org.sakaiproject.nakamura.api.lite.content.Content)
   */
  public ValueMap getCompactProfileMap(Content profile) {
    return getProfileMap(profile);
  }

}
