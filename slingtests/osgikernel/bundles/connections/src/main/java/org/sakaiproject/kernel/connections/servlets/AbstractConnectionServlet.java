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

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.api.connections.ConnectionManager;
import org.sakaiproject.kernel.resource.AbstractVirtualPathServlet;
import org.sakaiproject.kernel.util.PathUtils;
import org.sakaiproject.kernel.util.StringUtils;

/**
 * This handles the connections servlet code that is shared between the various
 * servlets which are assigned to the connection operations
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/contactstore"
 * @scr.property name="sling.servlet.methods" values.0="POST"
 * @scr.reference name="ConnectionManager"
 *                interface="org.sakaiproject.kernel.api.connections.ConnectionManager"
 */
public abstract class AbstractConnectionServlet extends
    AbstractVirtualPathServlet {

  private static final long serialVersionUID = 1112996718559864951L;
  protected static final String TARGET_USERID = "connections:targetUserId";

  protected ConnectionManager connectionManager;

  protected void bindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  protected void unbindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = null;
  }

  @Override
  protected String getTargetPath(Resource baseResource,
      SlingHttpServletRequest request, SlingHttpServletResponse response,
      String realPath, String virtualPath) {
    String path;
    String requesterUserId = request.getRemoteUser(); // current user
    if (requesterUserId == null) {
      // cannot proceed if the user is not logged in
      try {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
            "User must be logged in to access connections");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      path = realPath; // default
    } else {
      String[] virtualParts = StringUtils.split(virtualPath, '/');
      if (virtualParts.length > 0) {
        String targetUserId = virtualParts[0];
        request.setAttribute(TARGET_USERID, targetUserId);
        // build a user contact path e.g. contacts/..../nico/..../aaron
        String userPath = realPath
            + PathUtils.getHashedPath(requesterUserId, 4);
        // don't lose the virtual path
        String pathEnd = StringUtils.join(virtualParts, 1, '/');
        path = PathUtils.toInternalHashedPath(userPath, targetUserId, pathEnd);
      } else {
        // nothing extra included so use the base
        path = realPath;
      }
    }
    return path;
  }

}
