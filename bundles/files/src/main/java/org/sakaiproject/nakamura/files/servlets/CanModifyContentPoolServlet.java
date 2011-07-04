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
package org.sakaiproject.nakamura.files.servlets;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permission;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = { "GET" }, selectors = { "canModify" }, extensions = { "json" }, resourceTypes = { "sakai/pooled-content" })
@ServiceDocumentation(name = "CanModifyContentPoolServlet documentation", okForVersion = "0.11",
  shortDescription = "Check to see if user has privileges to modify a pooled content item.",
  description = "Check to see if user has privileges to modify a pooled content item.",
  bindings = @ServiceBinding(type = BindingType.TYPE, bindings = "sakai/pooled-content",
    selectors = @ServiceSelector(name = "canModify", description = "Check to see if user has privileges to modify a pooled content item."),
    extensions = { @ServiceExtension(name = "json") }),
  methods = {
    @ServiceMethod(name = "GET", description = "Check to see if user has privileges to modify a resource.",
      parameters = {
        @ServiceParameter(name = "verbose", description = "Optional: set to true if you want verbose output")
      },
      response = {
        @ServiceResponse(code = HttpServletResponse.SC_OK, description = "Request has been processed successfully."),
        @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Resource could not be found."),
        @ServiceResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to process request due to a runtime error.")
      })
})
public class CanModifyContentPoolServlet extends SlingSafeMethodsServlet {
  private static final long serialVersionUID = 9008556018380828590L;
  private static final Logger LOG = LoggerFactory.getLogger(CanModifyContentPoolServlet.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    LOG.debug("doGet(SlingHttpServletRequest {}, SlingHttpServletResponse {})", request,
        response);

    boolean verbose = false;
    if (request.getRequestParameter("verbose") != null) {
      verbose = Boolean.valueOf(request.getRequestParameter("verbose").getString());
    }

    try {
      Resource resource = request.getResource();
      Content node = resource.adaptTo(Content.class);
      
      if (node != null) {
        Session session = resource.adaptTo(Session.class);
        AccessControlManager accessControlManager = session.getAccessControlManager();
        AuthorizableManager authorizableManager = session.getAuthorizableManager();
        Authorizable thisUser = authorizableManager.findAuthorizable(session.getUserId());
        
        String path = node.getPath();
        boolean canModifyNode = accessControlManager.can(thisUser, Security.ZONE_CONTENT, path, Permissions.CAN_WRITE);

        response.setContentType("application/json");
        final ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
        final List<String> selectors = Arrays.asList(request.getRequestPathInfo()
            .getSelectors());
        writer.setTidy(selectors.contains("tidy"));
        writer.object(); // root object
        writer.key(request.getRequestPathInfo().getResourcePath());
        writer.value(canModifyNode);
        if (verbose) {
          writer.key("privileges");
          Map<String, Object> privileges = new HashMap<String, Object>();
          Permission[] userPrivs = accessControlManager.getPermissions(Security.ZONE_CONTENT, path);
          if (userPrivs != null && userPrivs.length > 0) {
            for (Permission permission : userPrivs) {
              privileges.put(permission.getName(), true);
            }
          }
          writer.valueMap(privileges);
        }
        writer.endObject(); // root object
        response.setStatus(HttpServletResponse.SC_OK);
        return;
      } else {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, request.getRequestPathInfo()
            .getResourcePath() + " could not be found!");
        return;
      }
    } catch (Throwable e) {
      LOG.error(e.getLocalizedMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          e.getLocalizedMessage());
    }
  }


}
