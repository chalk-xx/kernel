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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.impl.post.DeleteAuthorizableServlet;
import org.apache.sling.servlets.post.Modification;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceExtension;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceParameter;
import org.sakaiproject.kernel.api.doc.ServiceResponse;
import org.sakaiproject.kernel.api.doc.ServiceSelector;
import org.sakaiproject.kernel.api.user.UserPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

/**
 * Sling Post Operation implementation for deleting one or more users and/or groups from the 
 * jackrabbit UserManager.

 * <h2>Rest Service Description</h2>
 * <p>
 * Deletes an Authorizable, currently a user or a group. Maps on to nodes of resourceType <code>sling/users</code> or <code>sling/users</code> like
 * <code>/rep:system/rep:userManager/rep:users</code> or <code>/rep:system/rep:userManager/rep:groups</code> mapped to a resource url
 * <code>/system/userManager/user</code> or <code>/system/userManager/group</code>. This servlet responds at
 * <code>/system/userManager/user.delete.html</code> or <code>/system/userManager/group.delete.html</code>.
 * The servlet also responds to single delete requests eg <code>/system/userManager/group/newGroup.delete.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>:applyTo</dt>
 * <dd>An array of relative resource references to Authorizables to be deleted, if this parameter is present, the url is ignored and all the Authorizables in the list are removed.</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, no body.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found</dd>
 * <dt>500</dt>
 * <dd>Failure</dd>
 * </dl>
 * <h4>Example</h4>
 * 
 * <code>
 * curl -Fgo=1 http://localhost:8080/system/userManager/user/ieb.delete.html
 * </code>
 *
 *
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sling/user" values.1="sling/group" values.2="sling/userManager"
 * @scr.property name="sling.servlet.methods" value="POST" 
 * @scr.property name="sling.servlet.selectors" value="delete" 
 * 
 * @scr.reference name="UserPostProcessor" bind="bindUserPostProcessor" unbind="unbindUserPostProcessor"
 *                interface="org.sakaiproject.kernel.api.user.UserPostProcessor"
 *                cardinality="0..n" policy="dynamic"
 *
 */
@ServiceDocumentation(name="Delete Authorizable (Group and User) Servlet",
    description="Deletes a group. Maps on to nodes of resourceType sling/groups like " +
    		"/rep:system/rep:userManager/rep:groups mapped to a resource url " +
    		"/system/userManager/group. This servlet responds at " +
    		"/system/userManager/group.delete.html. The servlet also responds to single delete " +
    		"requests eg /system/userManager/group/g-groupname.delete.html",
    shortDescription="Delete a group or user",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sling/group", "sling/user"},
        selectors=@ServiceSelector(name="delete", description="Deletes one or more authorizables (groups or users)"),
        extensions=@ServiceExtension(name="html", description="Posts produce html containing the update status")),
    methods=@ServiceMethod(name="POST",
        description={"Delete a group or user, or set of groups.",
            "Example<br>" +
            "<pre>curl -Fgo=1 http://localhost:8080/system/userManager/group/g-groupname.delete.html</pre>"},
        parameters={
        @ServiceParameter(name=":applyTo", description="An array of relative resource references to groups to be deleted, if this parameter is present, the url is ignored and all listed groups are removed.")
    },
    response={
    @ServiceResponse(code=200,description="Success, a redirect is sent to the group's resource locator with HTML describing status."),
    @ServiceResponse(code=404,description="Group or User was not found."),
    @ServiceResponse(code=500,description="Failure with HTML explanation.")
        })) 
public class DeleteSakaiAuthorizableServlet extends DeleteAuthorizableServlet {

  /**
   *
   */
  private static final long serialVersionUID = 3417673949322305891L;


  private static final Logger LOGGER = LoggerFactory.getLogger(DeleteAuthorizableServlet.class);

  
  private transient UserPostProcessorRegister postProcessorTracker = new UserPostProcessorRegister();

  /**
   * {@inheritDoc}
   * @see org.apache.sling.jackrabbit.usermanager.post.CreateUserServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void handleOperation(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {
    super.handleOperation(request, response, changes);
    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      for (UserPostProcessor userPostProcessor : postProcessorTracker.getProcessors()) {
        userPostProcessor.process(session, request, changes);
      }
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(),e);

      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
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
