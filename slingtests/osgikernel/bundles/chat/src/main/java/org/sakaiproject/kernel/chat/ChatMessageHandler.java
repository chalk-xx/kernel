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

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.Event;
import org.sakaiproject.kernel.api.chat.ChatManagerService;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageRoute;
import org.sakaiproject.kernel.api.message.MessageRoutes;
import org.sakaiproject.kernel.api.message.MessageTransport;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Handler for chat messages. This will also write to a logfile.
 * 
 * @scr.component label="ChatMessageHandler"
 *                description="Handler for internally delivered chat messages."
 *                immediate="true"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.sakaiproject.kernel.api.message.MessageTransport"
 * @scr.reference interface="org.apache.sling.jcr.api.SlingRepository"
 *                name="SlingRepository"
 * @scr.reference interface="org.sakaiproject.kernel.api.message.MessagingService"
 *                name="MessagingService"
 * @scr.reference name="ChatManagerService"
 *                interface="org.sakaiproject.kernel.api.chat.ChatManagerService"
 */
public class ChatMessageHandler implements MessageTransport {
  private static final Logger LOG = LoggerFactory.getLogger(ChatMessageHandler.class);
  private static final String TYPE = MessageConstants.TYPE_CHAT;
  private static final Object CHAT_TRANSPORT = "chat";

  private ChatManagerService chatManagerService;

  protected void bindChatManagerService(ChatManagerService chatManagerService) {
    this.chatManagerService = chatManagerService;
  }

  protected void unbindChatManagerService(ChatManagerService chatManagerService) {
    this.chatManagerService = null;
  }

  /**
   * The JCR Repository we access.
   * 
   */
  private SlingRepository slingRepository;

  /**
   * @param slingRepository
   *          the slingRepository to set
   */
  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  /**
   * @param slingRepository
   *          the slingRepository to unset
   */
  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

  private MessagingService messagingService;

  protected void bindMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  protected void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }

  /**
   * Default constructor
   */
  public ChatMessageHandler() {
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.message.MessageTransport#send(org.sakaiproject.kernel.api.message.MessageRoutes,
   *      org.osgi.service.event.Event, javax.jcr.Node)
   */
  public void send(MessageRoutes routes, Event event, Node originalMessage) {
    try {

      Session session = slingRepository.loginAdministrative(null);

      for (MessageRoute route : routes) {
        if (CHAT_TRANSPORT.equals(route.getTransport())) {
          LOG.info("Started handling a message.");
          String rcpt = route.getRcpt();
          // the path were we want to save messages in.
          String messageId = originalMessage.getProperty(MessageConstants.PROP_SAKAI_ID)
              .getString();
          String toPath = messagingService.getFullPathToMessage(rcpt, messageId, session);

          LOG.info("Writing {} to {}", originalMessage.getPath(), toPath);

          // Copy the node into the user his folder.
          JcrUtils.deepGetOrCreateNode(session, toPath);
          session.save();

          /*
           * This gives PathNotFoundExceptions... Workspace workspace =
           * session.getWorkspace(); workspace.copy(originalMessage.getPath(), toPath);
           */

          Node n = (Node) session.getItem(toPath);

          PropertyIterator pi = originalMessage.getProperties();
          while (pi.hasNext()) {
            Property p = pi.nextProperty();
            if (!p.getName().contains("jcr:"))
              n.setProperty(p.getName(), p.getValue());
          }

          // Add some extra properties on the just created node.
          n.setProperty(MessageConstants.PROP_SAKAI_READ, false);
          n.setProperty(MessageConstants.PROP_SAKAI_TO, rcpt);
          n.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
              MessageConstants.BOX_INBOX);
          n.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
              MessageConstants.STATE_NOTIFIED);
          n.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
              MessageConstants.SAKAI_MESSAGE_RT);
          n.save();

          // Set it in the cache.
          chatManagerService.addUpdate(rcpt, System.currentTimeMillis());
        }
      }

    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  /**
   * Determines what type of messages this handler will process. {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.message.MessageHandler#getType()
   */
  public String getType() {
    return TYPE;
  }

}
