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

package org.sakaiproject.nakamura.discussion;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.discussion.DiscussionConstants;
import org.sakaiproject.nakamura.api.discussion.LiteDiscussionManager;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.message.LiteCreateMessagePreProcessor;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;

/**
 * Checks if the message the user wants to create has all the right properties on it.
 */
@Component(label = "%discussion.createMessagePreProcessor.label", description = "%discussion.createMessagePreProcessor.desc")
@Service
public class LiteDiscussionCreateMessagePreProcessor implements LiteCreateMessagePreProcessor {

  public static final Logger LOG = LoggerFactory
      .getLogger(LiteDiscussionCreateMessagePreProcessor.class);

  @Reference
  private LiteDiscussionManager discussionManager;

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "discussion")
  static final String CREATE_PREPROCESSOR = "sakai.message.createpreprocessor";

  protected void bindDiscussionManager(LiteDiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }

  protected void unbindDiscussionManager(LiteDiscussionManager discussionManager) {
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

    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver()
        .adaptTo(javax.jcr.Session.class));
    String path = request.getRequestPathInfo().getResourcePath();

    // If there is a replyon property set, we check if it points to a valid one.
    if (request.getRequestParameter(DiscussionConstants.PROP_REPLY_ON) != null) {
      String messageId = request.getRequestParameter(
          DiscussionConstants.PROP_REPLY_ON).getString();
      Content msg = discussionManager.findMessage(messageId, marker, session, path);
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
