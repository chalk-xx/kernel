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

import static org.sakaiproject.nakamura.util.ACLUtils.READ_DENIED;
import static org.sakaiproject.nakamura.util.ACLUtils.WRITE_DENIED;

import static org.sakaiproject.nakamura.api.user.UserConstants.SYSTEM_USER_MANAGER_GROUP_PATH;
import static org.sakaiproject.nakamura.api.user.UserConstants.SYSTEM_USER_MANAGER_GROUP_PREFIX;
import static org.sakaiproject.nakamura.api.user.UserConstants.SYSTEM_USER_MANAGER_USER_PATH;
import static org.sakaiproject.nakamura.api.user.UserConstants.SYSTEM_USER_MANAGER_USER_PREFIX;
import static org.sakaiproject.nakamura.util.ACLUtils.ADD_CHILD_NODES_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.MODIFY_PROPERTIES_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.REMOVE_CHILD_NODES_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.REMOVE_NODE_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.WRITE_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.READ_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.NODE_TYPE_MANAGEMENT_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.addEntry;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.AuthorizableEventUtil;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.api.user.UserPostProcessor;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * This PostProcessor listens to post operations on User objects and processes the
 * changes.
 * 
 * @scr.service interface="org.sakaiproject.nakamura.api.user.UserPostProcessor"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.component immediate="true" label="SitePostProcessor"
 *                description="Post Processor for User and Group operations" metatype="no"
 * @scr.property name="service.description"
 *               value="Post Processes User and Group operations"
 * @scr.reference name="EventAdmin" bind="bindEventAdmin" unbind="unbindEventAdmin"
 *                interface="org.osgi.service.event.EventAdmin"
 * 
 */
public class UserPostProcessorImpl implements UserPostProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(UserPostProcessorImpl.class);

  /**
   */
  private EventAdmin eventAdmin;

  /**
   * @param request
   * @param changes
   * @throws Exception
   */
  public void process(Authorizable athorizable, Session session,
      SlingHttpServletRequest request, List<Modification> changes) throws Exception {
    if ( athorizable == null ) {
      return;
    }
    try {
      String resourcePath = request.getRequestPathInfo().getResourcePath();
      if (resourcePath.equals(SYSTEM_USER_MANAGER_USER_PATH)) {
        createPrivate(session, athorizable);
        Node profileNode = createProfile(session, athorizable, false);
        updateProperties(session, profileNode, athorizable, changes);
        fireEvent(request, athorizable.getID(), changes);
      } else if (resourcePath.equals(SYSTEM_USER_MANAGER_GROUP_PATH)) {
        createPrivate(session, athorizable);
        Node profileNode = createProfile(session, athorizable, true);
        updateProperties(session, profileNode, athorizable, changes);
        fireEvent(request, athorizable.getID(), changes);
      } else if (resourcePath.startsWith(SYSTEM_USER_MANAGER_USER_PREFIX)) {
        createPrivate(session, athorizable);
        Node profileNode = createProfile(session, athorizable, false);
        updateProperties(session, profileNode, athorizable, changes);
        fireEvent(request, athorizable.getID(), changes);
      } else if (resourcePath.startsWith(SYSTEM_USER_MANAGER_GROUP_PREFIX)) {
        createPrivate(session, athorizable);
        Node profileNode = createProfile(session, athorizable, true);
        updateProperties(session, profileNode, athorizable, changes);
        fireEvent(request, athorizable.getID(), changes);
      }
    } catch (Exception ex) {
      LOGGER.error("Post Processing failed " + ex.getMessage(), ex);
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
      Authorizable athorizable, List<Modification> changes) throws RepositoryException {

    for (Modification m : changes) {
      String dest = m.getDestination();
      if (dest == null) {
        dest = m.getSource();
      }
      switch (m.getType()) {
      case DELETE:
        if (!dest.endsWith(athorizable.getID()) && profileNode != null) {
          String propertyName = PathUtils.lastElement(dest);
          if (profileNode.hasProperty(propertyName)) {
            Property prop = profileNode.getProperty(propertyName);
            changes.add(Modification.onDeleted(prop.getPath()));
            prop.remove();
          }
        } else {
          deleteProfileNode(session, athorizable);
        }
        break;
      }
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
        Property useridProp = profileNode.setProperty("rep:userId", athorizable.getID());
        changes.add(Modification.onModified(useridProp.getPath()));
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
              Property prop = null;
              if (v.length == 1) {
                prop = profileNode.setProperty(propertyName, v[0]);
              } else {
                prop = profileNode.setProperty(propertyName, v);
              }
              changes.add(Modification.onModified(prop.getPath()));
            }
          }
        } else {
          LOGGER.debug("Not Updating {}", propertyName);
        }
      }
    }
  }

  /**
   * @param request
   * @param authorizable
   * @return
   * @throws RepositoryException
   */
  private Node createProfile(Session session, Authorizable athorizable, boolean isGroup)
      throws RepositoryException {
    String path = PersonalUtils.getProfilePath(athorizable);
    String type = nodeTypeForAuthorizable(isGroup);
    if (session.itemExists(path)) {
      return (Node) session.getItem(path);
    }
    Node profileNode = JcrUtils.deepGetOrCreateNode(session, path);
    profileNode.setProperty("sling:resourceType", type);
    // Make sure we can place references to this profile node in the future.
    // This will make it easier to search on it later on.
    if (profileNode.canAddMixin(JcrConstants.MIX_REFERENCEABLE)) {
      profileNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
    }

    addEntry(profileNode.getParent().getPath(), athorizable.getPrincipal(), session,
        READ_GRANTED, WRITE_GRANTED, REMOVE_CHILD_NODES_GRANTED,
        MODIFY_PROPERTIES_GRANTED, ADD_CHILD_NODES_GRANTED, REMOVE_NODE_GRANTED,
        NODE_TYPE_MANAGEMENT_GRANTED);
    return profileNode;
  }

  private Node createPrivate(Session session, Authorizable athorizable)
      throws RepositoryException {
    String privatePathCreated = PersonalUtils.getPrivatePath(athorizable);
    if (session.itemExists(privatePathCreated)) {
      return (Node) session.getItem(privatePathCreated).getParent();
    }
    PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
    Principal anon = new Principal() {
      public String getName() {
        return UserConstants.ANON_USERID;
      }
    };
    Principal everyone = principalManager.getEveryone();

    Node createdNode = JcrUtils.deepGetOrCreateNode(session, privatePathCreated);
    createdNode.setProperty(UserConstants.JCR_CREATED_BY, athorizable.getID());
    Node privateNode = createdNode.getParent();
    String privateNodePath = privateNode.getPath();
    addEntry(privateNodePath, athorizable.getPrincipal(), session, READ_GRANTED,
        WRITE_GRANTED, REMOVE_CHILD_NODES_GRANTED, MODIFY_PROPERTIES_GRANTED,
        ADD_CHILD_NODES_GRANTED, REMOVE_NODE_GRANTED, NODE_TYPE_MANAGEMENT_GRANTED);
    // explicitly deny anon and everyone, this is private space.
    addEntry(privateNodePath, anon, session, READ_DENIED, WRITE_DENIED);
    addEntry(privateNodePath, everyone, session, READ_DENIED, WRITE_DENIED);
    return privateNode;
  }

  private void deleteProfileNode(Session session, Authorizable athorizable)
      throws RepositoryException {
    if (athorizable != null) {
      String path = PersonalUtils.getProfilePath(athorizable);
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
  private void fireEvent(SlingHttpServletRequest request, String principalName,
      List<Modification> changes) {
    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      String user = session.getUserID();
      for (Modification m : changes) {
        String path = m.getDestination();
        if (path == null) {
          path = m.getSource();
        }
        if (AuthorizableEventUtil.isAuthorizableModification(m)) {
          LOGGER.info("Got Authorizable modification: " + m);
          switch (m.getType()) {
          case COPY:
          case CREATE:
          case DELETE:
          case MOVE:
            LOGGER.debug("Ignoring unknown modification type: " + m.getType());
            break;
          case MODIFY:
            eventAdmin.postEvent(AuthorizableEventUtil.newGroupEvent(m));
            break;
          }
        } else if (path.endsWith(principalName)) {
          switch (m.getType()) {
          case COPY:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.update, user, principalName, m));
            break;
          case CREATE:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.create, user, principalName, m));
            break;
          case DELETE:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.delete, user, principalName, m));
            break;
          case MODIFY:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.update, user, principalName, m));
            break;
          case MOVE:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.update, user, principalName, m));
            break;
          }
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

}
