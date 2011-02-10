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

import static org.sakaiproject.nakamura.api.profile.ProfileConstants.GROUP_DESCRIPTION_PROPERTY;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.GROUP_TITLE_PROPERTY;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.USER_BASIC;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.USER_PICTURE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.profile.ProfileProvider;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.profile.ProviderSettings;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
@Component(immediate = true, specVersion = "1.1")
@Service(value = ProfileService.class)
@References(value = { @Reference(name = "ProfileProviders", referenceInterface = ProfileProvider.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, strategy = ReferenceStrategy.EVENT, bind = "bindProfileProvider", unbind = "unbindProfileProvider") })
public class ProfileServiceImpl implements ProfileService {

  private Map<String, ProfileProvider> providers = new ConcurrentHashMap<String, ProfileProvider>();
  private ProviderSettingsFactory providerSettingsFactory = new ProviderSettingsFactory();
  public static final Logger LOG = LoggerFactory.getLogger(ProfileServiceImpl.class);



  /**
   * {@inheritDoc}
   * @throws StorageClientException 
   * @throws AccessDeniedException 
   *
   * @see org.sakaiproject.nakamura.api.profile.ProfileService#getProfileMap(org.apache.jackrabbit.api.security.user.Authorizable,
   *      javax.jcr.Session)
   */
  public ValueMap getProfileMap(Authorizable authorizable, Session session)
      throws RepositoryException, StorageClientException, AccessDeniedException {
    String profilePath = LitePersonalUtils.getProfilePath(authorizable.getId());
    org.sakaiproject.nakamura.api.lite.Session sparseSession = StorageClientUtils.adaptToSession(session);
    ContentManager contentManager = sparseSession.getContentManager();
    ValueMap profileMap;
    if (contentManager.exists(profilePath)) {
      Content profileContent = contentManager.get(profilePath);
      profileMap = getProfileMap(profileContent, session);
    } else {
      profileMap = null;
    }
    return profileMap;
  }

  /**
   * {@inheritDoc}
   * @param jcrSession 
   *
   * @see org.sakaiproject.nakamura.api.profile.ProfileService#getProfileMap(javax.jcr.Node)
   */
  public ValueMap getProfileMap(Content profileContent, Session jcrSession) throws RepositoryException {
    // Get the data from our external providers.
    Map<String, List<ProviderSettings>> providersMap = scanForProviders(profileContent, jcrSession);
    Map<Node, Future<Map<String, Object>>> providedNodeData = new HashMap<Node, Future<Map<String, Object>>>();
    for (Entry<String, List<ProviderSettings>> e : providersMap.entrySet()) {
      ProfileProvider pp = providers.get(e.getKey());
      if (pp != null) {
        providedNodeData.putAll(pp.getProvidedMap(e.getValue()));
      }
    }
    try {
      // Return it as a ValueMap.
      ValueMap map = new ValueMapDecorator(new HashMap<String, Object>());
      handleNode(profileContent, providedNodeData, map);
      return map;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Fills the provided map with the JCR info and the external information.
   *
   * @param profileContent
   *          The node that should be merged with the external info. The entire nodetree
   *          will be checked.
   * @param baseMap
   *          The map that contains the external information.
   * @param map
   *          The map that should be filled.
   * @throws RepositoryException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  protected void handleNode(Content profileContent, Map<Node, Future<Map<String, Object>>> baseMap,
      Map<String, Object> map) throws RepositoryException, InterruptedException,
      ExecutionException {
    // If our map contains this node, that means one of the provides had some information
    // for it.
    // We will use the provider.
    if (baseMap.containsKey(profileContent.getPath())) {
      map.putAll(baseMap.get(profileContent.getPath()).get());
    } else {

      // The node wasn't found in the baseMap.
      // We just dump the JCR properties.
      map.putAll(profileContent.getProperties());
      map.put("jcr:path", PathUtils.translateAuthorizablePath(profileContent.getPath()));
      map.put("jcr:name", StorageClientUtils.getObjectName(profileContent.getPath()));

      // We loop over the child nodes, but each node get checked against the baseMap
      // again.
      for (Content childProfile : profileContent.listChildren()) {
        ValueMap childMap = new ValueMapDecorator(new HashMap<String, Object>());
        handleNode(childProfile, baseMap, childMap);
        map.put(StorageClientUtils.getObjectName(childProfile.getPath()), childMap);
      }
    }
  }

  /**
   * {@inheritDoc}
   * @throws AccessDeniedException 
   * @throws StorageClientException 
   *
   * @see org.sakaiproject.nakamura.api.profile.ProfileService#getCompactProfileMap(org.apache.jackrabbit.api.security.user.Authorizable,
   *      javax.jcr.Session)
   */
  public ValueMap getCompactProfileMap(Authorizable authorizable, Session session)
      throws RepositoryException, StorageClientException, AccessDeniedException {
    // The map were we will stick the compact information in.
    ValueMap compactProfile;

    // Get the entire profile.
    ValueMap profile = getProfileMap(authorizable, session);
    if (profile == null) {
      compactProfile = null;
    } else {
      compactProfile = new ValueMapDecorator(new HashMap<String, Object>());

      if (authorizable.isGroup()) {
        // For a group we just dump it's title and description.
        compactProfile.put("groupid", authorizable.getId());
        compactProfile.put(GROUP_TITLE_PROPERTY, profile.get(GROUP_TITLE_PROPERTY));
        compactProfile.put(GROUP_DESCRIPTION_PROPERTY, profile
            .get(GROUP_DESCRIPTION_PROPERTY));
      } else {
        compactProfile.put(USER_PICTURE, profile.get(USER_PICTURE));

        try{
          ValueMap basicMap =(ValueMap) profile.get(USER_BASIC);
          if ( basicMap != null ) {
            compactProfile.put(USER_BASIC, basicMap);
          } else {
            LOG.warn("User {} has no basic profile (firstName, lastName and email not avaiable) ",authorizable.getId());
          }
        }catch(Exception e){
          LOG.warn("Can't get authprofile basic information. ", e);
        }
        // Backward compatible reasons.
        compactProfile.put("userid", authorizable.getId());
        compactProfile.put("hash", authorizable.getId());
      }
    }
    return compactProfile;
  }


  /**
   * Loops over an entire profile and checks if some of the nodes are marked as external.
   * If there is a node found that has it's sakai:source property set to external, we'll
   * look for a ProfileProvider that matches that path.
   *
   * @param profileContent
   *          The top node of a profile.
   * @param jcrSession 
   * @return
   * @throws RepositoryException
   */
  private Map<String, List<ProviderSettings>> scanForProviders(Content profileContent, Session jcrSession)
      throws RepositoryException {
    Map<String, List<ProviderSettings>> providerMap = new HashMap<String, List<ProviderSettings>>();
    return scanForProviders("", profileContent, providerMap, jcrSession);
  }

  /**
   * @param path
   * @param profileContent
   * @param providerMap
   * @param jcrSession 
   * @return
   * @throws RepositoryException
   */
  private Map<String, List<ProviderSettings>> scanForProviders(String path, Content profileContent,
      Map<String, List<ProviderSettings>> providerMap, Session jcrSession) throws RepositoryException {
    ProviderSettings settings = providerSettingsFactory.newProviderSettings(path, profileContent, jcrSession);
    if (settings == null) {
      for (Content childProfileContent : profileContent.listChildren()) {
        scanForProviders(StorageClientUtils.newPath(path, StorageClientUtils.getObjectName(childProfileContent.getPath())), childProfileContent, providerMap, jcrSession);
      }
    } else {

      List<ProviderSettings> l = providerMap.get(settings.getProvider());

      if (l == null) {
        l = new ArrayList<ProviderSettings>();
        providerMap.put(settings.getProvider(), l);
      }
      l.add(settings);
    }
    return providerMap;
  }


  protected void bindProfileProvider(ProfileProvider provider,
      Map<String, Object> properties) {
    String name = (String) properties.get(ProfileProvider.PROVIDER_NAME);
    System.err.println("Bound reference with name: " + name);
    if (name != null) {
      providers.put(name, provider);
    }
  }

  protected void unbindProfileProvider(ProfileProvider provider,
      Map<String, Object> properties) {
    String name = (String) properties.get(ProfileProvider.PROVIDER_NAME);
    System.err.println("Unbound reference with name: " + name);
    if (name != null) {
      providers.remove(name);
    }
  }

  public ValueMap getProfileMap(
      org.apache.jackrabbit.api.security.user.Authorizable authorizable, Session session) throws RepositoryException {
    org.sakaiproject.nakamura.api.lite.Session sparseSession = StorageClientUtils.adaptToSession(session);
    try {
      return getProfileMap(sparseSession.getAuthorizableManager().findAuthorizable(authorizable.getID()), session);
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new RepositoryException(e.getMessage(), e);
    }
  }

  public ValueMap getCompactProfileMap(
      org.apache.jackrabbit.api.security.user.Authorizable authorizable, Session session) throws RepositoryException {
    org.sakaiproject.nakamura.api.lite.Session sparseSession = StorageClientUtils.adaptToSession(session);
    try {
      return getCompactProfileMap(sparseSession.getAuthorizableManager().findAuthorizable(authorizable.getID()), session);
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new RepositoryException(e.getMessage(), e);
    }
  }
}
