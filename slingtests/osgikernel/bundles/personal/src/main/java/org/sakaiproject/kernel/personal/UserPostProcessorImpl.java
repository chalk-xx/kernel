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
package org.sakaiproject.kernel.personal;

import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_GROUP_PATH;
import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_GROUP_PREFIX;
import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_USER_PATH;
import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_USER_PREFIX;
import static org.sakaiproject.kernel.util.ACLUtils.ADD_CHILD_NODES_GRANTED;
import static org.sakaiproject.kernel.util.ACLUtils.MODIFY_PROPERTIES_GRANTED;
import static org.sakaiproject.kernel.util.ACLUtils.REMOVE_CHILD_NODES_GRANTED;
import static org.sakaiproject.kernel.util.ACLUtils.REMOVE_NODE_GRANTED;
import static org.sakaiproject.kernel.util.ACLUtils.WRITE_GRANTED;
import static org.sakaiproject.kernel.util.ACLUtils.addEntry;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.user.AuthorizableEventUtil;
import org.sakaiproject.kernel.api.user.UserConstants;
import org.sakaiproject.kernel.api.user.UserPostProcessor;
import org.sakaiproject.kernel.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @scr.service interface="org.sakaiproject.kernel.api.user.UserPostProcessor"
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
  public void process(Session session, SlingHttpServletRequest request,
      List<Modification> changes) throws Exception {
    try {
      LOGGER.debug("Starting process with reques session {}", request
          .getResourceResolver().adaptTo(Session.class));
      String resourcePath = request.getRequestPathInfo().getResourcePath();
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable authorizable = null;
      String principalName = null;
      LOGGER.info("resourcePath: " + resourcePath);
      if (resourcePath.equals(SYSTEM_USER_MANAGER_USER_PATH)) {
        RequestParameter rpid = request
            .getRequestParameter(SlingPostConstants.RP_NODE_NAME);
        if (rpid != null) {
          principalName = rpid.getString();
          authorizable = userManager.getAuthorizable(principalName);
          if (authorizable != null) {
            Node profileNode = createProfile(session, authorizable);
            updateProperties(session, profileNode, authorizable, principalName, changes);
          }
          fireEvent(request, principalName, changes);
        }
      } else if (resourcePath.equals(SYSTEM_USER_MANAGER_GROUP_PATH)) {
        RequestParameter rpid = request
            .getRequestParameter(SlingPostConstants.RP_NODE_NAME);
        if (rpid != null) {
          principalName = rpid.getString();
          authorizable = userManager.getAuthorizable(principalName);
          if (authorizable != null) {
            Node profileNode = createProfile(session, authorizable);
            updateProperties(session, profileNode, authorizable, principalName, changes);
          }
          fireEvent(request, principalName, changes);
        }
      } else if (resourcePath.startsWith(SYSTEM_USER_MANAGER_USER_PREFIX)) {
        principalName = resourcePath.substring(SYSTEM_USER_MANAGER_USER_PREFIX.length());
        if (principalName.indexOf('/') != -1) {
          return;
        }
        authorizable = userManager.getAuthorizable(principalName);
        if (authorizable != null) {
          Node profileNode = createProfile(session, authorizable);
          updateProperties(session, profileNode, authorizable, principalName, changes);
        }
        fireEvent(request, principalName, changes);
      } else if (resourcePath.startsWith(SYSTEM_USER_MANAGER_GROUP_PREFIX)) {
        principalName = resourcePath.substring(SYSTEM_USER_MANAGER_GROUP_PREFIX.length());
        if (principalName.indexOf('/') != -1) {
          return;
        }
        authorizable = userManager.getAuthorizable(principalName);
        if (authorizable != null) {
          Node profileNode = createProfile(session, authorizable);
          updateProperties(session, profileNode, authorizable, principalName, changes);
        }
        fireEvent(request, principalName, changes);
      }
    } catch (Exception ex) {
      LOGGER.error("Post Processing failed " + ex.getMessage(), ex);
    }
  }

  /**
   * @param authorizable
   * @param principalName
   * @param changes
   * @throws RepositoryException
   * @throws ConstraintViolationException
   * @throws LockException
   * @throws VersionException
   * @throws PathNotFoundException
   */
  private void updateProperties(Session session, Node profileNode,
      Authorizable authorizable, String principalName, List<Modification> changes)
      throws RepositoryException {

    for (Modification m : changes) {
      String dest = m.getDestination();
      if (dest == null) {
        dest = m.getSource();
      }
      switch (m.getType()) {
      case DELETE:
        if (!dest.endsWith(principalName) && profileNode != null) {
          String propertyName = PathUtils.lastElement(dest);
          if (profileNode.hasProperty(propertyName)) {
            Property prop = profileNode.getProperty(propertyName);
            changes.add(Modification.onDeleted(prop.getPath()));
            prop.remove();
          }
        } else {
          deleteProfileNode(session, authorizable);
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
    if (authorizable != null) {
      Iterator<?> inames = authorizable.getPropertyNames();
      while (inames.hasNext()) {
        String propertyName = (String) inames.next();
        if (propertyName.equals("rep:userId") || !propertyName.startsWith("rep:") ) {
          if (!privateProperties.contains(propertyName)) {
            Value[] v = authorizable.getProperty(propertyName);
            if (!(profileNode.hasProperty(propertyName) && profileNode.getProperty(
                propertyName).getDefinition().isProtected())) {
              Property prop = profileNode.setProperty(propertyName, v);
              changes.add(Modification.onModified(prop.getPath()));
            }
          }
        } else {
         LOGGER.info("Not Updating "+propertyName); 
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
  private Node createProfile(Session session, Authorizable authorizable)
      throws RepositoryException {
    String path = PersonalUtils.getProfilePath(authorizable.getID());
    System.out.println("Getting/creating profile node: " + path);
    String type = nodeTypeForAuthorizable(authorizable);
    if (session.itemExists(path)) {
      return (Node) session.getItem(path);
    }
    String privatePath = PersonalUtils.getPrivatePath(authorizable.getID(), "created");
    if (!session.itemExists(privatePath)) {
      Node privateNode = JcrUtils.deepGetOrCreateNode(session, privatePath);
      privateNode.setProperty(UserConstants.JCR_CREATED_BY, authorizable.getID());
      addEntry(privateNode.getParent().getPath(), authorizable, session, WRITE_GRANTED,
          REMOVE_CHILD_NODES_GRANTED, MODIFY_PROPERTIES_GRANTED, ADD_CHILD_NODES_GRANTED,
          REMOVE_NODE_GRANTED);
    }
    Node profileNode = JcrUtils.deepGetOrCreateNode(session, path);
    profileNode.setProperty("sling:resourceType", type);
    addEntry(profileNode.getParent().getPath(), authorizable, session, WRITE_GRANTED,
        REMOVE_CHILD_NODES_GRANTED, MODIFY_PROPERTIES_GRANTED, ADD_CHILD_NODES_GRANTED,
        REMOVE_NODE_GRANTED);
    return profileNode;
  }

  private void deleteProfileNode(Session session, Authorizable authorizable)
      throws RepositoryException {
    String path = PersonalUtils.getProfilePath(authorizable.getID());
    if (session.itemExists(path)) {
      Node node = (Node) session.getItem(path);
      node.remove();
    }
  }

  private String nodeTypeForAuthorizable(Authorizable authorizable) {
    if (authorizable.isGroup()) {
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
        if (path.endsWith(principalName)) {
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
