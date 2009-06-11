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
package org.sakaiproject.kernel.user;

import static org.sakaiproject.kernel.api.personal.PersonalConstants._GROUP_PUBLIC;
import static org.sakaiproject.kernel.api.personal.PersonalConstants._USER_PUBLIC;
import static org.sakaiproject.kernel.api.user.UserConstants.AUTH_PROFILE;
import static org.sakaiproject.kernel.api.user.UserConstants.PRIVATE_PROPERTIES;
import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_GROUP_PATH;
import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_GROUP_PREFIX;
import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_USER_PATH;
import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_USER_PREFIX;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.kernel.api.user.AuthorizableEventUtil;
import org.sakaiproject.kernel.api.user.UserConstants;
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
 * @scr.service interface="org.sakaiproject.kernel.user.UserPostProcessor"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.component immediate="true" label="SitePostProcessor"
 *                description="Post Processor for User and Group operations" metatype="no"
 * @scr.property name="service.description"
 *               value="Post Processes User and Group operations"
 * @scr.reference interface="org.apache.sling.jcr.api.SlingRepository"
 *                name="SlingRepository" bind="bindSlingRepository"
 *                unbind="unbindSlingRepository"
 * @scr.reference name="EventAdmin" bind="bindEventAdmin" unbind="unbindEventAdmin"
 *                interface="org.osgi.service.event.EventAdmin"
 * 
 */
public class UserPostProcessorImpl implements UserPostProcessor  {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserPostProcessorImpl.class);

  /**
   */
  private EventAdmin eventAdmin;

  /**
   * The JCR Repository we access to update profile.
   * 
   */
  private SlingRepository slingRepository;

  /**
   * @param request
   * @param changes
   * @throws Exception
   */
  public void process(SlingHttpServletRequest request, List<Modification> changes)
      throws Exception {
    String resourcePath = request.getRequestPathInfo().getResourcePath();
    UserManager userManager = AccessControlUtil.getUserManager(request.getResourceResolver().adaptTo(Session.class));
    Authorizable authorizable = null;
    String principalName = null;
    if (resourcePath.equals(SYSTEM_USER_MANAGER_USER_PATH)) {
      RequestParameter rpid = request
          .getRequestParameter(SlingPostConstants.RP_NODE_NAME);
      if (rpid != null) {
        principalName = rpid.getString();
        authorizable = userManager.getAuthorizable(principalName);
        updateProperties(authorizable, principalName, false, changes);
      }
    } else if (resourcePath.equals(SYSTEM_USER_MANAGER_GROUP_PATH)) {
      RequestParameter rpid = request
          .getRequestParameter(SlingPostConstants.RP_NODE_NAME);
      if (rpid != null) {
        principalName = rpid.getString();
        authorizable = userManager.getAuthorizable(principalName);
        updateProperties(authorizable, principalName, true, changes);
      }
    } else if (resourcePath.startsWith(SYSTEM_USER_MANAGER_USER_PREFIX)) {
      principalName = resourcePath.substring(SYSTEM_USER_MANAGER_USER_PREFIX.length());
      if (principalName.indexOf('/') != -1) {
        return;
      }
      authorizable = userManager.getAuthorizable(principalName);
      updateProperties(authorizable, principalName,false,  changes);
    } else if (resourcePath.startsWith(SYSTEM_USER_MANAGER_GROUP_PREFIX)) {
      principalName = resourcePath.substring(SYSTEM_USER_MANAGER_GROUP_PREFIX.length());
      if (principalName.indexOf('/') != -1) {
        return;
      }
      authorizable = userManager.getAuthorizable(principalName);
      updateProperties(authorizable, principalName, true, changes);
    }
    fireEvent(request, principalName, changes);

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
  private void updateProperties(Authorizable authorizable, String principalName, boolean isGroup, List<Modification> changes)
      throws PathNotFoundException, VersionException, LockException,
      ConstraintViolationException, RepositoryException {
    Session session = slingRepository.loginAdministrative(null);
    try {
      Node profileNode = null;
      Iterator<?> inames = null;
      if (authorizable != null) {
        profileNode = createProfileNode(session, authorizable);
        inames = authorizable.getPropertyNames();
      }
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
            deleteProfileNode(session, principalName, isGroup);
          }
          break;
        }
      }
      
      if (profileNode == null) {
        return;
      }
      
      // build a blacklist set of properties that should be kept private
      Set<String> privateProperties = new HashSet<String>();
      if (profileNode.hasProperty(PRIVATE_PROPERTIES)) {
        Value[] pp = profileNode.getProperty(PRIVATE_PROPERTIES).getValues();
        for (Value v : pp) {
          privateProperties.add(v.getString());
        }
      }
      // copy the non blacklist set of properties into the users profile.
      while (inames.hasNext()) {
        String propertyName = (String) inames.next();
        if (!privateProperties.contains(propertyName)) {
          Value[] v = authorizable.getProperty(propertyName);
          if (!(profileNode.hasProperty(propertyName) && profileNode.getProperty(propertyName)
              .getDefinition().isProtected())) {
            Property prop = profileNode.setProperty(propertyName, v);
            changes.add(Modification.onModified(prop.getPath()));
          }
        }
      }
    } finally {
      if (session.hasPendingChanges()) {
        session.save();
      }
      session.logout();
    }
  }

  /**
   * @param request
   * @param authorizable
   * @return
   * @throws RepositoryException
   */
  private Node createProfileNode(Session session, Authorizable authorizable)
      throws RepositoryException {
    String path = profileNodeForAuthorizable(authorizable);
    String type = nodeTypeForAuthorizable(authorizable);
    if (session.itemExists(path)) {
      return (Node) session.getItem(path);
    }
    Node profileNode = JcrUtils.deepGetOrCreateNode(session, path);
    profileNode.setProperty("sling:resourceType", type);
    return profileNode;
  }

  private void deleteProfileNode(Session session, String principalName, boolean isGroup)
      throws RepositoryException {
    String path = profileNodeForAuthorizable(principalName, isGroup);
    if (session.itemExists(path)) {
      Node node = (Node)session.getItem(path);
      node.remove();
    }
  }

  private static String nodeTypeForAuthorizable(Authorizable authorizable) {
    if (authorizable.isGroup()) {
      return UserConstants.GROUP_PROFILE_RESOURCE_TYPE;
    } else {
      return UserConstants.USER_PROFILE_RESOURCE_TYPE;
    }
  }

  public static String profileNodeForAuthorizable(String principalName, boolean isGroup) throws RepositoryException
  {
    if (isGroup) {
      return PathUtils.toInternalHashedPath(_GROUP_PUBLIC, principalName,
          AUTH_PROFILE);
    } else {
      return PathUtils.toInternalHashedPath(_USER_PUBLIC, principalName,
          AUTH_PROFILE);
    }
  }

  public static String profileNodeForAuthorizable(Authorizable authorizable) throws RepositoryException
  {
    return profileNodeForAuthorizable(authorizable.getID(), authorizable.isGroup());
  }
  
  /**
   * @param slingRepository
   *          the slingRepository to set
   */
  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  /**
   * @param slingRepository
   *          the slingRepository to set
   */
  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
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
