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

package org.sakaiproject.nakamura.message.internal;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageProfileWriter;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessageTransport;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Handler for messages that are sent locally and intended for local delivery. Needs to be
 * started immediately to make sure it registers with JCR as soon as possible.
 */
@Component(immediate = true, label = "InternalMessageHandler", description = "Handler for internally delivered messages.")
@Services(value = {
    @Service(value = MessageTransport.class),
    @Service(value = MessageProfileWriter.class)
})
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Handler for internally delivered messages.")})
public class InternalMessageHandler implements MessageTransport, MessageProfileWriter {
  private static final Logger LOG = LoggerFactory.getLogger(InternalMessageHandler.class);
  private static final String TYPE = MessageConstants.TYPE_INTERNAL;

  @Reference
  protected transient SlingRepository slingRepository;

  @Reference
  protected transient MessagingService messagingService;

  /**
   * Default constructor
   */
  public InternalMessageHandler() {
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.message.MessageTransport#send(org.sakaiproject.nakamura.api.message.MessageRoutes,
   *      org.osgi.service.event.Event, javax.jcr.Node)
   */
  public void send(MessageRoutes routes, Event event, Node originalMessage) {
    try {

      Session session = slingRepository.loginAdministrative(null);

      for (MessageRoute route : routes) {
        if (MessageTransport.INTERNAL_TRANSPORT.equals(route.getTransport())) {
          LOG.info("Started handling a message.");
          String rcpt = route.getRcpt();
          // the path were we want to save messages in.
          String messageId = originalMessage.getProperty(MessageConstants.PROP_SAKAI_ID)
              .getString();
          String toPath = messagingService.getFullPathToMessage(rcpt, messageId, session);

          // Copy the node into the user his folder.
          JcrUtils.deepGetOrCreateNode(session, toPath.substring(0, toPath
              .lastIndexOf("/")));
          session.save();
          session.getWorkspace().copy(originalMessage.getPath(), toPath);
          Node n = JcrUtils.deepGetOrCreateNode(session, toPath);

          // Add some extra properties on the just created node.
          n.setProperty(MessageConstants.PROP_SAKAI_READ, false);
          n.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
              MessageConstants.BOX_INBOX);
          n.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
              MessageConstants.STATE_NOTIFIED);

          if (session.hasPendingChanges()) {
            session.save();
          }
        }
      }
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
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

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.message.MessageProfileWriter#writeProfileInformation(javax.jcr.Session,
   *      java.lang.String, org.apache.sling.commons.json.io.JSONWriter)
   */
  public void writeProfileInformation(Session session, String recipient, JSONWriter write) {
    PersonalUtils.writeCompactUserInfo(session, recipient, write);
  }

}
