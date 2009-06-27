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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.api.connections.ConnectionException;
import org.sakaiproject.kernel.api.connections.ConnectionManager;
import org.sakaiproject.kernel.api.connections.ConnectionOperation;
import org.sakaiproject.kernel.api.user.UserConstants;
import org.sakaiproject.kernel.connections.ConnectionUtils;
import org.sakaiproject.kernel.resource.AbstractVirtualPathServlet;
import org.sakaiproject.kernel.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/contactstore"
 * @scr.property name="sling.servlet.methods" values.0="POST" values.1="PUT"
 *               values.2="DELETE" values.3="GET"
 * @scr.property name="sling.servlet.selectors" values.0="invite" values.1="accept"
 *               values.2="reject" values.3="ignore" values.4="block" values.5="remove"
 * @scr.reference name="ConnectionManager"
 *                interface="org.sakaiproject.kernel.api.connections.ConnectionManager"
 */
public class ConnectionServlet extends AbstractVirtualPathServlet {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ConnectionServlet.class);
  private static final long serialVersionUID = 1112996718559864951L;

  private static final String TARGET_USERID = "connections:targetUserId";

  protected ConnectionManager connectionManager;

  protected void bindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  protected void unbindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = null;
  }

  @Override
  protected String getTargetPath(Resource baseResource, SlingHttpServletRequest request,
      SlingHttpServletResponse response, String realPath, String virtualPath) {
    String path;
    String user = request.getRemoteUser(); // current user
    if (user == null || UserConstants.ANON_USERID.equals(user)) {
      // cannot proceed if the user is not logged in
      try {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
            "User must be logged in to access connections");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      path = realPath; // default
    } else {
      
      // /_user/contacts.invite.html
      // /_user/contacts/aaron.accept.html
      
      String[] virtualParts = StringUtils.split(virtualPath, '.');
      if (virtualParts.length > 0) {
        String targetUser = virtualParts[0];
        path = ConnectionUtils.getConnectionPath(realPath,user,targetUser,virtualPath);        
        request.setAttribute(TARGET_USERID, targetUser);
      } else {
        // nothing extra included so use the base
        path = realPath;
      }
    }
    return path;
  }


  /**
   * {@inheritDoc}
   * 
   * @throws IOException
   * 
   * @see org.sakaiproject.kernel.resource.AbstractVirtualPathServlet#preDispatch(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse,
   *      org.apache.sling.api.resource.Resource, org.apache.sling.api.resource.Resource)
   */
  @Override
  protected boolean preDispatch(SlingHttpServletRequest request,
      SlingHttpServletResponse response, Resource baseResource, Resource resource)
      throws IOException {
    ConnectionOperation operation = ConnectionOperation.noop;
    if ("POST".equals(request.getMethod())) {
      String selector = request.getRequestPathInfo().getSelectorString();
      try {
        operation = ConnectionOperation.valueOf(selector);
      } catch (IllegalArgumentException e) {
        operation = ConnectionOperation.noop;
      }
    }
    try {
      String user = request.getRemoteUser(); // current user
      String targetUserId = null;
      switch (operation) {
      case noop:
        return true;
      default: 
        targetUserId = (String) request.getAttribute(TARGET_USERID);
        if (targetUserId == null || "".equals(targetUserId)) {
          response
              .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                  "targetUserId not found in the request, cannot continue without it being set");
          return false;
        }
      }
      connectionManager.connect(baseResource, user, targetUserId, operation);
    } catch (ConnectionException e) {
      LOGGER.error("Connection exception: {}", e);
      response.sendError(e.getCode(), e.getMessage());
      return false;
    }

    return true;

  }

}
