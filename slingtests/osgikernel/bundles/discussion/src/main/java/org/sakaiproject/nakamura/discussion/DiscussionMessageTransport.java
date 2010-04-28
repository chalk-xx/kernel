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

import static javax.jcr.security.Privilege.JCR_READ;
import static javax.jcr.security.Privilege.JCR_REMOVE_NODE;
import static javax.jcr.security.Privilege.JCR_WRITE;
import static org.apache.sling.jcr.base.util.AccessControlUtil.replaceAccessControlEntry;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.discussion.DiscussionConstants;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessageTransport;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Handler for messages that are sent locally and intended for local delivery. Needs to be
 * started immediately to make sure it registers with JCR as soon as possible.
 */
@Component(immediate = true, label = "%discussion.messageTransport.label", description = "%discussion.messageTransport.desc")
@Service
public class DiscussionMessageTransport implements MessageTransport {
  private static final Logger LOG = LoggerFactory
      .getLogger(DiscussionMessageTransport.class);
  private static final String TYPE = DiscussionConstants.TYPE_DISCUSSION;

  @Reference
  private SlingRepository slingRepository;
  @Reference
  private MessagingService messagingService;

  @org.apache.felix.scr.annotations.Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  /**
   * Due to the fact that we are setting ACLs it is hard to unit test this class.
   * If this variable is set to true, than the ACL settings will be omitted.
   */
  private boolean testing = false;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.message.MessageTransport#send(org.sakaiproject.nakamura.api.message.MessageRoutes,
   *      org.osgi.service.event.Event, javax.jcr.Node)
   */
  public void send(MessageRoutes routes, Event event, Node originalMessage) {
    Session session = null;
    try {
      // Login as admin.
      session = slingRepository.loginAdministrative(null);

      for (MessageRoute route : routes) {
        if (DiscussionConstants.TYPE_DISCUSSION.equals(route.getTransport())) {
          String rcpt = route.getRcpt();
          // the path were we want to save messages in.
          String messageId = originalMessage.getProperty(MessageConstants.PROP_SAKAI_ID)
              .getString();
          String toPath = messagingService.getFullPathToMessage(rcpt, messageId, session);

          // Copy the node to the destination
          Node n = JcrUtils.deepGetOrCreateNode(session, toPath);

          PropertyIterator pi = originalMessage.getProperties();
          while (pi.hasNext()) {
            Property p = pi.nextProperty();
            if (!p.getName().contains("jcr:"))
              n.setProperty(p.getName(), p.getValue());
          }

          // Add some extra properties on the just created node.
          n.setProperty(MessageConstants.PROP_SAKAI_TYPE, route.getTransport());
          n.setProperty(MessageConstants.PROP_SAKAI_TO, route.getRcpt());
          n.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
              MessageConstants.BOX_INBOX);
          n.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
              MessageConstants.STATE_NOTIFIED);

          if (!testing) {
            // This will probably be saved in a site store. Not all the users will have
            // access to their message. So we add an ACL that allows the user to edit and
            // delete it later on.
            String from = originalMessage.getProperty(MessageConstants.PROP_SAKAI_FROM)
                .getString();
            Authorizable authorizable = AccessControlUtil.getUserManager(session)
                .getAuthorizable(from);
            replaceAccessControlEntry(session, toPath, authorizable.getPrincipal(),
                new String[] { JCR_WRITE, JCR_READ, JCR_REMOVE_NODE }, null, null);
          }
          if (session.hasPendingChanges()) {
            session.save();
          }
        }
      }
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  /**
   * Determines what type of messages this handler will process. {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.message.MessageHandler#getType()
   */
  public String getType() {
    return TYPE;
  }

  protected void bindMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  protected void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }

  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

  /**
   * This method should only be called for unit testing purposses. It will disable the ACL
   * settings.
   */
  protected void activateTesting() {
    testing = true;
  }

}
