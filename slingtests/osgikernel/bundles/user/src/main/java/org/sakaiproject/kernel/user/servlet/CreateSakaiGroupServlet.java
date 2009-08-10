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
package org.sakaiproject.kernel.user.servlet;

import static org.sakaiproject.kernel.api.user.UserConstants.DEFAULT_HASH_LEVELS;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.impl.helper.RequestProperty;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.user.UserPostProcessor;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Sling Post Servlet implementation for creating a group in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Creates a new group. Maps on to nodes of resourceType <code>sling/groups</code> like
 * <code>/rep:system/rep:userManager/rep:groups</code> mapped to a resource url
 * <code>/system/userManager/group</code>. This servlet responds at
 * <code>/system/userManager/group.create.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>:name</dt>
 * <dd>The name of the new group (required)</dd>
 * <dt></dt>
 * <dd>Any additional parameters become properties of the group node (optional)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the group resource locator. The redirect comes with
 * HTML describing the status.</dd>
 * <dt>500</dt>
 * <dd>Failure, including group already exists. HTML explains the failure.</dd>
 * </dl>
 * <h4>Example</h4>
 * 
 * <code>
 * curl -F:name=newGroupA  -Fproperty1=value1 http://localhost:8080/system/userManager/group.create.html
 * </code>
 * 
 * <h4>Notes</h4>
 * <p>
 * Groups are stored as JCR node in a 3 way hased tree, for example <code>/rep:system/rep:userManager/rep:groups/ab/3e/4d/newGroupA</code>
 * </p>
 * 
 * @scr.component immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/groups"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="create"
 * 
 * @scr.property name="servlet.post.dateFormats"
 *               values.0="EEE MMM dd yyyy HH:mm:ss 'GMT'Z"
 *               values.1="yyyy-MM-dd'T'HH:mm:ss.SSSZ" values.2="yyyy-MM-dd'T'HH:mm:ss"
 *               values.3="yyyy-MM-dd" values.4="dd.MM.yyyy HH:mm:ss"
 *               values.5="dd.MM.yyyy"
 * 
 * @scr.reference name="UserPostProcessor" bind="bindUserPostProcessor"
 *                unbind="unbindUserPostProcessor"
 *                interface="org.sakaiproject.kernel.api.user.UserPostProcessor"
 *                cardinality="0..n" policy="dynamic"
 * 
 */

public class CreateSakaiGroupServlet extends AbstractSakaiGroupPostServlet implements
    ManagedService {

  
  /**
   *
   */
  private static final long serialVersionUID = 6587376522316825454L;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CreateSakaiGroupServlet.class);
  

  private UserPostProcessorRegister postProcessorTracker = new UserPostProcessorRegister();

  /**
   * The JCR Repository we access to resolve resources
   * 
   * @scr.reference
   */
  protected SlingRepository repository;

  /**
   * 
   * @scr.property value="authenticated,everyone" type="String"
   *               name="Groups who are allowed to create other groups" description=
   *               "A comma separated list of groups who area allowed to create other groups"
   */
  public static final String GROUP_AUTHORISED_TOCREATE = "groups.authorized.tocreate";

  private String[] authorizedGroups = {"authenticated"};

  /** Returns the JCR repository used by this service. */
  protected SlingRepository getRepository() {
    return repository;
  }

  /**
   * Returns an administrative session to the default workspace.
   */
  private Session getSession() throws RepositoryException {
    return getRepository().loginAdministrative(null);
  }

  /**
   * Return the administrative session and close it.
   */
  private void ungetSession(final Session session) {
    if (session != null) {
      try {
        session.logout();
      } catch (Throwable t) {
        LOGGER.error("Unable to log out of session: " + t.getMessage(), t);
      }
    }
  }

  // ---------- SCR integration ---------------------------------------------

  /**
   * Activates this component.
   * 
   * @param componentContext
   *          The OSGi <code>ComponentContext</code> of this component.
   */
  protected void activate(ComponentContext componentContext) {
    super.activate(componentContext);
    postProcessorTracker.setComponentContext(componentContext);
    String groupList = (String) componentContext.getProperties().get(
        GROUP_AUTHORISED_TOCREATE);
    if (groupList != null) {
      authorizedGroups = StringUtils.split(groupList, ',');
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("unchecked")
  public void updated(Dictionary dictionary) throws ConfigurationException {
    String groupList = (String) dictionary.get(GROUP_AUTHORISED_TOCREATE);
    if (groupList != null) {
      authorizedGroups = StringUtils.split(groupList, ',');
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @seeorg.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet#
   * handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void handleOperation(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {

    // check that the submitted parameter values have valid values.
    final String principalName = request.getParameter(SlingPostConstants.RP_NODE_NAME);
    if (principalName == null) {
      throw new RepositoryException("Group name was not submitted");
    }

    if (!principalName.startsWith("g-")) {
      throw new RepositoryException("Group names must begin with 'g-'");
    }

    // check for allow create Group
    boolean allowCreateGroup = false;
    User currentUser = null;
    try {
      Session currentSession = request.getResourceResolver().adaptTo(Session.class);
      UserManager um = AccessControlUtil.getUserManager(currentSession);
      currentUser = (User) um.getAuthorizable(currentSession.getUserID());
      if (currentUser.isAdmin()) {
        LOGGER.debug("User is an admin ");
        allowCreateGroup = true;
      } else {
        LOGGER.debug("Checking for membership of one of {} ", Arrays
            .toString(authorizedGroups));
        PrincipalManager principalManager = AccessControlUtil
            .getPrincipalManager(currentSession);
        PrincipalIterator pi = principalManager.getGroupMembership(principalManager
            .getPrincipal(currentSession.getUserID()));
        Set<String> groups = new HashSet<String>();
        for (; pi.hasNext();) {
          groups.add(pi.nextPrincipal().getName());
        }

        for (String groupName : authorizedGroups) {
          if (groups.contains(groupName)) {
            allowCreateGroup = true;
            break;
          }

          // TODO: move this nasty hack into the PrincipalManager dynamic groups need to
          // be in the principal manager for this to work.
          if ("authenticated".equals(groupName)
              && !SecurityConstants.ADMIN_ID.equals(currentUser.getID())) {
            allowCreateGroup = true;
            break;
          }

          // just check via the user manager for dynamic resolution.
          Group group = (Group) um.getAuthorizable(groupName);
          LOGGER.debug("Checking for group  {} {} ", groupName, group);
          if (group != null && group.isMember(currentUser)) {
            allowCreateGroup = true;
            LOGGER.debug("User is a member  of {} {} ", groupName, group);
            break;
          }
        }
      }
    } catch (Exception ex) {
      LOGGER.warn("Failed to determin if the user is an admin, assuming not. Cause: "
          + ex.getMessage());
      allowCreateGroup = false;
    }

    if (!allowCreateGroup) {
      LOGGER.debug("User is not allowed to create groups ");
      response.setStatus(HttpServletResponse.SC_FORBIDDEN,
          "User is not allowed to create groups");
      return;
    }

    Session session = getSession();

    try {
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable authorizable = userManager.getAuthorizable(principalName);

      if (authorizable != null) {
        // principal already exists!
        throw new RepositoryException(
            "A principal already exists with the requested name: " + principalName);
      } else {
        Map<String, RequestProperty> reqProperties = collectContent(request, response);

        Group group = userManager.createGroup(new Principal() {
          public String getName() {
            return principalName;
          }
        }, PathUtils.getUserPrefix(principalName, DEFAULT_HASH_LEVELS));

        String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
            + group.getID();
        response.setPath(groupPath);
        response.setLocation(externalizePath(request, groupPath));
        response.setParentLocation(externalizePath(request,
            AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH));
        changes.add(Modification.onCreated(groupPath));

        // write content from form
        writeContent(session, group, reqProperties, changes);

        // update the group memberships, although this uses session from the request, it
        // only
        // does so for finding authorizables, so its ok that we are using an admin session
        // here.
        updateGroupMembership(session, request, group, changes);
        updateOwnership(session, request, group, new String[] {currentUser.getID()},
            changes);

        try {
          for (UserPostProcessor userPostProcessor : postProcessorTracker.getProcessors()) {
            userPostProcessor.process(session, request, changes);
          }
        } catch (Exception e) {
          LOGGER.warn(e.getMessage(), e);
          response
              .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
        if (session.hasPendingChanges()) {
          session.save();
        }

      }
    } catch (RepositoryException re) {
      throw new RepositoryException("Failed to create new group.", re);
    } finally {
      ungetSession(session);
    }
  }


  protected void bindUserPostProcessor(ServiceReference serviceReference) {
    postProcessorTracker.bindUserPostProcessor(serviceReference);

  }

  protected void unbindUserPostProcessor(ServiceReference serviceReference) {
    postProcessorTracker.unbindUserPostProcessor(serviceReference);
  }

}
