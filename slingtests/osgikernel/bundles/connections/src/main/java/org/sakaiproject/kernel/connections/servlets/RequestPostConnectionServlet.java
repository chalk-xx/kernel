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
import org.sakaiproject.kernel.api.connections.ConnectionConstants;
import org.sakaiproject.kernel.api.connections.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * This handles POST requests for requesting connections
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/contactstore"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="request"
 * @scr.reference name="ConnectionManager"
 *                interface="org.sakaiproject.kernel.api.connections.ConnectionManager"
 */
public class RequestPostConnectionServlet extends AbstractPostConnectionServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestPostConnectionServlet.class);

  private static final long serialVersionUID = 111L;

  private static final String CONTACT = "contact";
  private static final String CONTACT_TYPES = "type";

  public RequestPostConnectionServlet() {
    setOPERATION(ConnectionConstants.ConnectionOperations.REQUEST);
  }

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    if (request.getParameter(CONTACT) == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Need to supply " + CONTACT
          + " argument");
      return;
    }
    String[] types = request.getParameterValues(CONTACT_TYPES);
    String targetUserId = request.getParameter(CONTACT);
    Resource baseResource = request.getResource();
    String user;
    try {
      user = request.getResource().adaptTo(Node.class).getSession().getUserID();
    } catch (RepositoryException e1) {
      LOGGER.error("Error getting repository session {}", e1);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to get repository session");
      return;
    }
    try {
      connectionManager.request(baseResource, targetUserId, types, user);
    } catch (ConnectionException e) {
      LOGGER.error("Connection exception: {}", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Connection exception adding request");
    }
  }

}
