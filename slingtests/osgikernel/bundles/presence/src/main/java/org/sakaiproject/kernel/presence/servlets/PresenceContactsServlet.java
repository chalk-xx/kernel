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

package org.sakaiproject.kernel.presence.servlets;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.kernel.api.connections.ConnectionManager;
import org.sakaiproject.kernel.api.connections.ConnectionState;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.presence.PresenceService;
import org.sakaiproject.kernel.presence.PresenceUtils;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet deals with GET and JSON only and outputs the contacts listing presence related to the current user,
 * only includes accepted contacts
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/presence"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.selectors" value="contacts"
 * @scr.property name="sling.servlet.extensions" value="json"
 * 
 * @scr.reference name="ConnectionManager"
 *                interface="org.sakaiproject.kernel.api.connections.ConnectionManager"
 * @scr.reference name="PresenceService"
 *                interface="org.sakaiproject.kernel.api.presence.PresenceService"
 */
public class PresenceContactsServlet extends SlingAllMethodsServlet {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PresenceContactsServlet.class);

  private static final long serialVersionUID = 11111111L;

  protected PresenceService presenceService;

  protected void bindPresenceService(PresenceService presenceService) {
    this.presenceService = presenceService;
  }

  protected void unbindPresenceService(PresenceService presenceService) {
    this.presenceService = null;
  }

  protected ConnectionManager connectionManager;

  protected void bindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  protected void unbindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = null;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // get current user
    String user = request.getRemoteUser();
    if (user == null) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User must be logged in to check their status");
    }
    LOGGER.info("GET to PresenceContactsServlet ("+user+")");

    try {
      Writer writer = response.getWriter();
      ExtendedJSONWriter output = new ExtendedJSONWriter(writer);
      // start JSON object
      output.object();
      PresenceUtils.makePresenceJSON(output, user, presenceService, true);
      // add in the list of contacts info
      Session session = request.getResource().adaptTo(Node.class).getSession();
      List<String> userIds = connectionManager.getConnectedUsers(user, ConnectionState.ACCEPTED);
      output.key("contacts");
      output.array();
      for (String userId : userIds) {
        output.object();
        // put in the basics
        PresenceUtils.makePresenceJSON(output, userId, presenceService, true);
        // add in the profile
        output.key("profile");
        Node profileNode = (Node) session.getItem(PersonalUtils.getProfilePath(userId));
        ExtendedJSONWriter.writeNodeToWriter(output, profileNode);
        output.endObject();
      }
      output.endArray();
      // finish it
      output.endObject();
    } catch (JSONException e) {
      LOGGER.error(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

    return;
  }

}
