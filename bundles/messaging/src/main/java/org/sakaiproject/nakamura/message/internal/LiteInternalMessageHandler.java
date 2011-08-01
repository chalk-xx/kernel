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

import com.google.common.collect.ImmutableMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.locking.LockManager;
import org.sakaiproject.nakamura.api.locking.LockTimeoutException;
import org.sakaiproject.nakamura.api.message.LiteMessageProfileWriter;
import org.sakaiproject.nakamura.api.message.LiteMessageTransport;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessageTransport;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.presence.PresenceUtils;
import org.sakaiproject.nakamura.api.user.BasicUserInfoService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler for messages that are sent locally and intended for local delivery. Needs to be
 * started immediately to make sure it registers with JCR as soon as possible.
 */
@Component(immediate = true, label = "LiteInternalMessageHandler", description = "Handler for internally delivered messages.")
@Service({ LiteMessageTransport.class, LiteMessageProfileWriter.class })
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Handler for internally delivered messages.") })
public class LiteInternalMessageHandler implements LiteMessageTransport,
    LiteMessageProfileWriter {
  private static final Logger LOG = LoggerFactory.getLogger(LiteInternalMessageHandler.class);
  private static final String TYPE = MessageConstants.TYPE_INTERNAL;

  @Reference
  protected transient Repository slingRepository;

  @Reference
  protected transient LiteMessagingService messagingService;

  @Reference
  protected transient PresenceService presenceService;

  @Reference
  protected transient LockManager lockManager;
  @Reference
  private BasicUserInfoService basicUserInfoService;

  /**
   * Default constructor
   */
  public LiteInternalMessageHandler() {
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.message.LiteMessageTransport#send(org.sakaiproject.nakamura.api.message.MessageRoutes,
   *      org.osgi.service.event.Event, Content)
   */
  public void send(MessageRoutes routes, Event event, Content originalMessage) {
    Session session = null;
    try {

      session = slingRepository.loginAdministrative();

      // recipients keeps track of who have already received the message, to avoid
      // duplicate messages
      List<String> recipients = new ArrayList<String>();
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      for (MessageRoute route : routes) {
        if (MessageTransport.INTERNAL_TRANSPORT.equals(route.getTransport())) {
          String recipient = route.getRcpt();
          LOG.info("Started handling a message for delivery to {} ", recipient );
          // the path were we want to save messages in.
          String messageId = (String) originalMessage
              .getProperty(MessageConstants.PROP_SAKAI_ID);
          sendHelper(recipients, recipient, originalMessage, session, messageId,
              authorizableManager);
        }
      }
    } catch (AccessDeniedException e) {
      LOG.error(e.getMessage(), e);
    } catch (ClientPoolException e) {
      LOG.error(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOG.error(e.getMessage(), e);
    } finally {
      if (session != null) {
        try {
          session.logout();
        } catch (ClientPoolException e) {
          throw new RuntimeException("Failed to logout session.", e);
        }
      }
    }
  }

  private void sendHelper(List<String> recipients, String recipient,
      Content originalMessage, Session session, String messageId,
      AuthorizableManager authManager) {
    try {
      ContentManager contentManager = session.getContentManager();
      Authorizable au = authManager.findAuthorizable(recipient);
      if (au != null && au instanceof Group) {
        Group group = (Group) au;
        // user must be in the group directly to send a message:
        for (String memberName : group.getMembers()) {
          if (!recipients.contains(memberName)) {
            recipients.add(memberName);
            // call back to itself: this allows for groups to be in groups and future
            // extensions
            sendHelper(recipients, memberName, originalMessage, session, messageId,
                authManager);
          }
        }
      } else {
        // only send a message to a user who hasn't already received one:
        if (!recipients.contains(recipient)) {

          String messageStorePath = messagingService.getFullPathToStore(recipient, session);
          if (messageStorePath.endsWith("/")) {
            messageStorePath = messageStorePath.substring(0, messageStorePath.length() - 1);
          }
          boolean forPublicOrEveryone = hasEveryoneOrPublicPermission(messageStorePath, contentManager);

          if ( forPublicOrEveryone || messagingService.checkDeliveryAccessOk(recipient, originalMessage, session ) ) {
            String toPath = messagingService.getFullPathToMessage(recipient, messageId,
                session);
            
            
  
            try {
              lockManager.waitForLock(toPath);
            } catch (LockTimeoutException e1) {
              throw new MessagingException("Unable to lock destination message store");
            }
            
            ImmutableMap.Builder<String, Object> propertyBuilder = ImmutableMap.builder();
            // Copy the content into the user his folder.
            contentManager.update(
                new Content(toPath.substring(0, toPath.lastIndexOf("/")), propertyBuilder
                    .build()));
            contentManager.copy(originalMessage.getPath(), toPath, true);
            Content message = contentManager.get(toPath);
            LOG.debug("Message As delivered at {} from {} is {} ",new Object[]{message.getPath(), originalMessage.getPath(), message});
  
            // Add some extra properties on the just created node.
            message.setProperty(MessageConstants.PROP_SAKAI_READ, false);
            message.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX, MessageConstants.BOX_INBOX);
            message.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE, MessageConstants.STATE_NOTIFIED);
            message.setProperty(MessageConstants.PROP_SAKAI_MESSAGE_STORE, messagingService.getFullPathToStore(recipient, session));
            contentManager.update(message);
          } else {
            LOG.warn("Unable to deliver message, permission denied {} ", originalMessage.getPath());
          }
          recipients.add(recipient);
        }
      }
    } catch (AccessDeniedException e) {
      LOG.error(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOG.error(e.getMessage(), e);
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    } finally {
      lockManager.clearLocks();
    }
  }

  private boolean hasEveryoneOrPublicPermission(String path, ContentManager contentManager) throws StorageClientException, AccessDeniedException {
    if ("/".equals(path)) {
      return false;
    }
    Content content = contentManager.get(path);
    if (content == null) {
      return false;
    }
    if (content.hasProperty("sakai:permissions")) {
      if ("public".equals(content.getProperty("sakai:permissions")) || "everyone".equals(content.getProperty("sakai:permissions"))) {
        return true;
      } else {
        return false;
      }

    }
    String parentPath = PathUtils.getParentReference(path);
    return hasEveryoneOrPublicPermission(parentPath, contentManager);

  }

  /**
   * Determines what type of messages this handler will process. {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.message.LiteMessageProfileWriter#getType()
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
    try {
      // Look up the recipient and check if it is an authorizable.
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      Authorizable au = authorizableManager.findAuthorizable(recipient);
      if (au != null) {
        ValueMap map = new ValueMapDecorator(basicUserInfoService.getProperties(au));
        ExtendedJSONWriter.writeValueMapInternals(write, map);
        if (au instanceof User) {
          // Pass in the presence.
          PresenceUtils.makePresenceJSON(write, au.getId(), presenceService, true);
        }
      } else {
        // No idea what this recipient is.
        // Just output it.
        write.value(recipient);
      }
    } catch (JSONException e) {
      LOG.error(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOG.error(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOG.error(e.getMessage(), e);
    } /*catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }*/
  }

}