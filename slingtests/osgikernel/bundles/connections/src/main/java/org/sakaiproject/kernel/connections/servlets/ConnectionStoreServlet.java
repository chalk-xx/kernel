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
package org.sakaiproject.kernel.connections.servlets;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceResponse;
import org.sakaiproject.kernel.api.user.UserConstants;
import org.sakaiproject.kernel.connections.ConnectionUtils;
import org.sakaiproject.kernel.resource.AbstractVirtualPathServlet;
import org.sakaiproject.kernel.resource.VirtualResourceProvider;
import org.sakaiproject.kernel.util.StringUtils;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "ConnectionStoreServlet", description = "Allows for updating/viewing of contact nodes.", shortDescription = "Allows for updating/viewing of contact nodes.", 
    bindings = { @ServiceBinding(type = BindingType.TYPE, bindings = { "sakai/contactstore" }) }, 
    methods = {
      @ServiceMethod(name = "GET", description = "Will return the contact node", response = @ServiceResponse(code = 200, description = "Dumps the contact node.")),
      @ServiceMethod(name = "POST", description = "Update the contact node", response = @ServiceResponse(code = 200, description = "Modifies the contact node."))
    })
@SlingServlet(resourceTypes = { "sakai/contactstore" }, methods = { "GET",
    "POST" })
@Properties(value = {
    @Property(name = "service.description", value = "Provides support for connection stores."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class ConnectionStoreServlet extends AbstractVirtualPathServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  @Reference
  protected VirtualResourceProvider virtualResourceProvider;

  @Override
  protected String getTargetPath(Resource baseResource,
      SlingHttpServletRequest request, SlingHttpServletResponse response,
      String realPath, String virtualPath) {
    String path = realPath;
    String user = request.getRemoteUser(); // current user
    if (user == null || UserConstants.ANON_USERID.equals(user)) {
      // cannot proceed if the user is not logged in
      try {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
            "User must be logged in to access connections");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {

      // example: /_user/contacts/simong/user1.json
      String[] users = StringUtils.split(virtualPath, '/');
      if (users.length == 2) {
        path = ConnectionUtils.getConnectionPath(users[0], users[1], "");
      }
    }
    return path;
  }

  @Override
  protected VirtualResourceProvider getVirtualResourceProvider() {
    return virtualResourceProvider;
  }

}
