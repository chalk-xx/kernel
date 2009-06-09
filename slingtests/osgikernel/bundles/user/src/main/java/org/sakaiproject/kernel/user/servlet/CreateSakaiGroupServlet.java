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

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.post.AbstractGroupPostServlet;
import org.apache.sling.jackrabbit.usermanager.post.impl.RequestProperty;
import org.apache.sling.jackrabbit.usermanager.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.user.UserPostProcessor;
import org.sakaiproject.kernel.util.PathUtils;

import java.security.Principal;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

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
 * @scr.reference name="UserPostProcessor" bind="bindUserPostProcessor"
 *                unbind="unbindUserPostProcessor"
 *                interface="org.sakaiproject.kernel.user.UserPostProcessor"
 */

public class CreateSakaiGroupServlet extends AbstractGroupPostServlet {

  /**
   *
   */
  private static final long serialVersionUID = 6587376522316825454L;
  /**
   * @scr.property label="%usermanager.hashlevels.name"
   *               description="%usermanager.hashlevels.description"
   *               valueRef="DEFAULT_HASH_LEVELS"
   */
  private static final String PROP_HASH_LEVELS = "usermanager.hashlevels";
  private static final int DEFAULT_HASH_LEVELS = 4;
  private UserPostProcessor userPostProcessor;
  private int hashLevels = DEFAULT_HASH_LEVELS;

  /**
   * Activates this component.
   * 
   * @param componentContext
   *          The OSGi <code>ComponentContext</code> of this component.
   */
  protected void activate(ComponentContext componentContext) {
    super.activate(componentContext);
    Dictionary<?, ?> props = componentContext.getProperties();
    Object propValue = props.get(PROP_HASH_LEVELS);
    if (propValue instanceof String) {
      hashLevels = Integer.parseInt((String) propValue);
    } else {
      hashLevels = DEFAULT_HASH_LEVELS;
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

    Session session = request.getResourceResolver().adaptTo(Session.class);
    if (session == null) {
      throw new RepositoryException("JCR Session not found");
    }

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
        }, PathUtils.getUserPrefix(principalName, hashLevels));

        String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
            + group.getID();
        response.setPath(groupPath);
        response.setLocation(externalizePath(request, groupPath));
        response.setParentLocation(externalizePath(request,
            AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH));
        changes.add(Modification.onCreated(groupPath));

        // write content from form
        writeContent(session, group, reqProperties, changes);

        // update the group memberships
        updateGroupMembership(request, group, changes);
      }
    } catch (RepositoryException re) {
      throw new RepositoryException("Failed to create new group.", re);
    }
    try {
      userPostProcessor.process(request, changes);
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * @param userPostProcessor
   *          the userPostProcessor to set
   */
  protected void bindUserPostProcessor(UserPostProcessor userPostProcessor) {
    this.userPostProcessor = userPostProcessor;
  }

  /**
   * @param userPostProcessor
   *          the userPostProcessor to set
   */
  protected void unbindUserPostProcessor(UserPostProcessor userPostProcessor) {
    this.userPostProcessor = null;
  }

}
