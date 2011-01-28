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
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.profile.LiteProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
*
*/
@Component(immediate = true, specVersion = "1.1")
@Service(value = LiteProfileService.class)
public class LiteProfileServiceImpl implements LiteProfileService {
  

  // TODO BL120 this implementation needs to be finished
  
  public static final Logger LOG = LoggerFactory.getLogger(LiteProfileServiceImpl.class);

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getCompactProfileMap(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable, org.sakaiproject.nakamura.api.lite.Session)
   */
  public ValueMap getCompactProfileMap(Authorizable authorizable, Session session) {
    final Map<String, Object> profileProps = new HashMap<String, Object>();
    profileProps.putAll(authorizable.getPropertiesForUpdate());
    
    if (authorizable instanceof Group) {
      profileProps.put("groupid", authorizable.getId());
    } else if (authorizable instanceof User) {
      profileProps.put("userid", authorizable.getId());
    }
    return new ValueMap(){

      public <T> T get(String name, Class<T> type) {
        // TODO Auto-generated method stub
        return null;
      }

      public <T> T get(String name, T defaultValue) {
        // TODO Auto-generated method stub
        return null;
      }

      public void clear() {
        // TODO Auto-generated method stub
        
      }

      public boolean containsKey(Object arg0) {
        // TODO Auto-generated method stub
        return false;
      }

      public boolean containsValue(Object arg0) {
        return profileProps.containsValue(arg0);
      }

      public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return profileProps.entrySet();
      }

      public Object get(Object arg0) {
        return profileProps.get((String)arg0);
      }

      public boolean isEmpty() {
        return profileProps.isEmpty();
      }

      public Set<String> keySet() {
        return profileProps.keySet();
      }

      public Object put(String arg0, Object arg1) {
        return profileProps.put(arg0, arg0);
      }

      public void putAll(Map<? extends String, ? extends Object> arg0) {
        profileProps.putAll(arg0);
      }

      public Object remove(Object arg0) {
        return profileProps.remove(arg0);
      }

      public int size() {
        return profileProps.size();
      }

      public Collection<Object> values() {
        return profileProps.values();
      }};
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getCompactProfileMap(org.sakaiproject.nakamura.api.lite.content.Content)
   */
  public ValueMap getCompactProfileMap(Content profile) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getHomePath(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable)
   */
  public String getHomePath(Authorizable authorizable) {
    return "~/" + authorizable.getId();
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getPrivatePath(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable)
   */
  public String getPrivatePath(Authorizable authorizable) {
    return getHomePath(authorizable) + "/private";
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getProfileMap(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable, org.sakaiproject.nakamura.api.lite.Session)
   */
  public ValueMap getProfileMap(Authorizable authorizable, Session session) {
    try {
      return getProfileMap(session.getContentManager().get(getProfilePath(authorizable)));
    } catch (StorageClientException e) {
      LOG.error("failed to get profile map for authorizable " + authorizable.getId());
    } catch (AccessDeniedException e) {
      LOG.error("failed to get profile map for authorizable " + authorizable.getId());
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getProfileMap(org.sakaiproject.nakamura.api.lite.content.Content)
   */
  public ValueMap getProfileMap(Content profile) {
    final Map<String, Object> profileProps = profile.getProperties();
    return new ValueMap(){

      public <T> T get(String name, Class<T> type) {
        // TODO Auto-generated method stub
        return null;
      }

      public <T> T get(String name, T defaultValue) {
        // TODO Auto-generated method stub
        return null;
      }

      public void clear() {
        // TODO Auto-generated method stub
        
      }

      public boolean containsKey(Object arg0) {
        // TODO Auto-generated method stub
        return false;
      }

      public boolean containsValue(Object arg0) {
        return profileProps.containsValue(arg0);
      }

      public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return profileProps.entrySet();
      }

      public Object get(Object arg0) {
        return profileProps.get((String)arg0);
      }

      public boolean isEmpty() {
        return profileProps.isEmpty();
      }

      public Set<String> keySet() {
        return profileProps.keySet();
      }

      public Object put(String arg0, Object arg1) {
        return profileProps.put(arg0, arg0);
      }

      public void putAll(Map<? extends String, ? extends Object> arg0) {
        profileProps.putAll(arg0);
      }

      public Object remove(Object arg0) {
        return profileProps.remove(arg0);
      }

      public int size() {
        return profileProps.size();
      }

      public Collection<Object> values() {
        return profileProps.values();
      }};
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
   * @see org.sakaiproject.nakamura.api.profile.LiteProfileService#getPublicPath(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable)
   */
  public String getPublicPath(Authorizable authorizable) {
    return getHomePath(authorizable) + "/public";
  }

}
