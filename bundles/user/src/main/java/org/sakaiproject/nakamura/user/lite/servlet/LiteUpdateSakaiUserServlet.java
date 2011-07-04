/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.user.lite.servlet;

import com.google.common.collect.Maps;

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
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.resource.RequestProperty;
import org.sakaiproject.nakamura.user.lite.resource.LiteAuthorizableResourceProvider;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Sling Post Operation implementation for updating a user in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Updates a users properties. Maps on to nodes of resourceType <code>sling/users</code> like
 * <code>/rep:system/rep:userManager/rep:users</code> mapped to a resource url
 * <code>/system/userManager/user/ieb</code>. This servlet responds at
 * <code>/system/userManager/user/ieb.update.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the user node (optional)</dd>
 * <dt>*@Delete</dt>
 * <dd>Delete the property eg prop3@Delete means prop3 will be deleted (optional)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the users resource locator. The redirect comes with
 * HTML describing the status.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found</dd>
 * <dt>500</dt>
 * <dd>Failure</dd>
 * </dl>
 * <h4>Example</h4>
 * 
 * <code>
 * curl -Fprop1=value2 -Fproperty1=value1 http://localhost:8080/system/userManager/user/ieb.update.html
 * </code>
 * 
 *
 */
@ServiceDocumentation(name="Update User Servlet", okForVersion = "0.11",
    description="Updates a user's properties. Maps on to nodes of resourceType sparse/user " +
    		"like /~suzy " +
    		"/system/userManager/user/suzy . This servlet responds at " +
    		"/system/userManager/user/suzy.update.html",
    shortDescription="Update a user properties",
    bindings=@ServiceBinding(type= BindingType.TYPE,bindings={"sparse/user"},
        selectors=@ServiceSelector(name="update", description="Updates the properties of a user"),
        extensions=@ServiceExtension(name="html", description="Posts produce html containing the update status")),
    methods=@ServiceMethod(name="POST",
        description={"Updates a user setting or deleting properties, " +
            "storing additional parameters as properties of the new user.",
            "Example<br>" +
            "<pre>curl -Fproperty1@Delete -Fproperty2=value2 http://localhost:8080/system/userManager/user/suzy.update.html</pre>"},
        parameters={
        @ServiceParameter(name="propertyName@Delete", description="Delete property, eg property1@Delete means delete property1 (optional)"),
        @ServiceParameter(name="",description="Additional parameters become user node properties, " +
            "except for parameters starting with ':', which are only forwarded to post-processors (optional)")
        },
        response={
          @ServiceResponse(code=200,description="Success, a redirect is sent to the user's resource locator with HTML describing status."),
          @ServiceResponse(code=404,description="User was not found."),
          @ServiceResponse(code=500,description="Failure with HTML explanation.")}
        ))
@SlingServlet(resourceTypes={"sparse/user"}, methods={"POST"}, selectors={"update"})
@Properties(value = {
    @Property(name = "password.digest.algorithm", value = "sha1"),
    @Property(name = "servlet.post.dateFormats", value = {
        "EEE MMM dd yyyy HH:mm:ss 'GMT'Z", "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd", "dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy" })})
public class LiteUpdateSakaiUserServlet extends LiteAbstractUserPostServlet {

    /**
   * 
   */
  private static final long serialVersionUID = -4631985372236273994L;

    /*
     * (non-Javadoc)
     * @see
     * org.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet
     * #handleOperation(org.apache.sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
     */
    @Override
    protected void handleOperation(SlingHttpServletRequest request,
            HtmlResponse htmlResponse, List<Modification> changes) throws StorageClientException, AccessDeniedException {
        Authorizable authorizable = null;
        Resource resource = request.getResource();
        if (resource != null) {
            authorizable = resource.adaptTo(Authorizable.class);
        }

        // check that the group was located.
        if (authorizable == null) {
            throw new ResourceNotFoundException(
                "User to update could not be determined");
        }

        Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
        if (session == null) {
            throw new StorageClientException("Sparse Session not found");
        }
        
        String userPath = LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
            + authorizable.getId();

        Map<String, RequestProperty> reqProperties = collectContent(request,
            htmlResponse, userPath);
        Map<String, Object> toSave = Maps.newLinkedHashMap();
        // cleanup any old content (@Delete parameters)
        processDeletes(authorizable, reqProperties, changes, toSave);

        // write content from form
        writeContent(session, authorizable, reqProperties, changes, toSave);
        
        saveAll(session, toSave);

    }
}
