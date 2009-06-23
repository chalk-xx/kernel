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

import static org.sakaiproject.kernel.api.connections.ConnectionConstants.CONNECTION_OPERATION;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.api.connections.ConnectionConstants;
import org.sakaiproject.kernel.api.connections.ConnectionException;
import org.sakaiproject.kernel.api.connections.ConnectionConstants.ConnectionOperations;

/**
 * This handles the connections servlet code that is shared between the POST
 * servlets which deal with connections
 */
public abstract class AbstractPostConnectionServlet extends
    AbstractConnectionServlet {

  private static final long serialVersionUID = 2222996718559864951L;

  protected ConnectionOperations OPERATION = ConnectionConstants.ConnectionOperations.REQUEST;

  protected void setOPERATION(ConnectionOperations operation) {
    OPERATION = operation;
  }

  @Override
  protected boolean preDispatch(SlingHttpServletRequest request,
      SlingHttpServletResponse response, Resource baseResource,
      Resource resource) {
    request.setAttribute(CONNECTION_OPERATION, OPERATION);
    // POST request only
    String requesterUserId = request.getRemoteUser(); // current user
    String targetUserId = (String) request.getAttribute(TARGET_USERID);
    if (targetUserId == null || "".equals(targetUserId)) {
      try {
        response
            .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "targetUserId not found in the request, cannot continue without it being set");
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
      return false;
    }

    try {
      if (ConnectionConstants.ConnectionOperations.REQUEST.equals(OPERATION)) {
        String[] types = request
            .getParameterValues(ConnectionConstants.SAKAI_CONNECTION_TYPES);
        // handle the request operation since it is special
        connectionManager.request(baseResource, targetUserId, types,
            requesterUserId);
      } else {
        // handle all the other operations
        connectionManager.connect(baseResource, targetUserId, OPERATION,
            requesterUserId);
      }
    } catch (ConnectionException e) {
      // convert the exception into messages
      try {
        response.sendError(e.getCode(), e.getMessage());
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
      return false;
    }
    return true;
  }

}
