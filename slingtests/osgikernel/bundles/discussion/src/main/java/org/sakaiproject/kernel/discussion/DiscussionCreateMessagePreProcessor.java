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

package org.sakaiproject.kernel.discussion;

import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.kernel.api.discussion.DiscussionConstants;
import org.sakaiproject.kernel.api.discussion.DiscussionManager;
import org.sakaiproject.kernel.api.message.CreateMessagePreProcessor;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessagingException;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

/**
 * Checks if the message the user wants to create has all the right properties on it.
 * 
 * @scr.component immediate="true" label="DiscussionCreateMessagePreProcessor"
 *                description="Checks request for Discussion messages"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.message.createpreprocessor" value="discussion"
 * @scr.service interface="org.sakaiproject.kernel.api.message.CreateMessagePreProcessor"
 * @scr.reference name="MessagingService"
 *                interface="org.sakaiproject.kernel.api.message.MessagingService"
 * @scr.reference name="DiscussionManager"
 *                interface="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 */
public class DiscussionCreateMessagePreProcessor implements CreateMessagePreProcessor {

  public static final Logger LOG = LoggerFactory.getLogger(DiscussionCreateMessagePreProcessor.class);
  
  private MessagingService messagingService;

  protected void bindMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  protected void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }

  private DiscussionManager discussionManager;
  protected void bindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }
  protected void unbindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }
  
  public void checkRequest(SlingHttpServletRequest request) throws MessagingException {

    // Check the TO field
    if (request.getRequestParameter(MessageConstants.PROP_SAKAI_TO) == null) {
      throw new MessagingException(HttpServletResponse.SC_BAD_REQUEST, "The "
          + MessageConstants.PROP_SAKAI_TO + " parameter has to be specified.");
    }

    // If replyto is specified, then use it.
    // If it is not, check if there is a writeto specified.
    if (request.getRequestParameter(DiscussionConstants.PROP_SAKAI_REPLY_ON) == null
        && request.getRequestParameter(DiscussionConstants.PROP_SAKAI_WRITETO) == null) {
      throw new MessagingException(HttpServletResponse.SC_BAD_REQUEST, "The "
          + DiscussionConstants.PROP_SAKAI_REPLY_ON + " or the "
          + DiscussionConstants.PROP_SAKAI_WRITETO + "parameter has to be specified.");
    }
    
    if (request.getRequestParameter(DiscussionConstants.PROP_SAKAI_REPLY_ON) != null
        && request.getRequestParameter(DiscussionConstants.PROP_SAKAI_WRITETO) != null) {
      throw new MessagingException(HttpServletResponse.SC_BAD_REQUEST, "The "
          + DiscussionConstants.PROP_SAKAI_REPLY_ON + " and the "
          + DiscussionConstants.PROP_SAKAI_WRITETO + "parameter cannot be specified at the same time.");
    }
    
    // check if the specified path is an absolute path to a messagestore.
    try {

      String writeTo = "";

      Node store = (Node) request.getResource().adaptTo(Node.class);
      Session session = store.getSession();
      
      if (request.getRequestParameter(DiscussionConstants.PROP_SAKAI_WRITETO) != null) {
        writeTo = request.getRequestParameter(DiscussionConstants.PROP_SAKAI_WRITETO)
        .getString();
      }
      else {
        String messageid = request.getRequestParameter(DiscussionConstants.PROP_SAKAI_REPLY_ON).getString();
        writeTo = discussionManager.findStoreForMessage(messageid, session);
      }
      
      if (!writeTo.startsWith("/")) {
        throw new MessagingException(HttpServletResponse.SC_BAD_REQUEST,
            DiscussionConstants.PROP_SAKAI_WRITETO + " should be an absolute path.");
      }

      if (!session.itemExists(writeTo)) {
        throw new MessagingException(HttpServletResponse.SC_BAD_REQUEST,
            DiscussionConstants.PROP_SAKAI_WRITETO + " points to a non existing path.");
      }

      Node n = (Node) session.getItem(writeTo);
      if (!messagingService.isMessageStore(n)) {
        throw new MessagingException(HttpServletResponse.SC_BAD_REQUEST,
            DiscussionConstants.PROP_SAKAI_WRITETO + " does not point to a message store.");
      }

    } catch (RepositoryException e) {
      LOG.warn("Failed to check the request for this discussionpost.");
      e.printStackTrace();
    }

  }

  public String getType() {
    return DiscussionConstants.TYPE_DISCUSSION;
  }

}
