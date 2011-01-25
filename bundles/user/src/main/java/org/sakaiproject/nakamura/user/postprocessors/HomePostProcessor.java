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
package org.sakaiproject.nakamura.user.postprocessors;

import com.google.common.collect.ImmutableMap;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_HOME_RESOURCE_TYPE;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_HOME_RESOURCE_TYPE;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.nakamura.api.user.AuthorizableEventUtil;
import org.sakaiproject.nakamura.api.user.LiteAuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This PostProcessor listens to post operations on User and Group objects and creates
 * or modifies that authorizable's home folder as necessary.
 *
 */
@Component(immediate = true, metatype = true)
@Service(value = LiteAuthorizablePostProcessor.class)
@Properties(value = {
    @org.apache.felix.scr.annotations.Property(name = "service.vendor", value = "The Sakai Foundation"),
    @org.apache.felix.scr.annotations.Property(name = "service.description", value = "Post Processes User and Group operations"),
    @org.apache.felix.scr.annotations.Property(name = "service.ranking", intValue=0)})
public class HomePostProcessor implements LiteAuthorizablePostProcessor {
  
  /**
   * Available default access settings for a new User or Group.
   */
  static final String VISIBILITY_PRIVATE = "private";
  static final String VISIBILITY_LOGGED_IN = "logged_in";
  static final String VISIBILITY_PUBLIC = "public";
  static final String SEARCH_EXCLUDE_TREE = "sakai:search-exclude-tree";

  static final String SPARSE_CONTENT_TYPE = "sparse/Content";
  static final String SLING_RESOURCE_SUPER_TYPE_PROPERTY = "sling/resourceSuperType";
@org.apache.felix.scr.annotations.Property(description = "The default access settings for the home of a new user or group.",
    value = VISIBILITY_PUBLIC,
    options = {
      @PropertyOption(name = VISIBILITY_PRIVATE, value = "The home is private."),
      @PropertyOption(name = VISIBILITY_LOGGED_IN, value = "The home is blocked to anonymous users; all logged-in users can see it."),
      @PropertyOption(name = VISIBILITY_PUBLIC, value = "The home is completely public.")
    }
  )
  static final String VISIBILITY_PREFERENCE = "org.sakaiproject.nakamura.personal.visibility.preference";
  static final String VISIBILITY_PREFERENCE_DEFAULT = VISIBILITY_PUBLIC;

  @Reference
  private EventAdmin eventAdmin;

  private String visibilityPreference;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(HomePostProcessor.class);
  
  //default constructor needed to be an OSGi Component
  public HomePostProcessor() {
    
  }

  public HomePostProcessor(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.servlets.post.Modification, java.util.Map)
   */
  public void process(Authorizable authorizable, Session session, Modification change,
      Map<String, Object[]> parameters) throws Exception {
    if (!ModificationType.DELETE.equals(change.getType())) {
      LOGGER.debug("Processing  {} ", authorizable.getId());
      try {
        if (ModificationType.CREATE.equals(change.getType())) {
          createHomeFolder(session, authorizable, change, parameters);
        } else {
          updateHomeFolder(session, authorizable, change, parameters);
        }
        fireEvent(session, authorizable.getId(), change);
        LOGGER.debug("DoneProcessing  {} ", authorizable.getId());
      } catch (Exception ex) {
        LOGGER.error("Post Processing failed " + ex.getMessage(), ex);
      }
    }
  }

  /**
   * Creates the home folder for a {@link User user} or a {@link Group group}. It will
   * also create all the subfolders such as private, public, ..
   *
   * @param session
   * @param authorizable
   * @param isGroup
   * @param change
   * @throws RepositoryException
   * @throws StorageClientException
   * @throws AccessDeniedException
   */
  private void createHomeFolder(Session session, Authorizable authorizable,
      Modification change, Map<String, Object[]> parameters) throws AccessDeniedException, StorageClientException {
    ContentManager contentManager = session.getContentManager();
//    String homeFolderPath = PersonalUtils.getHomePath(authorizable);
    String homeFolderPath = "a:" + authorizable.getId();
    Content homeFolder = new Content(homeFolderPath, null);
    if (homeFolder.isNew()) {
      LOGGER.debug("Created Home Node for {} at   {} user was {} ", new Object[] {
          authorizable.getId(), homeFolder, session.getUserId() });
    } else {
      LOGGER.debug("Existing Home Node for {} at   {} user was {} ", new Object[] {
          authorizable.getId(), homeFolder, session.getUserId() });
    }

    if ( !UserConstants.ANON_USERID.equals(authorizable.getId()) ) {
      initializeAccess(homeFolder, session, authorizable);
    }

    refreshOwnership(session, authorizable, homeFolderPath);

    // add things to home
    decorateHome(homeFolder, authorizable);

    // Create the public, private, authprofile
    createPrivate(session, authorizable);
    createPublic(session, authorizable);

    if (authorizable instanceof Group) {
      String pathString = homeFolderPath + "/joinrequests";
      contentManager.update(new Content(pathString, ImmutableMap.of(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, StorageClientUtils.toStore("sakai/joinrequests"))));
    }
    
    contentManager.update(homeFolder);
  }

  private void decorateHome(Content homeNode, Authorizable authorizable) {
    // set the home node resource type
    homeNode.setProperty(SLING_RESOURCE_SUPER_TYPE_PROPERTY, StorageClientUtils.toStore(SPARSE_CONTENT_TYPE));
    if (authorizable instanceof Group) {
      homeNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, StorageClientUtils.toStore(GROUP_HOME_RESOURCE_TYPE));
    } else {
      homeNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, StorageClientUtils.toStore(USER_HOME_RESOURCE_TYPE));
    }

    // set whether the home node should be excluded from searches
    if (authorizable.hasProperty(SEARCH_EXCLUDE_TREE)) {
      homeNode.setProperty(SEARCH_EXCLUDE_TREE,
          StorageClientUtils.toStore(authorizable.getProperty(SEARCH_EXCLUDE_TREE)));
    }
  }

//  /**
//   * @param principalManager
//   * @param managerSettings
//   * @return
//   * @throws RepositoryException
//   */
//  private Principal[] valuesToPrincipal(Value[] values, Principal[] defaultValue,
//      PrincipalManager principalManager) throws RepositoryException {
//    // An explicitly empty list of group viewers or managers does not mean the
//    // same thing as having no group viewers or managers property, and so
//    // a zero-length array should still override the defaults.
//    if (values != null) {
//      Principal[] valueAsStrings = new Principal[values.length];
//      for (int i = 0; i < values.length; i++) {
//        valueAsStrings[i] = principalManager.getPrincipal(values[i].getString());
//        if ( valueAsStrings[i] == null ) {
//          LOGGER.warn("Principal {} cant be resolved, will be ignored ",values[i].getString());
//        }
//      }
//      return valueAsStrings;
//    } else {
//      return defaultValue;
//    }
//  }

  /**
   * Creates the private folder in the user his home space.
   * TODO As of 2010-09-28 the "private" node is no longer used by any Nakamura
   * component. Can this code be eliminated?
   *
   * @param session
   *          The session to create the node
   * @param athorizable
   *          The Authorizable to create it for
   * @return The {@link Node node} that represents the private folder.
   * @throws StorageClientException
   * @throws AccessDeniedException
   */
  private Content createPrivate(Session session, Authorizable authorizable)
      throws StorageClientException, AccessDeniedException {
    ContentManager contentManager = session.getContentManager();
    // TODO BL120 still waiting on port of PersonalUtils
//    String privatePath = PersonalUtils.getPrivatePath(authorizable);
    String privatePath = "a:" + authorizable.getId() + "/private";
    if (contentManager.equals(privatePath)) {
      return contentManager.get(privatePath);
    }
    LOGGER.debug("creating or replacing ACLs for private at {} ", privatePath);
    Content privateNode = new Content(privatePath, null);
    contentManager.update(privateNode);

    LOGGER.debug("Done creating private at {} ", privatePath);
    return privateNode;
  }

  /**
   * Set access controls on the new User or Group node according to the profile
   * preference configuration property.
   *
   * @param node
   * @param session
   * @param authorizable
   * @throws RepositoryException
   * @throws StorageClientException
   * @throws AccessDeniedException
   */
  private void initializeAccess(Content node, Session session, Authorizable authorizable) throws StorageClientException, AccessDeniedException {
    String somePath = node.getPath();
    String userId = authorizable.getId();
    AccessControlManager adminAccessControl = session.getAccessControlManager();
    AuthorizableManager adminAuthManager = session.getAuthorizableManager();
    Authorizable user = adminAuthManager.findAuthorizable(userId);
    if (user == null) {
      throw new StorageClientException("Expected user to exist for initialization: {}");
    }
      List<AclModification> aclModifications = new ArrayList<AclModification>();
    // KERN-886 : Depending on the profile preference we set some ACL's on the profile.
    if ( UserConstants.ANON_USERID.equals(authorizable.getId()) ) {
      AclModification.addAcl(Boolean.TRUE, Permissions.CAN_READ, User.ANON_USER, aclModifications);
      AclModification.addAcl(Boolean.TRUE, Permissions.CAN_READ, Group.EVERYONE, aclModifications);
    } else if (VISIBILITY_PUBLIC.equals(visibilityPreference)) {
      AclModification.addAcl(Boolean.TRUE, Permissions.CAN_READ, User.ANON_USER, aclModifications);
      AclModification.addAcl(Boolean.TRUE, Permissions.CAN_READ, Group.EVERYONE, aclModifications);
    } else if (VISIBILITY_LOGGED_IN.equals(visibilityPreference)) {
      AclModification.addAcl(Boolean.FALSE, Permissions.CAN_READ, User.ANON_USER, aclModifications);
      AclModification.addAcl(Boolean.FALSE, Permissions.CAN_READ, Group.EVERYONE, aclModifications);
    } else if (VISIBILITY_PRIVATE.equals(visibilityPreference)) {
      AclModification.addAcl(Boolean.FALSE, Permissions.CAN_READ, User.ANON_USER, aclModifications);
      AclModification.addAcl(Boolean.FALSE, Permissions.CAN_READ, Group.EVERYONE, aclModifications);
    }
    AclModification[] arrayOfModifications = aclModifications
        .toArray(new AclModification[aclModifications.size()]);
    adminAccessControl.setAcl(Security.ZONE_CONTENT, somePath,
        arrayOfModifications);
  }

  /**
   * Creates the public folder in the user his home space.
   *
   * @param session
   *          The session to create the node
   * @param athorizable
   *          The Authorizable to create it for
   * @return The {@link Node node} that represents the public folder.
   * @throws StorageClientException
   * @throws AccessDeniedException
   */
  private Content createPublic(Session session, Authorizable athorizable)
      throws StorageClientException, AccessDeniedException {
    ContentManager contentManager = session.getContentManager();
    String publicPath = "a:" + athorizable.getId() + "/public";
    if (!contentManager.exists(publicPath)) {
      LOGGER.debug("Creating Public  for {} at   {} ", athorizable.getId(), publicPath);
      contentManager.update(new Content(publicPath, null));
    }
    return contentManager.get(publicPath);
  }

  // event processing
  // -----------------------------------------------------------------------------

  /**
   * Fire events, into OSGi, one synchronous one asynchronous.
   *
   * @param operation
   *          the operation being performed.
   * @param session
   *          the session performing operation.
   * @param request
   *          the request that triggered the operation.
   * @param authorizable
   *          the authorizable that is the target of the operation.
   * @param changes
   *          a list of {@link Modification} caused by the operation.
   */
  private void fireEvent(Session session, String principalName, Modification change) {
    try {
      String user = session.getUserId();
      String path = change.getDestination();
      if (path == null) {
        path = change.getSource();
      }
      if (AuthorizableEventUtil.isAuthorizableModification(change)) {
        LOGGER.debug("Got Authorizable modification: " + change);
        switch (change.getType()) {
          case COPY:
          case CREATE:
          case DELETE:
          case MOVE:
            LOGGER.debug("Ignoring unknown modification type: " + change.getType());
            break;
          case MODIFY:
            eventAdmin.postEvent(AuthorizableEventUtil.newGroupEvent(user, change));
            break;
          }
        } else if (path.endsWith(principalName)) {
          switch (change.getType()) {
          case COPY:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.update, user, principalName, change));
            break;
          case CREATE:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.create, user, principalName, change));
            break;
          case DELETE:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.delete, user, principalName, change));
            break;
          case MODIFY:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.update, user, principalName, change));
            break;
          case MOVE:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.update, user, principalName, change));
            break;
          }
        }
    } catch (Throwable t) {
      LOGGER.warn("Failed to fire event", t);
    }
  }

  /**
   * @param eventAdmin
   *          the new EventAdmin service to bind to this service.
   */
  protected void bindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  /**
   * @param eventAdmin
   *          the EventAdminService to be unbound from this service.
   */
  protected void unbindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = null;
  }

  private void updateHomeFolder(Session session, Authorizable authorizable,
      Modification change, Map<String, Object[]> parameters) throws StorageClientException, AccessDeniedException {
      // Mirror the current state of the Authorizable's visibility and
      // management controls, if the current session has the right to do
      // so.
      // TODO Replace these implicit side-effects with something more controllable
      // by the client.
      refreshOwnership(session, authorizable, "a:" + authorizable.getId());
  }

  /**
   * If the current session has sufficient rights, synchronize home folder
   * access to match the current accessibility of the Jackrabbit User or
   * Group. Currently this is done for every update, overwriting any ACLs
   * which might have been explicitly set on the home node.
   *
   * TODO Since we can now use normal ACLs to control access to Authorizables, and since
   * the UX also directly manages access of Home resource do we really still
   * need to do this?
   *
   * @param session
   * @param authorizable
   * @param homeFolderPath
    * @throws AccessDeniedException
   * @throws StorageClientException
   */
  private void refreshOwnership(Session session, Authorizable authorizable,
      String homeFolderPath) throws StorageClientException, AccessDeniedException {
    if (isAbleToControlAccess(session, homeFolderPath, authorizable)) {
      List<AclModification> aclModifications = new ArrayList<AclModification>();

      // If the Authorizable has a managers list, everyone on that list gets write access.
      // Otherwise, the Authorizable itself is the owner.
      String[] managerSettings = StorageClientUtils.toStringArray(authorizable.getProperty(UserConstants.PROP_GROUP_MANAGERS));
      if (managerSettings == null) {
        managerSettings = new String[] {authorizable.getId()};
      }
      for (String manager : managerSettings) {
        LOGGER.debug("User {} is attempting to make {} a manager ", session.getUserId(),
            manager);
        AclModification.addAcl(true, Permissions.CAN_MANAGE, manager, aclModifications);
      }

      // Do not automatically give read-access to anonymous and everyone, since that
      // forces Home folders to be public and overwrites configuration settings.
      String[] viewerSettings = StorageClientUtils.toStringArray(authorizable.getProperty(UserConstants.PROP_GROUP_VIEWERS));
      if (viewerSettings != null) {
        for (String viewer : viewerSettings) {
          LOGGER.debug("User {} is attempting to make {} a viewer ", session.getUserId(),
              viewer);
          AclModification.addAcl(true, Permissions.CAN_READ, viewer, aclModifications);
        }
      }
      AccessControlManager accessControlManager = session.getAccessControlManager();
      accessControlManager.setAcl(Security.ZONE_CONTENT, homeFolderPath,
          aclModifications.toArray(new AclModification[aclModifications.size()]));
      LOGGER.debug("Set ACL on Home for {} at {} ", authorizable.getId(), homeFolderPath);
    }
  }

  private boolean isAbleToControlAccess(Session session, String homeFolderPath, Authorizable authorizable) throws StorageClientException {
    AccessControlManager accessControlManager = session.getAccessControlManager();
    return accessControlManager.can(authorizable, Security.ZONE_CONTENT, homeFolderPath,
        Permissions.CAN_MANAGE);
  }

  @Activate @Modified
  protected void modified(Map<?, ?> props) {
    visibilityPreference = OsgiUtil.toString(props.get(VISIBILITY_PREFERENCE),
        VISIBILITY_PREFERENCE_DEFAULT);
  }
}