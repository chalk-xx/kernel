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
import org.sakaiproject.kernel.api.message.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
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
 * @scr.reference name="DiscussionManager"
 *                interface="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 */
public class DiscussionCreateMessagePreProcessor implements CreateMessagePreProcessor {

  public static final Logger LOG = LoggerFactory
      .getLogger(DiscussionCreateMessagePreProcessor.class);

  private DiscussionManager discussionManager;

  protected void bindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }

  protected void unbindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }

  public void checkRequest(SlingHttpServletRequest request) throws MessagingException {

    // Check the marker field
    if (request.getRequestParameter(DiscussionConstants.PROP_MARKER) == null) {
      throw new MessagingException(HttpServletResponse.SC_BAD_REQUEST, "The "
          + DiscussionConstants.PROP_MARKER + " parameter has to be specified.");
    }

    String marker = request.getRequestParameter(DiscussionConstants.PROP_MARKER)
        .getString();
    Session session = request.getResourceResolver().adaptTo(Session.class);
    String path = request.getRequestPathInfo().getResourcePath();

    // If there is a replyon property set, we check if it points to a valid one.
    if (request.getRequestParameter(DiscussionConstants.PROP_REPLY_ON) != null) {
      String messageId = request.getRequestParameter(
          DiscussionConstants.PROP_REPLY_ON).getString();
      Node msg = discussionManager.findMessage(messageId, marker, session, path);
      if (msg == null) {
        throw new MessagingException(HttpServletResponse.SC_BAD_REQUEST, "The "
            + DiscussionConstants.PROP_REPLY_ON
            + " does not point to a valid message.");
      }
    }
  }

  public String getType() {
    return DiscussionConstants.TYPE_DISCUSSION;
  }

}
