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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.AuthorizableEventUtil;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;

/**
 * This PostProcessor listens to post operations on User objects and processes the
 * changes.
 * 
 * @scr.service interface="org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.component immediate="true" label="SitePostProcessor"
 *                description="Post Processor for User and Group operations" metatype="no"
 * @scr.property name="service.description"
 *               value="Post Processes User and Group operations"
 * 
 */
@Component(immediate=true, description="Post Processor for User and Group operations", metatype=false, label="PersonalAuthorizablePostProcessor")
@Service(value=AuthorizablePostProcessor.class)
@Properties(value={
  @org.apache.felix.scr.annotations.Property(name="service.vendor", value="The Sakai Foundation"),
  @org.apache.felix.scr.annotations.Property(name="service.description", value="Post Processes User and Group operations")
})
public class PersonalAuthorizablePostProcessor implements AuthorizablePostProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PersonalAuthorizablePostProcessor.class);

  /**
   */
  @Reference
  private EventAdmin eventAdmin;

  /**
   * @param request
   * @param changes
   * @throws Exception
   */
  public void process(Authorizable authorizable, Session session, Modification change)
      throws Exception {
    if (ModificationType.DELETE.equals(change.getType())) {
      deleteHomeNode(session, authorizable);
    } else {
      LOGGER.debug("Processing  {} ", authorizable.getID());
      try {
        createHomeFolder(session, authorizable, change);
        fireEvent(session, authorizable.getID(), change);
        LOGGER.debug("DoneProcessing  {} ", authorizable.getID());
      } catch (Exception ex) {
        LOGGER.error("Post Processing failed " + ex.getMessage(), ex);
      }
    }
  }

  /**
   * @param athorizable
   * @param changes
   * @throws RepositoryException
   * @throws ConstraintViolationException
   * @throws LockException
   * @throws VersionException
   * @throws PathNotFoundException
   */
  private void updateProperties(Session session, Node profileNode,
      Authorizable athorizable, Modification change) throws RepositoryException {

      String dest = change.getDestination();
      if (dest == null) {
        dest = change.getSource();
      }
      switch (change.getType()) {
      case DELETE:
        if (!dest.endsWith(athorizable.getID()) && profileNode != null) {
          String propertyName = PathUtils.lastElement(dest);
          if (profileNode.hasProperty(propertyName)) {
            Property prop = profileNode.getProperty(propertyName);
            prop.remove();
          }
        }
        break;
      }

    if (profileNode == null) {
      return;
    }

    // build a blacklist set of properties that should be kept private

    Set<String> privateProperties = new HashSet<String>();
    if (profileNode.hasProperty(UserConstants.PRIVATE_PROPERTIES)) {
      Value[] pp = profileNode.getProperty(UserConstants.PRIVATE_PROPERTIES).getValues();
      for (Value v : pp) {
        privateProperties.add(v.getString());
      }
    }
    // copy the non blacklist set of properties into the users profile.
    if (athorizable != null) {
      // explicitly add protected properties form the authorizable
      if (!profileNode.hasProperty("rep:userId")) {
        profileNode.setProperty("rep:userId", athorizable.getID());
      }
      Iterator<?> inames = athorizable.getPropertyNames();
      while (inames.hasNext()) {
        String propertyName = (String) inames.next();
        // No need to copy in jcr:* properties, otherwise we would copy over the uuid
        // which could lead to a lot of confusion.
        if (!propertyName.startsWith("jcr:") && !propertyName.startsWith("rep:")) {
          if (!privateProperties.contains(propertyName)) {
            Value[] v = athorizable.getProperty(propertyName);
            if (!(profileNode.hasProperty(propertyName) && profileNode.getProperty(
                propertyName).getDefinition().isProtected())) {
              if (v.length == 1) {
                profileNode.setProperty(propertyName, v[0]);
              } else {
                profileNode.setProperty(propertyName, v);
              }
            }
          }
        } else {
          LOGGER.debug("Not Updating {}", propertyName);
        }
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
   * @param changes
   * @return
   * @throws RepositoryException
   */
  private Node createHomeFolder(Session session, Authorizable authorizable, Modification change) throws RepositoryException {
    String homeFolderPath = PersonalUtils.getHomeFolder(authorizable);

    Node homeNode = JcrUtils.deepGetOrCreateNode(session, homeFolderPath);
    if (homeNode.isNew()) {
      LOGGER.info("Created Home Node for {} at   {} user was {} ", new Object[] {
          authorizable.getID(), homeNode, session.getUserID() });
    } else {
      LOGGER.info("Existing Home Node for {} at   {} user was {} ", new Object[] {
          authorizable.getID(), homeNode, session.getUserID() });
    }

    PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
    Principal anon = new Principal() {
      public String getName() {
        return UserConstants.ANON_USERID;
      }
    };

    Principal everyone = principalManager.getEveryone();

    Value[] managerSettings = authorizable.getProperty(UserConstants.PROP_GROUP_MANAGERS);
    Value[] viewerSettings = authorizable.getProperty(UserConstants.PROP_GROUP_VIEWERS);

    Principal[] managers = valuesToPrincipal(managerSettings,
        new Principal[] { authorizable.getPrincipal() }, principalManager);
    Principal[] viewers = valuesToPrincipal(viewerSettings, new Principal[] { anon,
        everyone }, principalManager);

    // The user can do everything on this node.
    for (Principal manager : managers) {
      LOGGER.info("User {} is attempting to make {} a manager ", session.getUserID(),
          manager.getName());
      AccessControlUtil.replaceAccessControlEntry(session, homeFolderPath, manager,
          new String[] { JCR_ALL }, null, null);
    }
    for (Principal viewer : viewers) {
      LOGGER.info("User {} is attempting to make {} a viewer ", session.getUserID(),
          viewer.getName());
      AccessControlUtil.replaceAccessControlEntry(session, homeFolderPath, viewer,
          new String[] { JCR_READ }, new String[] { JCR_WRITE }, null);
    }
    LOGGER.debug("Set ACL on Node for {} at   {} ", authorizable.getID(), homeNode);

    // Create the public, private, authprofile
    createPrivate(session, authorizable);
    createPublic(session, authorizable);
    Node profileNode = createProfile(session, authorizable);

    // Update the values on the profile node.
    updateProperties(session, profileNode, authorizable, change);
    return homeNode;
  }

  /**
   * @param session
   * @param homeFolderPath
   * @throws RepositoryException
   */
  private void dumpAccessControlList(Session session, String resourcePath)
      throws RepositoryException {
    AccessControlEntry[] declaredAccessControlEntries = getDeclaredAccessControlEntries(
        session, resourcePath);
    Map<String, Map<String, Set<String>>> aclMap = new LinkedHashMap<String, Map<String, Set<String>>>();
    for (AccessControlEntry ace : declaredAccessControlEntries) {
      Principal principal = ace.getPrincipal();
      Map<String, Set<String>> map = aclMap.get(principal.getName());
      if (map == null) {
        map = new LinkedHashMap<String, Set<String>>();
        aclMap.put(principal.getName(), map);
      }

      boolean allow = AccessControlUtil.isAllow(ace);
      if (allow) {
        Set<String> grantedSet = map.get("granted");
        if (grantedSet == null) {
          grantedSet = new LinkedHashSet<String>();
          map.put("granted", grantedSet);
        }
        Privilege[] privileges = ace.getPrivileges();
        for (Privilege privilege : privileges) {
          grantedSet.add(privilege.getName());
        }
      } else {
        Set<String> deniedSet = map.get("denied");
        if (deniedSet == null) {
          deniedSet = new LinkedHashSet<String>();
          map.put("denied", deniedSet);
        }
        Privilege[] privileges = ace.getPrivileges();
        for (Privilege privilege : privileges) {
          deniedSet.add(privilege.getName());
        }
      }
    }

    StringBuilder sb = new StringBuilder();
    for (Entry<String, Map<String, Set<String>>> acl : aclMap.entrySet()) {
      sb.append("Principal ").append(acl.getKey()).append("\n");
      for (Entry<String, Set<String>> ace : acl.getValue().entrySet()) {
        sb.append("\t").append(ace.getKey());
        for (String perm : ace.getValue()) {
          sb.append("[").append(perm).append("]");
        }
        sb.append("\n");
      }
    }

    LOGGER.info("Permissions at {} are \n {} ", resourcePath, sb.toString());
  }

  private AccessControlEntry[] getDeclaredAccessControlEntries(Session session,
      String absPath) throws RepositoryException {
    AccessControlManager accessControlManager = AccessControlUtil
        .getAccessControlManager(session);
    AccessControlPolicy[] policies = accessControlManager.getPolicies(absPath);
    for (AccessControlPolicy accessControlPolicy : policies) {
      if (accessControlPolicy instanceof AccessControlList) {
        AccessControlEntry[] accessControlEntries = ((AccessControlList) accessControlPolicy)
            .getAccessControlEntries();
        return accessControlEntries;
      }
    }
    return new AccessControlEntry[0];
  }

  /**
   * @param principalManager
   * @param managerSettings
   * @return
   * @throws RepositoryException
   */
  private Principal[] valuesToPrincipal(Value[] values, Principal[] defaultValue,
      PrincipalManager principalManager) throws RepositoryException {
    if (values != null && values.length > 0) {
      Principal[] valueAsStrings = new Principal[values.length];
      for (int i = 0; i < values.length; i++) {
        valueAsStrings[i] = principalManager.getPrincipal(values[i].getString());
      }
      return valueAsStrings;
    } else {
      return defaultValue;
    }
  }

  /**
   * @param request
   * @param authorizable
   * @return
   * @throws RepositoryException
   */
  private Node createProfile(Session session, Authorizable authorizable)
      throws RepositoryException {
    String path = PersonalUtils.getProfilePath(authorizable);
    Node profileNode = null;
    if (!isPostProcessingDone(session, authorizable)) {

      String type = nodeTypeForAuthorizable(authorizable.isGroup());
      LOGGER.debug("Creating or resetting Profile Node {} for authorizable {} ", path,
          authorizable.getID());
      profileNode = JcrUtils.deepGetOrCreateNode(session, path);
      profileNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, type);
      // Make sure we can place references to this profile node in the future.
      // This will make it easier to search on it later on.
      if (profileNode.canAddMixin(JcrConstants.MIX_REFERENCEABLE)) {
        profileNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
      }
    } else {
      profileNode = session.getNode(path);
    }
    return profileNode;
  }

  /**
   * Creates the private folder in the user his home space.
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
    // Make sure that this folder is completely private.
    PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
    Principal everyone = principalManager.getEveryone();
    Principal anon = new Principal() {
      public String getName() {
        return UserConstants.ANON_USERID;
      }
    };
    AccessControlUtil.replaceAccessControlEntry(session, privatePath, authorizable
        .getPrincipal(), new String[] { JCR_ALL }, null, null);
    AccessControlUtil.replaceAccessControlEntry(session, privatePath, anon, null,
        new String[] { JCR_READ, JCR_WRITE }, null);
    AccessControlUtil.replaceAccessControlEntry(session, privatePath, everyone, null,
        new String[] { JCR_READ, JCR_WRITE }, null);

    LOGGER.debug("Done creating private at {} ", privatePath);
    return privateNode;
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

  private void deleteHomeNode(Session session, Authorizable athorizable)
      throws RepositoryException {
    if (athorizable != null) {
      String path = PersonalUtils.getHomeFolder(athorizable);
      if (session.itemExists(path)) {
        Node node = (Node) session.getItem(path);
        node.remove();
      }
    }
  }

  private String nodeTypeForAuthorizable(boolean isGroup) {
    if (isGroup) {
      return UserConstants.GROUP_PROFILE_RESOURCE_TYPE;
    } else {
      return UserConstants.USER_PROFILE_RESOURCE_TYPE;
    }
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
            eventAdmin.postEvent(AuthorizableEventUtil.newGroupEvent(change));
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

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#getSequence()
   */
  public int getSequence() {
    return 0;
  }

  /**
   * Decide whether post-processing this user or group would be redundant because it has
   * already been done. The current logic uses the existence of a profile node of the
   * correct type as a marker.
   * 
   * @param session
   * @param authorizable
   * @return true if there is evidence that post-processing has already occurred for this
   *         user or group
   * @throws RepositoryException
   */
  private boolean isPostProcessingDone(Session session, Authorizable authorizable)
      throws RepositoryException {
    boolean isProfileCreated = false;
    String path = PersonalUtils.getProfilePath(authorizable);
    if (session.nodeExists(path)) {
      Node node = session.getNode(path);
      String type = nodeTypeForAuthorizable(authorizable.isGroup());
      if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
        if (node.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            .getString().equals(type)) {
          isProfileCreated = true;
        }
      }
    }
    return isProfileCreated;
  }
}
