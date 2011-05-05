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

import static org.sakaiproject.nakamura.api.profile.CountProvider.CONTACTS_PROP;
import static org.sakaiproject.nakamura.api.profile.CountProvider.CONTENT_ITEMS_PROP;
import static org.sakaiproject.nakamura.api.profile.CountProvider.COUNTS_PROP;
import static org.sakaiproject.nakamura.api.profile.CountProvider.GROUP_MEMBERSHIPS_PROP;
import static org.sakaiproject.nakamura.api.profile.CountProvider.GROUP_MEMBERS_PROP;
import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_DESCRIPTION_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_TITLE_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.PREFERRED_NAME;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_BASIC;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_COLLEGE;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_DATEOFBIRTH;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_DEPARTMENT;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_EMAIL_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_FIRSTNAME_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_LASTNAME_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_PICTURE;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_ROLE;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.profile.CountProvider;
import org.sakaiproject.nakamura.api.profile.ProfileConstants;
import org.sakaiproject.nakamura.api.profile.ProfileProvider;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.profile.ProviderSettings;
import org.sakaiproject.nakamura.api.resource.lite.LiteJsonImporter;
import org.sakaiproject.nakamura.api.user.BasicUserInfo;
import org.sakaiproject.nakamura.api.user.UserConstants;
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
@Component(immediate = true, metatype=true, specVersion="1.1")
@Service(value = ProfileService.class)
@References(value = { @Reference(name = "ProfileProviders", referenceInterface = ProfileProvider.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, strategy = ReferenceStrategy.EVENT, bind = "bindProfileProvider", unbind = "unbindProfileProvider") })
public class ProfileServiceImpl implements ProfileService {

  private Map<String, ProfileProvider> providers = new ConcurrentHashMap<String, ProfileProvider>();
  private ProviderSettingsFactory providerSettingsFactory = new ProviderSettingsFactory();
  public static final Logger LOG = LoggerFactory.getLogger(ProfileServiceImpl.class);
  
  private final static String[] DEFAULT_BASIC_PROFILE_ELEMENTS = new String[] {USER_FIRSTNAME_PROPERTY, USER_LASTNAME_PROPERTY,
    USER_EMAIL_PROPERTY, USER_PICTURE, PREFERRED_NAME, USER_ROLE, USER_DEPARTMENT, USER_COLLEGE, USER_DATEOFBIRTH};

  private final static String[] USER_COUNTS_PROPS = new String[] {CONTACTS_PROP, GROUP_MEMBERSHIPS_PROP, CONTENT_ITEMS_PROP, GROUP_MEMBERS_PROP};

  @Property(value={USER_FIRSTNAME_PROPERTY, USER_LASTNAME_PROPERTY,
      USER_EMAIL_PROPERTY, USER_PICTURE, PREFERRED_NAME, USER_ROLE, USER_DEPARTMENT, USER_COLLEGE, USER_DATEOFBIRTH})
  public final static String BASIC_PROFILE_ELEMENTS = "basicProfileElements";

  private String[] basicProfileElements;
  
  @Reference
  private CountProvider countProvider;
  
  @Activate
  protected void activate(Map<String, Object> properties ) {
    basicProfileElements = OsgiUtil.toStringArray(properties.get(BASIC_PROFILE_ELEMENTS), DEFAULT_BASIC_PROFILE_ELEMENTS);
  }

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
    if (User.ANON_USER.equals(authorizable.getId())) {
      return anonymousProfile();
    }
    String profilePath = LitePersonalUtils.getProfilePath(authorizable.getId());
    org.sakaiproject.nakamura.api.lite.Session sparseSession = StorageClientUtils.adaptToSession(session);
    ContentManager contentManager = sparseSession.getContentManager();
    ValueMap profileMap = new ValueMapDecorator(new HashMap<String, Object>());
    
    if (contentManager.exists(profilePath)) {
      Content profileContent = contentManager.get(profilePath);
      profileMap.putAll(getProfileMap(profileContent, session));
    }
    profileMap.put(USER_BASIC, basicProfileMapForAuthorizable(authorizable));
    profileMap.put(COUNTS_PROP, countsMapforAuthorizable(authorizable, sparseSession));
    if (authorizable.isGroup()) {
      addGroupProperties(authorizable, profileMap);
    } else {
      addUserProperties(authorizable, profileMap);
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
    Map<Content, Future<Map<String, Object>>> providedNodeData = new HashMap<Content, Future<Map<String, Object>>>();
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
      final String resourceType = (String) profileContent
          .getProperty("sling:resourceType");
      if (ProfileConstants.USER_PROFILE_RT.equals(resourceType)) {
        map.put("userid", PathUtils.getAuthorizableId(profileContent.getPath()));
      } else if (ProfileConstants.GROUP_PROFILE_RT.equals(resourceType)) {
        map.put("groupid", PathUtils.getAuthorizableId(profileContent.getPath()));
      }
      org.sakaiproject.nakamura.api.lite.Session sparseSession = StorageClientUtils.adaptToSession(jcrSession);
      AuthorizableManager auManager = sparseSession.getAuthorizableManager();
      String authorizableId = PathUtils.getAuthorizableId(profileContent.getPath());
      Authorizable au = auManager.findAuthorizable(authorizableId);
      map.put(COUNTS_PROP, countsMapforAuthorizable(au, sparseSession));
      return map;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } catch (StorageClientException e) {
      throw new RuntimeException(e);
    } catch (AccessDeniedException e) {
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
  protected void handleNode(Content profileContent, Map<Content, Future<Map<String, Object>>> baseMap,
      Map<String, Object> map) throws RepositoryException, InterruptedException,
      ExecutionException {
    // If our map contains this node, that means one of the provides had some information
    // for it.
    // We will use the provider.
    if (baseMap.containsKey(profileContent.getPath())) {
      map.putAll(baseMap.get(profileContent.getPath()).get());
    } else {

      // The node wasn't found in the baseMap.
      // We just dump the Sparse properties excluding any system properties.
      for ( Entry<String, Object> e : profileContent.getProperties().entrySet()) {
        String k = e.getKey();
        if ( !k.startsWith("_") &&  !k.startsWith(":") ) {
          map.put(k,e.getValue());
        }
      }
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
   * @deprecated Replaced with {@link BasicUserInfo#getProperties(Authorizable)} in user bundle
   */
  public ValueMap getCompactProfileMap(Authorizable authorizable, Session session)
      throws RepositoryException, StorageClientException, AccessDeniedException {
    if (User.ANON_USER.equals(authorizable.getId())) {
      return anonymousProfile();
    }
    
    ValueMap compactProfile = new ValueMapDecorator(new HashMap<String, Object>());
    compactProfile.put(USER_BASIC, basicProfileMapForAuthorizable(authorizable));

    if (authorizable.isGroup()) {
      addGroupProperties(authorizable, compactProfile);
    } else {
      addUserProperties(authorizable, compactProfile);
    }
    return compactProfile;
  }

  private void addUserProperties(Authorizable user, ValueMap profileMap) {
    // Backward compatible reasons.
    profileMap.put("rep:userId", user.getId());
    profileMap.put("userid", user.getId());
    profileMap.put("hash", user.getId());
  }

  private void addGroupProperties(Authorizable group, ValueMap profileMap) {
    // For a group we just dump it's title and description.
    profileMap.put("groupid", group.getId());
    profileMap.put("sakai:group-id", group.getId());
    profileMap.put(GROUP_TITLE_PROPERTY, group.getProperty(GROUP_TITLE_PROPERTY));
    profileMap.put(GROUP_DESCRIPTION_PROPERTY, group
        .getProperty(GROUP_DESCRIPTION_PROPERTY));
  }

  private ValueMap basicProfileMapForAuthorizable(Authorizable authorizable) {
    Builder<String, String> propertyBuilder = ImmutableMap.builder();
    for (String profileElementName : basicProfileElements) {
      if (authorizable.hasProperty(profileElementName)) {
        propertyBuilder.put(profileElementName, (String)authorizable.getProperty(profileElementName));
      }
    }
    // The map were we will stick the compact information in.
    ValueMap compactProfile = basicProfile(propertyBuilder.build());
    if ( authorizable.hasProperty("access")) {
      compactProfile.put("access", authorizable.getProperty("access"));
    } else {
      compactProfile.put(UserConstants.USER_BASIC_ACCESS, UserConstants.EVERYBODY_ACCESS_VALUE);
    }
    return compactProfile;
  }

  private ValueMap countsMapforAuthorizable(Authorizable authorizable, org.sakaiproject.nakamura.api.lite.Session session) throws AccessDeniedException, StorageClientException {
    if (countProvider != null) {
      if (countProvider.needsRefresh(authorizable)) {
        countProvider.update(authorizable);
      }
    } else {
      throw new IllegalStateException("@Reference CountProvider is null!");
    }
    Builder<String, Object> propertyBuilder = ImmutableMap.builder();
    for (String countPropName : USER_COUNTS_PROPS) {
      if (authorizable.hasProperty(countPropName)) {
        propertyBuilder.put(countPropName, authorizable.getProperty(countPropName));
      }
    }
    ValueMap allCounts = new ValueMapDecorator(propertyBuilder.build());
    if (LOG.isDebugEnabled())LOG.debug("counts map: {} for authorizableId: {}", new Object[]{allCounts, authorizable.getId()});
    return allCounts;
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

  
  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.profile.ProfileService#getCompactProfileMap(org.sakaiproject.nakamura.api.lite.authorizable.Authorizable, javax.jcr.Session)
   * @deprecated Replaced with {@link BasicUserInfo#getProperties(org.apache.jackrabbit.api.security.user.Authorizable, Session)} in user bundle
   */
  public ValueMap getCompactProfileMap(
      org.apache.jackrabbit.api.security.user.Authorizable authorizable, Session session) throws RepositoryException {
    org.sakaiproject.nakamura.api.lite.Session sparseSession = StorageClientUtils.adaptToSession(session);
    try {
      return getCompactProfileMap(sparseSession.getAuthorizableManager().findAuthorizable(authorizable.getID()), session);
      //BasicUserInfo basicUserInfo = new BasicUserInfo();
      //return new ValueMapDecorator(basicUserInfo.getProperties(sparseSession.getAuthorizableManager().findAuthorizable(authorizable.getID())));
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new RepositoryException(e.getMessage(), e);
    }
  }
  
  private ValueMap basicProfile(Map<String, String> elementsMap) {
    ValueMap basic = new ValueMapDecorator(new HashMap<String, Object>());
    ValueMap elements = new ValueMapDecorator(new HashMap<String, Object>());
    for (String key : elementsMap.keySet()) {
      elements.put(key, new ValueMapDecorator(ImmutableMap.of("value", (Object) elementsMap.get(key))));
    }
    basic.put("elements", elements);
    return basic;
  }
  
  
  private ValueMap anonymousProfile() {
    ValueMap rv = new ValueMapDecorator(new HashMap<String, Object>());
    rv.put("rep:userId", User.ANON_USER);
    ValueMap basicProfile =  basicProfile(
        ImmutableMap.of(USER_FIRSTNAME_PROPERTY, "Anonymous", USER_LASTNAME_PROPERTY, "User", USER_EMAIL_PROPERTY, "anon@sakai.invalid"));
    basicProfile.put(UserConstants.USER_BASIC_ACCESS, UserConstants.EVERYBODY_ACCESS_VALUE);
    rv.put(USER_BASIC,basicProfile);
    return rv;
  }

  public void update(org.sakaiproject.nakamura.api.lite.Session session,
      String profilePath, JSONObject json) throws StorageClientException,
      AccessDeniedException, JSONException {
    String authorizableId = PathUtils.getAuthorizableId(profilePath);
    // update the authorizable
    if (authorizableId != null) {
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      Authorizable a = authorizableManager.findAuthorizable(authorizableId);
      if (a != null) {
        if (json.has("basic")) {
          JSONObject basic = json.getJSONObject("basic");
          if (basic.has("elements")) {
            JSONObject elements = basic.getJSONObject("elements");
            for (String element : basicProfileElements) {
              if (elements.has(element)) {
                JSONObject elementObject = elements.getJSONObject(element);
                if (elementObject.has("value")) {
                  a.setProperty(element, elementObject.get("value"));
                }
              }
            }
            if ( basic.has("access")) {
              a.setProperty("access", basic.get("access"));
            }
          }
        }
        if (json.has(GROUP_TITLE_PROPERTY)) {
          a.setProperty(GROUP_TITLE_PROPERTY, json.get(GROUP_TITLE_PROPERTY));
        }
        if (json.has(GROUP_DESCRIPTION_PROPERTY)) {
          a.setProperty(GROUP_DESCRIPTION_PROPERTY, json.get(GROUP_DESCRIPTION_PROPERTY));
        }
        authorizableManager.updateAuthorizable(a);
      }
    }
    // update the profile content
    ContentManager contentManager = session.getContentManager();
    AccessControlManager accessControlManger = session.getAccessControlManager();
    LiteJsonImporter importer = new LiteJsonImporter();
    importer.importContent(contentManager, json, profilePath, true, true,true, accessControlManger);

  }
}
