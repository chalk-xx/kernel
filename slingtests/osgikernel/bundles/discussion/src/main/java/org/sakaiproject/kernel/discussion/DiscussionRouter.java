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

import org.sakaiproject.kernel.api.discussion.DiscussionConstants;
import org.sakaiproject.kernel.api.discussion.DiscussionManager;
import org.sakaiproject.kernel.api.message.AbstractMessageRoute;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageRouter;
import org.sakaiproject.kernel.api.message.MessageRoutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * This router will check for messages who have a transport of discussion or comment, then
 * checks the settings for this discussion. If there is a property that states all
 * discussion messages should be re-routed to an email address, this router will take care
 * of it.
 * 
 * @scr.component inherit="true" label="DiscussionRouter" immediate="true"
 * @scr.service interface="org.sakaiproject.kernel.api.message.MessageRouter"
 * @scr.property name="service.description"
 *               value="Manages Routing for the discussion posts."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.reference name="DiscussionManager"
 *                interface="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 */
public class DiscussionRouter implements MessageRouter {

  private DiscussionManager discussionManager;
  private static final Logger logger = LoggerFactory.getLogger(DiscussionRouter.class);

  protected void bindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }

  protected void unbindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = null;
  }

  public int getPriority() {
    return 0;
  }

  public void route(Node n, MessageRoutes routing) {
    // Check if this message is a discussion/comment transport.
    try {
      // TODO check sakai:to because I think sakai:type won't be staying?
      if (n.hasProperty(MessageConstants.PROP_SAKAI_TYPE)
          && n.hasProperty(DiscussionConstants.PROP_MARKER)) {

        String type = n.getProperty(MessageConstants.PROP_SAKAI_TYPE).getString();

        if ("comment".equals(type) || "discussion".equals(type)) {

          // TODO: I have a feeling that this is really part of something more generic
          // and not specific to discussion. If we make it specific to discussion we
          // will loose unified messaging and control of that messaging.

          // This is a discussion message, find the settings file for it.

          String marker = n.getProperty(DiscussionConstants.PROP_MARKER).getString();
          Node settings = discussionManager.findSettings(marker, n.getSession(), type);
          if (settings != null
              && settings.hasProperty(DiscussionConstants.PROP_NOTIFICATION)) {
            boolean sendMail = settings
                .getProperty(DiscussionConstants.PROP_NOTIFICATION).getBoolean();
            if (sendMail && settings.hasProperty(DiscussionConstants.PROP_NOTIFY_ADDRESS)) {
              String address = settings.getProperty(
                  DiscussionConstants.PROP_NOTIFY_ADDRESS).getString();
              // TODO: make this smtp.
              routing.add(new AbstractMessageRoute("internal:" + address) {
              });

            }
          }
        }

      }
    } catch (RepositoryException e) {
      logger.warn("Catched an exception when trying to re-route discussion messages: {}",
          e.getMessage());
      e.printStackTrace();
    }
  }

}
