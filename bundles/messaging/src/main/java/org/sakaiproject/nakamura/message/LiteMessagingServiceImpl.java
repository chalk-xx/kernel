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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.locking.LockManager;
import org.sakaiproject.nakamura.api.locking.LockTimeoutException;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
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
        ContentManager contentManager = session.getContentManager();
        contentManager.update(msg);
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
    String path = "";
    if (rcpt.startsWith("w-")) {
      // This is a widget
      // TODO This is also a bug. KERN-1442
      return LitePersonalUtils.expandHomeDirectory(rcpt.substring(2)) + "/";
    }
    // TODO TEMPORARY HACK TO ENABLE SPARSE MIGRATION! Use a proper service once the Home Folder
    // logic is properly set up.
    path = MessageConstants.SAKAI_MESSAGE_PATH_PREFIX + rcpt + "/" + MessageConstants.FOLDER_MESSAGES + "/";
//      Authorizable au = PersonalUtils.getAuthorizable(session, rcpt);
//      path = PersonalUtils.getHomeFolder(au) + "/" + MessageConstants.FOLDER_MESSAGES;

    return path;
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

}