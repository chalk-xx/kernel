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
package org.sakaiproject.nakamura.personal;

import static javax.jcr.security.Privilege.JCR_ALL;
import static javax.jcr.security.Privilege.JCR_READ;
import static javax.jcr.security.Privilege.JCR_WRITE;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants.VISIBILITY_LOGGED_IN;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants.VISIBILITY_PRIVATE;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants.VISIBILITY_PUBLIC;
import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_HOME_RESOURCE_TYPE;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_AUTHORIZABLE_PATH;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_HOME_RESOURCE_TYPE;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.jcr.JCRConstants;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.nakamura.api.user.AuthorizableEventUtil;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

/**
 * This PostProcessor listens to post operations on User and Group objects and creates
 * or modifies that authorizable's home folder as necessary.
 *
 */
@Component(immediate = true, metatype = true)
@Service(value = AuthorizablePostProcessor.class)
@Properties(value = {
    @org.apache.felix.scr.annotations.Property(name = "service.vendor", value = "The Sakai Foundation"),
    @org.apache.felix.scr.annotations.Property(name = "service.description", value = "Post Processes User and Group operations"),
    @org.apache.felix.scr.annotations.Property(name = "service.ranking", intValue=0)})
public class HomePostProcessor implements AuthorizablePostProcessor {

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

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.servlets.post.Modification, java.util.Map)
   */
  public void process(Authorizable authorizable, Session session, Modification change,
      Map<String, Object[]> parameters) throws Exception {
    if (!ModificationType.DELETE.equals(change.getType())) {
      LOGGER.debug("Processing  {} ", authorizable.getID());
      try {
        if (ModificationType.CREATE.equals(change.getType())) {
          createHomeFolder(session, authorizable, change, parameters);
        } else {
          updateHomeFolder(session, authorizable, change, parameters);
        }
        fireEvent(session, authorizable.getID(), change);
        LOGGER.debug("DoneProcessing  {} ", authorizable.getID());
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
   */
  private void createHomeFolder(Session session, Authorizable authorizable,
      Modification change, Map<String, Object[]> parameters) throws RepositoryException {
    String homeFolderPath = PersonalUtils.getHomePath(authorizable);

    Node homeNode = JcrUtils.deepGetOrCreateNode(session, homeFolderPath);
    if (homeNode.isNew()) {
      LOGGER.debug("Created Home Node for {} at   {} user was {} ", new Object[] {
          authorizable.getID(), homeNode, session.getUserID() });
    } else {
      LOGGER.debug("Existing Home Node for {} at   {} user was {} ", new Object[] {
          authorizable.getID(), homeNode, session.getUserID() });
    }

    if ( !UserConstants.ANON_USERID.equals(authorizable.getID()) ) {
      initializeAccess(homeNode, session, authorizable);
    }

    refreshOwnership(session, authorizable, homeFolderPath);

    // add things to home
    decorateHome(homeNode, authorizable);

    // Create the public, private, authprofile
    createPrivate(session, authorizable);
    createPublic(session, authorizable);

    if (authorizable.isGroup()) {
      // setup a joinrequests node for the group
      Value[] path = authorizable.getProperty(PROP_AUTHORIZABLE_PATH);
      if (path != null && path.length > 0) {
        String pathString = "/_group" + path[0].getString() + "/joinrequests";
        Node messageStore = JcrUtils.deepGetOrCreateNode(session, pathString);
        messageStore.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            "sakai/joinrequests");
      }
    }
  }

  private void decorateHome(Node homeNode, Authorizable authorizable)
      throws RepositoryException {
    // set the home node resource type
    if (authorizable.isGroup()) {
      homeNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, GROUP_HOME_RESOURCE_TYPE);
    } else {
      homeNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, USER_HOME_RESOURCE_TYPE);
    }

    // set whether the home node should be excluded from searches
    if (authorizable.hasProperty(JCRConstants.SEARCH_EXCLUDE_TREE)) {
      homeNode.setProperty(JCRConstants.SEARCH_EXCLUDE_TREE,
          authorizable.getProperty(JCRConstants.SEARCH_EXCLUDE_TREE)[0]);
    }
  }

  /**
   * @param principalManager
   * @param managerSettings
   * @return
   * @throws RepositoryException
   */
  private Principal[] valuesToPrincipal(Value[] values, Principal[] defaultValue,
      PrincipalManager principalManager) throws RepositoryException {
    // An explicitly empty list of group viewers or managers does not mean the
    // same thing as having no group viewers or managers property, and so
    // a zero-length array should still override the defaults.
    if (values != null) {
      Principal[] valueAsStrings = new Principal[values.length];
      for (int i = 0; i < values.length; i++) {
        valueAsStrings[i] = principalManager.getPrincipal(values[i].getString());
        if ( valueAsStrings[i] == null ) {
          LOGGER.warn("Principal {} cant be resolved, will be ignored ",values[i].getString());
        }
      }
      return valueAsStrings;
    } else {
      return defaultValue;
    }
  }

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
   * @throws RepositoryException
   */
  private Node createPrivate(Session session, Authorizable authorizable)
      throws RepositoryException {
    String privatePath = PersonalUtils.getPrivatePath(authorizable);
    if (session.itemExists(privatePath)) {
      return session.getNode(privatePath);
    }
    LOGGER.debug("creating or replacing ACLs for private at {} ", privatePath);
    Node privateNode = JcrUtils.deepGetOrCreateNode(session, privatePath);

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
   */
  private void initializeAccess(Node node, Session session, Authorizable authorizable) throws RepositoryException {
    String nodePath = node.getPath();
    PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
    Principal everyone = principalManager.getEveryone();
    Principal anon = new Principal() {
      public String getName() {
        return UserConstants.ANON_USERID;
      }
    };
    // KERN-886 : Depending on the profile preference we set some ACL's on the profile.
    if ( UserConstants.ANON_USERID.equals(authorizable.getID()) ) {
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, anon,
          new String[] { JCR_READ }, null, null, null);
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, everyone,
          new String[] { JCR_READ }, null, null, null);
    } else if (VISIBILITY_PUBLIC.equals(visibilityPreference)) {
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, anon,
          new String[] { JCR_READ }, null, null, null);
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, everyone,
          new String[] { JCR_READ }, null, null, null);
    } else if (VISIBILITY_LOGGED_IN.equals(visibilityPreference)) {
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, anon, null,
          new String[] { JCR_READ }, null, null);
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, everyone,
          new String[] { JCR_READ }, null, null, null);
    } else if (VISIBILITY_PRIVATE.equals(visibilityPreference)) {
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, anon, null,
          new String[] { JCR_READ }, null, null);
      AccessControlUtil.replaceAccessControlEntry(session, nodePath, everyone, null,
          new String[] { JCR_READ }, null, null);
    }
  }

  /**
   * Creates the public folder in the user his home space.
   *
   * @param session
   *          The session to create the node
   * @param athorizable
   *          The Authorizable to create it for
   * @return The {@link Node node} that represents the public folder.
   * @throws RepositoryException
   */
  private Node createPublic(Session session, Authorizable athorizable)
      throws RepositoryException {
    String publicPath = PersonalUtils.getPublicPath(athorizable);
    if (session.nodeExists(publicPath)) {
      // No more work needed at present.
      return session.getNode(publicPath);
    }
    LOGGER.debug("Creating Public  for {} at   {} ", athorizable.getID(), publicPath);
    Node publicNode = JcrUtils.deepGetOrCreateNode(session, publicPath);
    return publicNode;
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
      String user = session.getUserID();
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
      Modification change, Map<String, Object[]> parameters) throws RepositoryException {
      // Mirror the current state of the Authorizable's visibility and
      // management controls, if the current session has the right to do
      // so.
      // TODO Replace these implicit side-effects with something more controllable
      // by the client.
      refreshOwnership(session, authorizable, PersonalUtils.getHomePath(authorizable));
  }

  /**
   * If the current session has sufficient rights, synchronize home folder
   * access to match the current accessibility of the Jackrabbit User or
   * Group. Currently this is done for every update, overwriting any ACLs
   * which might have been explicitly set on the home node.
   *
   * @param session
   * @param authorizable
   * @param homeFolderPath
   * @throws RepositoryException
   */
  private void refreshOwnership(Session session, Authorizable authorizable,
      String homeFolderPath) throws RepositoryException {
    if (isAbleToControlAccess(session, homeFolderPath)) {
      PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);

      Value[] managerSettings = authorizable.getProperty(UserConstants.PROP_GROUP_MANAGERS);
      Value[] viewerSettings = authorizable.getProperty(UserConstants.PROP_GROUP_VIEWERS);

      // If the Authorizable has a managers list, everyone on that list gets write access.
      // Otherwise, the Authorizable itself is the owner.
      Principal[] managers = valuesToPrincipal(managerSettings,
          new Principal[] { authorizable.getPrincipal() }, principalManager);

      // Do not automatically give read-access to anonymous and everyone, since that
      // forces User Home folders to be public and overwrites configuration settings.
      Principal[] viewers = valuesToPrincipal(viewerSettings, new Principal[] { },
          principalManager);

      for (Principal manager : managers) {
        if ( manager != null && !UserConstants.ANON_USERID.equals(manager.getName()) ) {
          LOGGER.debug("User {} is attempting to make {} a manager ", session.getUserID(),
            manager.getName());
          AccessControlUtil.replaceAccessControlEntry(session, homeFolderPath, manager,
            new String[] { JCR_ALL }, null, null, null);
        }
      }
      for (Principal viewer : viewers) {
        if ( viewer != null && !UserConstants.ANON_USERID.equals(viewer.getName())) {
          LOGGER.debug("User {} is attempting to make {} a viewer ", session.getUserID(),
            viewer.getName());
          AccessControlUtil.replaceAccessControlEntry(session, homeFolderPath, viewer,
            new String[] { JCR_READ }, new String[] { JCR_WRITE }, null, null);
        }
      }
      LOGGER.debug("Set ACL on Node for {} at   {} ", authorizable.getID(), homeFolderPath);
    }
  }

  private boolean isAbleToControlAccess(Session session, String homeFolderPath) throws RepositoryException {
    AccessControlManager accessControlManager = AccessControlUtil.getAccessControlManager(session);
    Privilege[] modifyAclPrivileges = { accessControlManager.privilegeFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL) };
    return accessControlManager.hasPrivileges(homeFolderPath, modifyAclPrivileges);
  }

  @Activate @Modified
  protected void modified(Map<?, ?> props) {
    visibilityPreference = OsgiUtil.toString(props.get(VISIBILITY_PREFERENCE),
        VISIBILITY_PREFERENCE_DEFAULT);
  }
}
