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
package org.sakaiproject.nakamura.user.servlet;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.impl.post.ChangeUserPasswordServlet;
import org.apache.sling.servlets.post.Modification;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Sling Post Operation implementation for updating the password of a user in the 
 * jackrabbit UserManager.
 * <p>
 * Changes the password associated with a user. a new group. Maps on to nodes of resourceType <code>sling/groups</code> like
 * <code>/rep:system/rep:userManager/rep:users/ae/fd/3e/ieb</code> mapped to a resource url
 * <code>/system/userManager/user/ieb</code>. This servlet responds at
 * <code>/system/userManager/user/ieb.changePassword.create.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>oldPwd</dt>
 * <dd>The current password for the user (required)</dd>
 * <dt>newPwd</dt>
 * <dd>The new password for the user (required)</dd>
 * <dt>newPwdConfirm</dt>
 * <dd>The confirm new password for the user (required)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Sucess sent with no body</dd>
 * <dt>404</dt>
 * <dd>If the user was not found.</dd>
 * <dt>500</dt>
 * <dd>Failure, including group already exists. HTML explains the failure.</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl -FoldPwd=oldpassword -FnewPwd=newpassword =FnewPwdConfirm=newpassword http://localhost:8080/system/userManager/user/ieb.changePassword.html
 * </code>
 *
 * <h4>Notes</h4>
 *
 * @disabled.scr.component metatype="no" immediate="true"
 * @disabled.scr.service interface="javax.servlet.Servlet"
 * @disabled.scr.property name="sling.servlet.resourceTypes" value="sling/user"
 * @disabled.scr.property name="sling.servlet.methods" value="POST"
 * @disabled.scr.property name="sling.servlet.selectors" value="changePassword"
 */
@ServiceDocumentation(name="Change Password Servlet",
    description="Changes user password. Maps on to nodes of resourceType sling/user " +
    		"like /rep:system/rep:userManager/rep:users/ae/fd/3e/username mapped to a resource " +
    		"url /system/userManager/user/username . This servlet responds at " +
    		"/system/userManager/user/username.changePassword.html",
    shortDescription="Change a user password",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sling/user"},
        selectors={@ServiceSelector(name="changePassword",description="selects this servlet to change the users password")
        }),
    methods=@ServiceMethod(name="POST",
        description={"Creates a new user with a name :name, and password pwd, " +
            "storing additional parameters as properties of the new user.",
            "Example<br><pre>curl -FoldPwd=oldpassword -FnewPwd=newpassword =FnewPwdConfirm=newpassword " +
            "http://localhost:8080/system/userManager/user/username.changePassword.html</pre>"},
        parameters={
        @ServiceParameter(name="oldPwd", description="current password for user (required)"),
        @ServiceParameter(name="newPwd", description="new password for user (required)"),
        @ServiceParameter(name="newPwdConfirm", description="confirm new password for user (required)")
        },
        response={
        @ServiceResponse(code=200,description="Sucess sent with no body."),
        @ServiceResponse(code=404,description="User was not found."),
        @ServiceResponse(code=500,description="Failure with HTML explanation.")
    }))
public class ChangeSakaiUserPasswordServlet extends ChangeUserPasswordServlet	 {

  /**
   *
   */
  private static final long serialVersionUID = 7178297046643735984L;

  /*
   * (non-Javadoc)
   * @see
   * org.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet
   * #handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void handleOperation(SlingHttpServletRequest request,
          HtmlResponse htmlResponse, List<Modification> changes)
          throws RepositoryException {
      Authorizable authorizable = null;
      Resource resource = request.getResource();
      if (resource != null) {
          authorizable = resource.adaptTo(Authorizable.class);
      }

      // check that the user was located.
      if (authorizable == null || authorizable.isGroup()) {
          throw new ResourceNotFoundException(
              "User to update could not be determined.");
      }

      if ("anonymous".equals(authorizable.getID())) {
          throw new RepositoryException(
              "Can not change the password of the anonymous user.");
      }

      Session session = request.getResourceResolver().adaptTo(Session.class);
      if (session == null) {
          throw new RepositoryException("JCR Session not found");
      }

      // check that the submitted parameter values have valid values.
      String oldPwd = request.getParameter("oldPwd");
      if (oldPwd == null || oldPwd.length() == 0) {
          throw new RepositoryException("Old Password was not submitted");
      }
      String newPwd = request.getParameter("newPwd");
      if (newPwd == null || newPwd.length() == 0) {
          throw new RepositoryException("New Password was not submitted");
      }
      String newPwdConfirm = request.getParameter("newPwdConfirm");
      if (!newPwd.equals(newPwdConfirm)) {
          throw new RepositoryException(
              "New Password does not match the confirmation password");
      }


      try {
    	  // this is ugly by the JR apis are not able to validate a password at a lower level and the digest 
    	  // of the password is an out of bound agreement. :oldpassword is checked on changePassword and then 
    	  // cleared.
    	  authorizable.setProperty(":oldpassword", session.getValueFactory().createValue(digestPassword(oldPwd)));
          ((User) authorizable).changePassword(digestPassword(newPwd));

          changes.add(Modification.onModified(resource.getPath()
              + "/rep:password"));
      } catch (RepositoryException re) {
          throw new RepositoryException("Failed to change user password.", re);
      }
  }
  
  @Override
  protected void activate(ComponentContext context) {
	  super.activate(context);
  }
  
  @Override
	protected void deactivate(ComponentContext context) {
		super.deactivate(context);
	}

}
