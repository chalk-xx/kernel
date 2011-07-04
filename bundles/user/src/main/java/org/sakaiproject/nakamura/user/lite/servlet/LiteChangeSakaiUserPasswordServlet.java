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
package org.sakaiproject.nakamura.user.lite.servlet;


import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;

import java.util.List;

/**
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
 *
 */
@SlingServlet(resourceTypes={"sparse/user"}, methods={"POST"},selectors={"changePassword"})
@Properties(value = {
    @Property(name = "password.digest.algorithm", value = "sha1"),
    @Property(name = "servlet.post.dateFormats", value = {
        "EEE MMM dd yyyy HH:mm:ss 'GMT'Z", "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd", "dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy" })})

@ServiceDocumentation(name="Change Password Servlet", okForVersion = "0.11",
    description="Changes user password. Maps on to nodes of resourceType sparse/user " +
        "like /~username mapped to a resource " +
        "url /system/userManager/user/username . This servlet responds at " +
        "/system/userManager/user/username.changePassword.html",
    shortDescription="Change a user password",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sparse/user"},
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
        @ServiceResponse(code=200,description="Success sent with no body."),
        @ServiceResponse(code=404,description="User was not found."),
        @ServiceResponse(code=500,description="Failure with HTML explanation.")
    }))

public class LiteChangeSakaiUserPasswordServlet extends LiteAbstractUserPostServlet {
    private static final long serialVersionUID = 1923614318474654502L;

    /*
     * (non-Javadoc)
     * @see
     * org.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet
     * #handleOperation(org.apache.sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
     */
    @Override
    protected void handleOperation(SlingHttpServletRequest request,
            HtmlResponse htmlResponse, List<Modification> changes) throws StorageClientException, IllegalArgumentException, AccessDeniedException {
        Authorizable authorizable = null;
        Resource resource = request.getResource();
        if (resource != null) {
            authorizable = resource.adaptTo(Authorizable.class);
        }

        // check that the user was located.
        if (authorizable == null || (authorizable instanceof Group)) {
            throw new ResourceNotFoundException(
                "User to update could not be determined.");
        }

        if (User.ANON_USER.equals(authorizable.getId())) {
            throw new IllegalArgumentException(
                "Can not change the password of the anonymous user.");
        }

        Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
        if (session == null) {
            throw new StorageClientException("Sparse Session not found");
        }

        // check that the submitted parameter values have valid values.
        String oldPwd = request.getParameter("oldPwd");
        if (oldPwd == null || oldPwd.length() == 0) {
            throw new IllegalArgumentException("Old Password was not submitted");
        }
        String newPwd = request.getParameter("newPwd");
        if (newPwd == null || newPwd.length() == 0) {
            throw new IllegalArgumentException("New Password was not submitted");
        }
        String newPwdConfirm = request.getParameter("newPwdConfirm");
        if (!newPwd.equals(newPwdConfirm)) {
            throw new IllegalArgumentException(
                "New Password does not match the confirmation password");
        }


          AuthorizableManager authorizableManager = session.getAuthorizableManager();
          authorizableManager.changePassword(authorizable, digestPassword(newPwd), digestPassword(oldPwd));
            changes.add(Modification.onModified(resource.getPath()
                + "/password"));
    }
    

}
