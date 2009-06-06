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
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.kernel.api.user.AuthorizableEventUtil;
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
 * @scr.service interface="org.apache.sling.servlets.post.SlingPostProcessor"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.component immediate="true" label="SitePostProcessor"
 *                description="Post Processor for User and Group operations" metatype="no"
 * @scr.property name="service.description"
 *               value="Post Processes User and Group operations"
 * @scr.reference interface="org.apache.sling.jcr.api.SlingRepository"
 *                name="SlingRepository" bind="bindSlingRepository"
 *                unbind="unbindSlingRepository"
 * @scr.reference bind="bindEventAdmin" unbind="bindEventAdmin"
 *                interface="org.osgi.service.event.EventAdmin"
 * 
 */
public class UserPostProcessor implements SlingPostProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserPostProcessor.class);

  /**
   */
  private EventAdmin eventAdmin;

  /**
   * The JCR Repository we access to update profile.
   * 
   */
  private SlingRepository slingRepository;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.servlets.post.SlingPostProcessor#process(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.List)
   */
  public void process(SlingHttpServletRequest request, List<Modification> changes)
      throws Exception {
    String resourcePath = request.getRequestPathInfo().getResourcePath();
    UserManager userManager = AccessControlUtil.getUserManager(request.getResourceResolver().adaptTo(Session.class));
    Authorizable authorizable = null;
    if (resourcePath.equals(SYSTEM_USER_MANAGER_USER_PATH)) {
      RequestParameter rpid = request
          .getRequestParameter(SlingPostConstants.RP_NODE_NAME);
      if (rpid != null) {
        authorizable = userManager.getAuthorizable(rpid.getString());
        updateProperties(authorizable, changes);
      }
    } else if (resourcePath.equals(SYSTEM_USER_MANAGER_GROUP_PATH)) {
      RequestParameter rpid = request
          .getRequestParameter(SlingPostConstants.RP_NODE_NAME);
      if (rpid != null) {
        authorizable = userManager.getAuthorizable(rpid.getString());
        updateProperties(authorizable, changes);
      }
    } else if (resourcePath.startsWith(SYSTEM_USER_MANAGER_USER_PREFIX)) {
      String pid = resourcePath.substring(SYSTEM_USER_MANAGER_USER_PREFIX.length());
      if (pid.indexOf('/') != -1) {
        return;
      }
      authorizable = userManager.getAuthorizable(pid);
      updateProperties(authorizable, changes);
    } else if (resourcePath.startsWith(SYSTEM_USER_MANAGER_GROUP_PREFIX)) {
      String pid = resourcePath.substring(SYSTEM_USER_MANAGER_GROUP_PREFIX.length());
      if (pid.indexOf('/') != -1) {
        return;
      }
      authorizable = userManager.getAuthorizable(pid);
      updateProperties(authorizable, changes);
    }
    if (authorizable != null) {
      fireEvent(request, authorizable, changes);
    }

  }

  /**
   * @param request
   * @param user
   * @param changes
   * @throws RepositoryException
   * @throws ConstraintViolationException
   * @throws LockException
   * @throws VersionException
   * @throws PathNotFoundException
   */
  private void updateProperties(Authorizable authorizable, List<Modification> changes)
      throws PathNotFoundException, VersionException, LockException,
      ConstraintViolationException, RepositoryException {
    Session session = slingRepository.loginAdministrative(null);
    try {
      Node profileNode = createProfileNode(session, authorizable);
      Iterator<?> inames = authorizable.getPropertyNames();
      String id = authorizable.getID();
      for (Modification m : changes) {
        String dest = m.getDestination();
        switch (m.getType()) {
        case DELETE:
          if (!dest.endsWith(id)) {
            String propertyName = PathUtils.lastElement(dest);
            if (profileNode.hasProperty(propertyName)) {
              Property prop = profileNode.getProperty(propertyName);
              changes.add(Modification.onDeleted(prop.getPath()));
              prop.remove();
            }
          }
          break;
        }
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
          Property prop = profileNode.setProperty(propertyName, v);
          changes.add(Modification.onModified(prop.getPath()));
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
    String path = null;
    if (authorizable.isGroup()) {
      path = PathUtils.toInternalHashedPath(_GROUP_PUBLIC, authorizable.getID(),
          AUTH_PROFILE);
    } else {
      path = PathUtils.toInternalHashedPath(_USER_PUBLIC, authorizable.getID(),
          AUTH_PROFILE);
    }
    if (session.itemExists(path)) {
      return (Node) session.getItem(path);
    }
    return JcrUtils.deepGetOrCreateNode(session, path);
  }

  /**
   * @param slingRepository
   *          the slingRepository to set
   */
  public void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  /**
   * @param slingRepository
   *          the slingRepository to set
   */
  public void unbindSlingRepository(SlingRepository slingRepository) {
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
  private void fireEvent(SlingHttpServletRequest request, Authorizable authorizable,
      List<Modification> changes) {
    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      String user = session.getUserID();
      for (Modification m : changes) {
        String path = m.getDestination();
        if (path.endsWith(authorizable.getID())) {
          switch (m.getType()) {
          case COPY:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.update, user, authorizable, m));
            break;
          case CREATE:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.create, user, authorizable, m));
            break;
          case DELETE:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.delete, user, authorizable, m));
            break;
          case MODIFY:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.update, user, authorizable, m));
            break;
          case MOVE:
            eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(
                Operation.update, user, authorizable, m));
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
