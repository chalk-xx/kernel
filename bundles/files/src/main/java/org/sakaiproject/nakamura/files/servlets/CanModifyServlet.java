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
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.files.FileUtils;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "CanModifyServlet", shortDescription = "Check to see if user has privileges to modify a resource.", description = "Check to see if user has privileges to modify a resource.", bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/system/resource/canModify", extensions = { @ServiceExtension(name = "json", description = "This is the default return type if none is specified.") }), methods = {
    @ServiceMethod(name = "GET", description = "Check to see if user has privileges to modify a resource.", parameters = {
        @ServiceParameter(name = "key", description = "Required: Can be either 1) A fully qualified path, 2) UUID, or 3) a content poolId."),
        @ServiceParameter(name = "verbose", description = "Optional: set to true if you want verbose output") }, response = {
        @ServiceResponse(code = HttpServletResponse.SC_OK, description = "Request has been processed successfully."),
        @ServiceResponse(code = HttpServletResponse.SC_BAD_REQUEST, description = "Missing required parameters."),
        @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Resource could not be found."),
        @ServiceResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to process request due to a runtime error.") }),
    @ServiceMethod(name = "POST", description = "Check to see if user has privileges to modify a resource.", parameters = {
        @ServiceParameter(name = "key", description = "Required: Can be either 1) A fully qualified path, 2) UUID, or 3) a content poolId."),
        @ServiceParameter(name = "verbose", description = "Optional: set to true if you want verbose output") }, response = {
        @ServiceResponse(code = HttpServletResponse.SC_OK, description = "Request has been processed successfully."),
        @ServiceResponse(code = HttpServletResponse.SC_BAD_REQUEST, description = "Missing required parameters."),
        @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Resource could not be found."),
        @ServiceResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to process request due to a runtime error.") }) })
@SlingServlet(methods = { "GET", "POST" }, paths = { "/system/resource/canModify" }, extensions = { "json" })
public class CanModifyServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = 9008556018380828590L;
  private static final Logger LOG = LoggerFactory.getLogger(CanModifyServlet.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    if (request.getRequestParameter("key") == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "key cannot be null!");
      return;
    }
    boolean verbose = false;
    if (request.getRequestParameter("verbose") != null) {
      verbose = Boolean.valueOf(request.getRequestParameter("verbose").getString());
    }

    final String key = request.getRequestParameter("key").getString();
    final Session session = request.getResourceResolver().adaptTo(Session.class);
    try {
      final Node node = FileUtils.resolveNode(key, session);
      if (node != null) {
        final String path = node.getPath();
        final AccessControlManager accessControlManager = AccessControlUtil
            .getAccessControlManager(session);
        final Privilege[] modifyProperties = {
            accessControlManager.privilegeFromName(Privilege.JCR_READ),
            accessControlManager.privilegeFromName(Privilege.JCR_WRITE),
            accessControlManager.privilegeFromName(Privilege.JCR_REMOVE_NODE) };
        final boolean canModifyNode = accessControlManager.hasPrivileges(path,
            modifyProperties);

        response.setContentType("application/json");
        final ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
        final List<String> selectors = Arrays.asList(request.getRequestPathInfo()
            .getSelectors());
        writer.setTidy(selectors.contains("tidy"));
        writer.object(); // root object
        writer.key(key);
        writer.value(canModifyNode);
        if (verbose) {
          writer.key("privileges");
          final Map<String, Object> privileges = new HashMap<String, Object>();
          final Privilege[] userPrivs = accessControlManager.getPrivileges(path);
          if (userPrivs != null && userPrivs.length > 0) {
            for (final Privilege privilege : userPrivs) {
              privileges.put(privilege.getName(), true);
            }
          }
          writer.valueMap(privileges);
        }
        writer.endObject(); // root object
        response.setStatus(HttpServletResponse.SC_OK);
        return;
      } else {
        response
            .sendError(HttpServletResponse.SC_NOT_FOUND, key + " could not be found!");
        return;
      }
    } catch (Throwable e) {
      LOG.error(e.getLocalizedMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          e.getLocalizedMessage());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    this.doGet(request, response);
    return;
  }

}
