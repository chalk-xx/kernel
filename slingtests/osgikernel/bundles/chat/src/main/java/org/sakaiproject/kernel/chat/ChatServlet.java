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
package org.sakaiproject.kernel.chat;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.chat.ChatManagerService;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceParameter;
import org.sakaiproject.kernel.api.doc.ServiceResponse;
import org.sakaiproject.kernel.api.doc.ServiceSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * Will check if a user has any chat updates.
 */
@SlingServlet(selectors = { "chatupdate" }, resourceTypes = { "sakai/messagestore" }, generateComponent = true, methods = { "GET" })
@Reference(referenceInterface = ChatManagerService.class, name = "ChatManagerService")
@ServiceDocumentation(name = "ChatServlet", shortDescription = "Check for new chat messages.", description = "Provides a mechanism to check if the currently logged in user has new chat messages awaiting.", bindings = @ServiceBinding(type = BindingType.TYPE, bindings = "sakai/messagestore", selectors = @ServiceSelector(name = "chatupdate")), methods = { @ServiceMethod(name = "GET", response = {
    @ServiceResponse(code = 200, description = "Normal retrieval."),
    @ServiceResponse(code = 500, description = "Something went wrong trying to look for an update.") }, description = "GETs to this servlet will produce a JSON object with 2 keys. \n"
    + "<ul><li>update: A boolean that states if there is a new chat message.</li><li>time: The time since the last retrieval.</li></ul>", parameters = @ServiceParameter(name = "t", description = "This variable should hold the last time value retrieved from this servet. If this variable is ommitted it uses the current time.")) })
public class ChatServlet extends SlingAllMethodsServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChatServlet.class);
  private static final long serialVersionUID = -4011626674940239621L;
  private ChatManagerService chatManagerService;

  protected void bindChatManagerService(ChatManagerService chatManagerService) {
    this.chatManagerService = chatManagerService;
  }

  protected void unbindChatManagerService(ChatManagerService chatManagerService) {
    this.chatManagerService = null;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    String userID = request.getRemoteUser();
    boolean update = false;

    long time = System.currentTimeMillis();

    Long lastUpdate = chatManagerService.getLastUpdate(userID);
    LOGGER.info("lastUpdate = {}", lastUpdate);
    if (lastUpdate == null) {
      // This the first time (ever) the user poll's the chat update.
      // Insert it.
      chatManagerService.addUpdate(userID, time);
      update = true;
    } else {
      RequestParameter timestampParam = request.getRequestParameter("t");
      if (timestampParam != null) {
        time = Long.parseLong(timestampParam.getString());
        LOGGER.info("?t = {}", time);
        if (time < lastUpdate) {
          // There is a new message.
          time = lastUpdate;
          update = true;
        }
      }
      else {
        time = lastUpdate;
        update = true;
      }
    }
    
    LOGGER.info("Returned time = {}, update = {}", time, update);

    JSONWriter write = new JSONWriter(response.getWriter());
    try {
      write.object();
      write.key("update");
      write.value(update);
      write.key("time");
      write.value(time);
      write.endObject();
    } catch (JSONException e) {
      LOGGER.warn("Unable to parse JSON for user {} and time {}", userID, time);
      e.printStackTrace();
      response.sendError(500, "Unable to parse JSON.");
    }

    // Make sure the connection is not keep-alive.
    response.setHeader("Connection", "close");
  }
}
