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
package org.sakaiproject.nakamura.message;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.message.MessageConstants.BOX_OUTBOX;
import static org.sakaiproject.nakamura.api.message.MessageConstants.BOX_PENDING;
import static org.sakaiproject.nakamura.api.message.MessageConstants.EVENT_LOCATION;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PENDINGMESSAGE_EVENT;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_MESSAGEBOX;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_SENDSTATE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.SAKAI_MESSAGE_RT;
import static org.sakaiproject.nakamura.api.message.MessageConstants.STATE_NONE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.STATE_NOTIFIED;
import static org.sakaiproject.nakamura.api.message.MessageConstants.STATE_PENDING;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.locking.LockManager;
import org.sakaiproject.nakamura.api.locking.LockTimeoutException;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ActivityUtils;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Service for doing operations with messages.
 */
@Component(immediate = true, label = "Sakai Messaging Service", description = "Service for doing operations with messages.", name = "org.sakaiproject.nakamura.api.message.LiteMessagingService")
@Service
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class LiteMessagingServiceImpl implements LiteMessagingService {

  @Reference
  protected transient LockManager lockManager;

  @Reference
  protected transient EventAdmin eventAdmin;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(LiteMessagingServiceImpl.class);

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.message.LiteMessagingService#create(org.sakaiproject.nakamura.api.lite.Session, java.util.Map)
   */
  public Content create(Session session, Map<String, Object> mapProperties)
      throws MessagingException {
    return create(session, mapProperties, null);
  }

  private String generateMessageId() {
    String messageId = String.valueOf(Thread.currentThread().getId())
        + String.valueOf(System.currentTimeMillis());
    try {
      return org.sakaiproject.nakamura.util.StringUtils.sha1Hash(messageId);
    } catch (Exception ex) {
      throw new MessagingException("Unable to create hash.");
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.message.LiteMessagingService#create(org.sakaiproject.nakamura.api.lite.Session, java.util.Map, java.lang.String)
   */
  public Content create(Session session, Map<String, Object> mapProperties, String messageId)
      throws MessagingException {

    if (messageId == null) {
      messageId = generateMessageId();
    }

    String user = session.getUserId();
    String messagePathBase = getFullPathToStore(user, session);
    return create(session, mapProperties, messageId, messagePathBase);

  }

  public Content create(Session session, Map<String, Object> mapProperties, String messageId, String messagePathBase)
    throws MessagingException {
    String box = (String) mapProperties.get(MessageConstants.PROP_SAKAI_MESSAGEBOX);
    if (box == null) {
      box = MessageConstants.BOX_OUTBOX;
    }
    String messagePath = messagePathBase + box + "/" + messageId;
    Content msg = new Content(messagePath, null);
    for (Entry<String, Object> e : mapProperties.entrySet()) {
      String val = e.getValue().toString();
      try {
        Long l = Long.valueOf(val);
        msg.setProperty(e.getKey(), l);
      } catch (NumberFormatException ex) {
        msg.setProperty(e.getKey(), val);
      }
    }
    // Add the id for this message.
    msg.setProperty(MessageConstants.PROP_SAKAI_ID, messageId);
    Calendar cal = Calendar.getInstance();
    msg.setProperty(MessageConstants.PROP_SAKAI_CREATED, cal);
    msg.setProperty("sling:resourceSuperType", "sparse/Content");
    msg.setProperty(MessageConstants.PROP_SAKAI_MESSAGE_STORE, messagePathBase);

    try {
      lockManager.waitForLock(messagePathBase);
    } catch (LockTimeoutException e1) {
      throw new MessagingException("Unable to lock user mailbox");
    }
    try {
      try {
        // TODO: perhaps we should check that we have permission to deliver the message, especially if routing is internal:/ 
        ContentManager contentManager = session.getContentManager();
        contentManager.update(msg);
        ActivityUtils.postActivity(eventAdmin, session.getUserId(), msg.getPath(), "content", "default", "message", "SENT_MESSAGE", null);
        raisePendingMessageEvent(session, msg);
      } catch (StorageClientException e) {
        LOGGER.warn("StorageClientException on trying to save message."
            + e.getMessage());
        throw new MessagingException("Unable to save message.");
      } catch (AccessDeniedException e) {
        LOGGER.warn("AccessDeniedException on trying to save message."
            + e.getMessage());
        throw new MessagingException("Unable to save message.");
      }
      return msg;
    } finally {
      lockManager.clearLocks();
    }
  }

  

  

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.message.LiteMessagingService#copyMessageNode(org.sakaiproject.nakamura.api.lite.content.Content, java.lang.String, org.sakaiproject.nakamura.api.lite.Session)
   */
  public void copyMessageNode(Content sourceMessage, String targetStore, Session session)
      throws StorageClientException, AccessDeniedException, IOException {
    String sourcePath = sourceMessage.getPath();
    String messageId = StorageClientUtils.getObjectName(sourcePath);
    String targetNodePath = PathUtils.toSimpleShardPath(targetStore, messageId, "");
    ContentManager contentManager = session.getContentManager();
    contentManager.copy(sourcePath, targetNodePath, true);
    Content msg = contentManager.get(targetNodePath);
    raisePendingMessageEvent(session, msg);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.message.LiteMessagingService#getFullPathToMessage(java.lang.String, java.lang.String, org.sakaiproject.nakamura.api.lite.Session)
   */
  public String getFullPathToMessage(String rcpt, String messageId, Session session) throws MessagingException {
    String storePath = getFullPathToStore(rcpt, session);
    return storePath + MessageConstants.BOX_INBOX +"/" + messageId;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.message.LiteMessagingService#getFullPathToStore(java.lang.String, org.sakaiproject.nakamura.api.lite.Session)
   */
  public String getFullPathToStore(String rcpt, Session session) throws MessagingException {
    if (rcpt.indexOf("/") >= 0) {
      // This is a path
      return PathUtils.toUserContentPath(rcpt) + "/";
    }
    return LitePersonalUtils.getHomePath( rcpt ) + "/" + MessageConstants.FOLDER_MESSAGES + "/";
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.message.LiteMessagingService#expandAliases(java.lang.String)
   */
  public List<String> expandAliases(String localRecipient) {
    List<String> expanded = new ArrayList<String>();
    // at the moment we dont do alias expansion
    expanded.add(localRecipient);
    return expanded;
  }
  
  /**
   * Check that the message can be delivered by the creator of the message. If the
   * destination is a users inbox, then there are no permissions, however if the
   * destination is a content location, then we need to check that the user would have had
   * write to that location. The user is defined as the owner of the outbox, and failing
   * that the user that created the message node.
   * 
   * @param recipient the recipient of the message
   * @param originalMessage the original message
   * @param session the admin session being used to perform the delivery.
   * @return true if the message should be delivered, false if not.
   */
  public boolean checkDeliveryAccessOk(String recipient, Content originalMessage,
      Session session) {
    try {
      if (recipient.indexOf('/') < 0) {
        return true; // delivery to non path recipients is always granted.
      }
      // extract the target path, and the user who created the original message, and then
      // check that the user has write to the location.
      String path = getFullPathToStore(recipient, session);
      // messages come from an outbox in the users home space
      String sendingUser = PathUtils.getAuthorizableId(originalMessage.getPath());
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      Authorizable sendingUserAu = authorizableManager.findAuthorizable(sendingUser);
      if (sendingUserAu == null || sendingUserAu.isGroup()) {
        sendingUser = (String) originalMessage.getProperty(Content.CREATED_BY_FIELD);
        sendingUserAu = authorizableManager.findAuthorizable(sendingUser);
      }
      if (sendingUserAu == null) {
        return false;
      }

      return session.getAccessControlManager().can(sendingUserAu, Security.ZONE_CONTENT,
          path, Permissions.CAN_WRITE);
    } catch (StorageClientException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.error(e.getMessage(), e);
    }
    return false;
  }

  private void raisePendingMessageEvent(Session session, Content msg) throws StorageClientException, AccessDeniedException {
    if (SAKAI_MESSAGE_RT.equals(msg.getProperty(SLING_RESOURCE_TYPE_PROPERTY)) &&
            (!msg.hasProperty(PROP_SAKAI_MESSAGEBOX) ||
                    (BOX_OUTBOX.equals(msg.getProperty(PROP_SAKAI_MESSAGEBOX))
                            || BOX_PENDING.equals(msg.getProperty(PROP_SAKAI_MESSAGEBOX))))) {
      String sendstate;
      if (msg.hasProperty(PROP_SAKAI_SENDSTATE)) {
        sendstate = (String) msg.getProperty(PROP_SAKAI_SENDSTATE);
      } else {
        sendstate = STATE_NONE;
      }

      if (STATE_NONE.equals(sendstate) || STATE_PENDING.equals(sendstate)) {

        msg.setProperty(PROP_SAKAI_SENDSTATE, STATE_NOTIFIED);
        session.getContentManager().update(msg);

        Dictionary<String, Object> messageDict = new Hashtable<String, Object>();
        // WARNING
        // We can't pass in the node, because the session might expire before the event gets handled
        // This does mean that the listener will have to get the node each time, and probably create a new session for each message
        // This might be heavy on performance.
        messageDict.put(EVENT_LOCATION, msg.getPath());
        messageDict.put(UserConstants.EVENT_PROP_USERID, session.getUserId());
        LOGGER.debug("Launched event for message: {} ", msg.getPath());
        Event pendingMessageEvent = new Event(PENDINGMESSAGE_EVENT, messageDict);
        // KERN-790: Initiate a synchronous event.
        try {
          eventAdmin.postEvent(pendingMessageEvent);
        } catch (Exception e) {
          LOGGER.warn("Failed to post message dispatch event, cause {} ", e.getMessage(), e);
        }
      }

    }
  }

}