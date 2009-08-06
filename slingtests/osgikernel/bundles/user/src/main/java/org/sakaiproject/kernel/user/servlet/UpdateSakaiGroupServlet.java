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

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.impl.helper.RequestProperty;
import org.apache.sling.jackrabbit.usermanager.impl.post.UpdateGroupServlet;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.user.UserPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Sling Post Operation implementation for updating a group in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Updates a group's properties. Maps on to nodes of resourceType
 * <code>sling/groups</code> like
 * <code>/rep:system/rep:userManager/rep:groups/ae/3f/ed/testGroup</code> mapped to a
 * resource url <code>/system/userManager/group/testGroup</code>. This servlet responds at
 * <code>/system/userManager/group/testGroup.update.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt></dt>
 * <dd>Any additional parameters become properties of the group node (optional)</dd>
 * <dt>@Delete</dt>
 * <dd>The property is deleted, eg prop1@Delete</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the group's resource locator. The redirect comes
 * with HTML describing the status.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found</dd>
 * <dt>500</dt>
 * <dd>Failure</dd>
 * </dl>
 * <h4>Example</h4>
 * 
 * <code>
 * curl -Fprop1=value2 -Fproperty1=value1 http://localhost:8080/system/userManager/group/testGroup.update.html
 * </code>
 * 
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sling/group"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="update"
 * @scr.reference name="UserPostProcessor" bind="bindUserPostProcessor"
 *                unbind="unbindUserPostProcessor"
 *                interface="org.sakaiproject.kernel.api.user.UserPostProcessor"
 *                cardinality="0..n" policy="dynamic"
 * 
 */
public class UpdateSakaiGroupServlet extends UpdateGroupServlet {

  /**
   *
   */
  private static final long serialVersionUID = -2378929115784007976L;

  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateGroupServlet.class);

  private UserPostProcessorRegister postProcessorTracker = new UserPostProcessorRegister();

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jackrabbit.usermanager.post.CreateUserServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void handleOperation(SlingHttpServletRequest request, HtmlResponse htmlResponse,
      List<Modification> changes) throws RepositoryException {
    boolean modifyGroup = false;
    Principal authorizablePrincipal = null;
    
    { // check if the user can modify the group

      Authorizable authorizable = null;
      Resource resource = request.getResource();

      if (resource != null) {
        authorizable = resource.adaptTo(Authorizable.class);
      }

      // check that the group was located.
      if (authorizable == null) {
        throw new ResourceNotFoundException("Group to update could not be determined");
      }
      authorizablePrincipal = authorizable.getPrincipal();
      Session session = request.getResourceResolver().adaptTo(Session.class);
      if (session == null) {
        throw new RepositoryException("JCR Session not found");
      }
      UserManager userManager = AccessControlUtil.getUserManager(session);
      User currentUser = (User) userManager.getAuthorizable(session.getUserID());
      if (currentUser == null) {
        throw new RepositoryException("Cant locate the current user ");
      }
      if (currentUser.isAdmin()) {
        modifyGroup = true;
      } else {

        Set<String> userPrincipals = new HashSet<String>();
        for (PrincipalIterator pi = currentUser.getPrincipals(); pi.hasNext();) {
          userPrincipals.add(pi.nextPrincipal().getName());
        }
        if (authorizable.hasProperty(CreateSakaiGroupServlet.ADMIN_PRINCIPALS_PROPERTY)) {
          Value[] groups = authorizable
              .getProperty(CreateSakaiGroupServlet.ADMIN_PRINCIPALS_PROPERTY);
          for (Value group : groups) {
            String groupName = group.getString();
            if (userPrincipals.contains(groupName)) {
              modifyGroup = true;
              break;
            }
          }
        }
      }
    }
    if (!modifyGroup) {
      throw new RepositoryException("Not Allowed to modify the group ");
    }
    // switch to the admin
    Session session = getSession();
    try {
    UserManager userManager = AccessControlUtil.getUserManager(session);
    Authorizable authorizable = userManager.getAuthorizable(authorizablePrincipal);

    Map<String, RequestProperty> reqProperties = collectContent(request, htmlResponse);
    try {
      // cleanup any old content (@Delete parameters)
      processDeletes(authorizable, reqProperties, changes);

      // write content from form
      writeContent(session, authorizable, reqProperties, changes);

      // update the group memberships
      if (authorizable.isGroup()) {
        updateGroupMembership(request, authorizable, changes);
      }
    } catch (RepositoryException re) {
      throw new RepositoryException("Failed to update group.", re);
    }

    
    try {
      for (UserPostProcessor userPostProcessor : postProcessorTracker.getProcessors()) {
        userPostProcessor.process(session, request, changes);
      }
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);

      htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
    
    if ( session.hasPendingChanges() ) {
      session.save();
    }
    } finally {
      session.logout();
    }
  }

  /**
   * @return
   */
  private Session getSession() {
    // TODO Auto-generated method stub
    return null;
  }

  protected void bindUserPostProcessor(ServiceReference serviceReference) {
    postProcessorTracker.bindUserPostProcessor(serviceReference);

  }

  protected void unbindUserPostProcessor(ServiceReference serviceReference) {
    postProcessorTracker.unbindUserPostProcessor(serviceReference);
  }

  /**
   * Activates this component.
   * 
   * @param componentContext
   *          The OSGi <code>ComponentContext</code> of this component.
   */
  protected void activate(ComponentContext componentContext) {
    super.activate(componentContext);
    postProcessorTracker.setComponentContext(componentContext);
  }

}
