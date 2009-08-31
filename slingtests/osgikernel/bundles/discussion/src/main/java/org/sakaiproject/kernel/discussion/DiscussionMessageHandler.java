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

import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.service.event.Event;
import org.sakaiproject.kernel.api.discussion.DiscussionConstants;
import org.sakaiproject.kernel.api.discussion.DiscussionManager;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageHandler;
import org.sakaiproject.kernel.api.message.MessageUtils;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.util.DateUtils;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Discussion handler
 * 
 * @scr.component label="DiscussionMessageHandler"
 *                description="Handler for discussion messages." immediate="true"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.sakaiproject.kernel.api.message.MessageHandler"
 * 
 * @scr.reference name="SiteService"
 *                interface="org.sakaiproject.kernel.api.site.SiteService"
 * @scr.reference interface="org.apache.sling.jcr.api.SlingRepository"
 *                name="SlingRepository" bind="bindSlingRepository"
 *                unbind="unbindSlingRepository"
 * @scr.reference name="DiscussionManager"
 *                interface="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 */
public class DiscussionMessageHandler implements MessageHandler {

  public static final Logger LOG = LoggerFactory.getLogger(DiscussionMessageHandler.class);

  /**
   * Site service binding
   */
  private SiteService siteService;

  protected void bindSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  protected void unbindSiteService(SiteService siteService) {
    this.siteService = null;
  }
  /**
   * Sling repository binding
   */
  private SlingRepository slingRepository;

  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

  /**
   * Discussion Manager binding
   */
  private DiscussionManager discussionManager;

  protected void bindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }

  protected void unbindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }

  public String getType() {
    return DiscussionConstants.TYPE_DISCUSSION;
  }

  public void handle(Event event, Node node) {
    try {
      LOG.info("Handling discussion message {}" + node.getPath());
      Session session = node.getSession();

      // The ID for this message.
      String messageId = node.getName();

      String writeTo = "";
      // Check if it is a reply:
      if (node.hasProperty(DiscussionConstants.PROP_SAKAI_REPLY_ON)) {
        // This is a reply, find the store were the first message is in.
        String replyMessageId = node.getProperty(DiscussionConstants.PROP_SAKAI_REPLY_ON)
            .getString();
        writeTo = discussionManager.findStoreForMessage(replyMessageId, node.getSession());
      } else {
        // This is the initial post. This should have a writeto property on it.
        writeTo = node.getProperty(DiscussionConstants.PROP_SAKAI_WRITETO).getString();
      }

      LOG.info("Writing discussion to store: {}", writeTo);

      // Now check if the user has access to this store.
      AccessControlManager acm = AccessControlUtil.getAccessControlManager(session);
      Privilege[] privs = { acm.privilegeFromName(Privilege.JCR_WRITE) };
      if (acm.hasPrivileges(writeTo, privs)) {

        // the user has access, copy the message to this store.
        LOG.info("User has access.");

        // Create the path to the message.
        String targetMessage = PathUtils.toInternalHashedPath(writeTo, messageId, "");
        String parent = targetMessage.substring(0, targetMessage.lastIndexOf("/"));
        JcrUtils.deepGetOrCreateNode(session, parent);

        // Save the session so the parent gets created.
        session.save();

        // Copy the message over.
        session.getWorkspace().copy(node.getPath(), targetMessage);

        // Save the session so we can get the copied item.
        session.save();

        // Get the newly created message and set some extra properties on it.
        Node newMessage = (Node) session.getItem(targetMessage);

        // TODO: This should be jcr:created, but this doesn't seem to get created.
        newMessage.setProperty("sakai:created", DateUtils.rfc3339());
        newMessage.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
            MessageConstants.STATE_NOTIFIED);
        newMessage.setProperty(MessageConstants.PROP_SAKAI_READ, false);
        newMessage.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX, MessageConstants.BOX_INBOX);
        newMessage.setProperty(DiscussionConstants.PROP_SAKAI_WRITETO, writeTo);

        // Save the properties
        session.save();

        // If this store is in a site, then we will notify all other members of this site.
        Node store = (Node) session.getItem(writeTo);
        Node siteNode = store.getParent();
        if (siteService.isSite(siteNode)) {
          // Get all the members of this site.
          int total = siteService.getMemberCount(siteNode);

          try {
            Session adminSession = slingRepository.loginAdministrative(null);

            Iterator<User> it = siteService.getMembers(siteNode, 0, total, null);
            while (it.hasNext()) {
              User user = it.next();
              // The current user already has a message in his messagestore, no need to
              // include him.
              String userID = user.getID();
              if (!userID.equals(session.getUserID())) {
                // Copy our message in the member's messagestore.
                String target = MessageUtils.getMessagePath(userID, messageId);
                LOG.info("Copying message for user: {} store: {}", userID, target);
                
                // Create path to message.
                String targetParent = target.substring(0, target.lastIndexOf("/"));
                JcrUtils.deepGetOrCreateNode(adminSession, targetParent);
                adminSession.save();
                adminSession.getWorkspace().copy(newMessage.getPath(), target);                
              }              
              if (adminSession.hasPendingChanges()) {
                adminSession.save();
              }
            }
            LOG.info("Sucessfully wrote a message to all the site's members.");
          } catch (RepositoryException ex) {
            LOG.warn("Unable to send message to other users of the site.");
          }
        }

        LOG.info("Sucessfully wrote a comment message to the store.");

      }

    } catch (RepositoryException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
