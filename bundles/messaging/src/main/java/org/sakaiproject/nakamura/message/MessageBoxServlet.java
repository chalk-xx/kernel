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
package org.sakaiproject.nakamura.message;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "MessageBoxServlet", shortDescription = "Lists the messages of the current users mailboxes.", description = "Presents mailbox messages of current user in JSON format.", bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/system/messages"), methods = @ServiceMethod(name = "GET", description = "List current user's mailbox messages.", response = {
    @ServiceResponse(code = 200, description = "Request for information was successful. <br />"),
    @ServiceResponse(code = 401, description = "Unauthorized: credentials provided were not acceptable to return information for."),
    @ServiceResponse(code = 500, description = "Unable to return information about current user.") }))
@SlingServlet(paths = { "/system/messages" }, generateComponent = true, generateService = true, methods = { "GET" })
public class MessageBoxServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -3786472219389695181L;
  private static final Logger LOG = LoggerFactory.getLogger(MessageBoxServlet.class);

  @Reference
  protected transient LiteMessagingService messagingService;

  @Reference
  protected transient ProfileService profileService;

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      RequestParameter boxType = request.getRequestParameter("box");
      if (boxType == null || !(MessageConstants.BOX_INBOX.equals(boxType.getString()) || MessageConstants.BOX_OUTBOX.equals(boxType.getString()) || "all".equals(boxType.getString()))) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The box parameter must be 'inbox', 'outbox', or 'all')");
        return;
      }
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
      ContentManager cm = session.getContentManager();
      PrintWriter w = response.getWriter();
      ExtendedJSONWriter writer = new ExtendedJSONWriter(w);
      writer.object();
      // User info
      writer.key("items");
      writer.value(25);

      writer.key("results");
      writer.array();
      Collection<Iterable<Content>> boxes = new ArrayList<Iterable<Content>>();
      if ("all".equals(boxType.getString())) {
        boxes.add(getMessageBoxChildren(cm, session.getUserId(), MessageConstants.BOX_INBOX));
        boxes.add(getMessageBoxChildren(cm, session.getUserId(), MessageConstants.BOX_OUTBOX));
      } else {
        boxes.add(getMessageBoxChildren(cm, session.getUserId(), boxType.getString()));
      }
      int messageCount = 0;
      
      for(Iterable<Content> messages : boxes) {
        for (Content message : messages) {
          writer.object();
          for (String messagePropKey : message.getProperties().keySet()) {
            writer.key(messagePropKey);
            writer
                .value(message.getProperty(messagePropKey));
          }
          writer.endObject();
          messageCount++;
        }
      }
      writer.endArray();
      // Dump this user his number of unread messages.
      writer.key("total");
      writer.value(messageCount);

      writer.endObject();
    } catch (JSONException e) {
      LOG.error("Failed to create proper JSON response in /system/message", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to create proper JSON response.");
    } catch (StorageClientException e) {
      LOG.error(e.getLocalizedMessage());
    } catch (AccessDeniedException e) {
      LOG.error(e.getLocalizedMessage());
    } 

  }

  private Iterable<Content> getMessageBoxChildren(ContentManager cm, String userId, String box) throws StorageClientException, AccessDeniedException {
    Content content = cm.get(MessageConstants.SAKAI_MESSAGE_PATH_PREFIX + userId + "/message/" + box);
    if (content != null) {
      return content.listChildren();
    } else return new Iterable<Content>() {

      public Iterator<Content> iterator() {
        return new ArrayList<Content>().iterator();
      }};
  }

  
}