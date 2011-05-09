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

package org.sakaiproject.nakamura.chat;

import com.google.common.collect.ImmutableMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.chat.ChatManagerService;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.LiteMessageProfileWriter;
import org.sakaiproject.nakamura.api.message.LiteMessageTransport;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.user.BasicUserInfoService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Handler for chat messages.
 */
@Component(label = "LiteChatMessageHandler", description = "Handler for internally delivered chat messages.", immediate = true)
@Services(value = { @Service(value = LiteMessageTransport.class),
    @Service(value = LiteMessageProfileWriter.class) })
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Handler for internally delivered chat messages") })
public class LiteChatMessageHandler implements LiteMessageTransport,
    LiteMessageProfileWriter {
  private static final Logger LOG = LoggerFactory.getLogger(LiteChatMessageHandler.class);
  private static final String TYPE = MessageConstants.TYPE_CHAT;
  private static final Object CHAT_TRANSPORT = "chat";

  @Reference
  protected transient ChatManagerService chatManagerService;

  @Reference
  protected transient Repository contentRepository;

  @Reference
  protected transient LiteMessagingService messagingService;

  @Reference
  protected transient BasicUserInfoService basicUserInfoService;

  /**
   * Default constructor
   */
  public LiteChatMessageHandler() {
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.message.MessageTransport#send(org.sakaiproject.nakamura.api.message.MessageRoutes,
   *      org.osgi.service.event.Event, javax.jcr.Node)
   */
  public void send(MessageRoutes routes, Event event, Content originalMessage) {
    Session session = null;
    try {

      session = contentRepository.loginAdministrative();
      ContentManager contentManager = session.getContentManager();

      for (MessageRoute route : routes) {
        if (CHAT_TRANSPORT.equals(route.getTransport())) {
          LOG.info("Started handling a message.");
          String rcpt = route.getRcpt();
          // the path were we want to save messages in.
          String messageId = (String)originalMessage.getProperty(MessageConstants.PROP_SAKAI_ID);
          String toPath = messagingService.getFullPathToMessage(rcpt, messageId, session);

          ImmutableMap.Builder<String, Object> propertyBuilder = ImmutableMap.builder();
          // Copy the node into the user his folder.
          contentManager.update(new Content(toPath.substring(0, toPath.lastIndexOf("/")), propertyBuilder.build()));
          contentManager.copy(originalMessage.getPath(), toPath, true);
          Content message = contentManager.get(toPath);

          // Add some extra properties on the just created node.
          message.setProperty(MessageConstants.PROP_SAKAI_READ, false);
          message.setProperty(MessageConstants.PROP_SAKAI_TO, rcpt);
          message.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
              MessageConstants.BOX_INBOX);
          message.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
              MessageConstants.STATE_NOTIFIED);
          message.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
              MessageConstants.SAKAI_MESSAGE_RT);

          long time = ((java.util.Calendar)originalMessage.getProperty(MessageConstants.PROP_SAKAI_CREATED)).getTimeInMillis();

          String from = (String) originalMessage.getProperty(MessageConstants.PROP_SAKAI_FROM);

          if ( from != null ) {
            // Set the rcpt in the cache.
            chatManagerService.put(rcpt, time);
            // Set the from in the cache
            chatManagerService.put(from, time);
          }

        }
      }

    } catch (AccessDeniedException e) {
      LOG.error(e.getLocalizedMessage());
    } catch (ClientPoolException e) {
      LOG.error(e.getLocalizedMessage());
    } catch (StorageClientException e) {
      LOG.error(e.getLocalizedMessage());
    } catch (IOException e) {
      LOG.error(e.getLocalizedMessage());
    } finally {
      try {
        if (session != null) {
          session.logout();
        }
      } catch (ClientPoolException e) {
        throw new RuntimeException("Failed to logout session.", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * @param jcrSession 
   *
   * @see org.sakaiproject.nakamura.api.message.LiteMessageProfileWriter#writeProfileInformation(Session,
   *      String, org.apache.sling.commons.json.io.JSONWriter)
   */
  public void writeProfileInformation(Session session, String recipient, JSONWriter write, javax.jcr.Session jcrSession) {
    try {
      Authorizable au = session.getAuthorizableManager().findAuthorizable(recipient);
      ValueMap map = new ValueMapDecorator(basicUserInfoService.getProperties(au));
      ((ExtendedJSONWriter) write).valueMap(map);
    } catch (Exception e) {
      LOG.error("Failed to write profile information for " + recipient, e);
    }
  }

  /**
   * Determines what type of messages this handler will process. {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.message.LiteMessageProfileWriter#getType()
   */
  public String getType() {
    return TYPE;
  }

}
