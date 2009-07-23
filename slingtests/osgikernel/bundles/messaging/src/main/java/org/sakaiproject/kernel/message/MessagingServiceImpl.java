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
package org.sakaiproject.kernel.message;

import static org.sakaiproject.kernel.api.message.MessageConstants.SAKAI_MESSAGESTORE_RT;

import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.locking.LockManager;
import org.sakaiproject.kernel.api.locking.LockTimeoutException;
import org.sakaiproject.kernel.api.message.MessagingException;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

/**
 * Service for doing operations with messages.
 * 
 * @scr.component immediate="true" label="Sakai Messaging Service"
 *                description="Service for doing operations with messages."
 *                name="org.sakaiproject.kernel.api.message.MessagingService"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.sakaiproject.kernel.api.message.MessagingService"
 */
public class MessagingServiceImpl implements MessagingService {

  /** @scr.reference */
  private LockManager lockManager;
  
  private static final Logger LOGGER = LoggerFactory
      .getLogger(MessagingServiceImpl.class);

  /**
   * 
   * {@inheritDoc}
   * 
   * @throws MessagingException
   * 
   * @see org.sakaiproject.kernel.api.message.MessagingService#create(org.apache.sling.api.resource.Resource)
   */
  public Node create(Session session, Map<String, Object> mapProperties)
      throws MessagingException {
    return create(session, mapProperties, null);
  }
  
  private String generateMessageId() {
    String messageId = String.valueOf(Thread.currentThread().getId())
        + String.valueOf(System.currentTimeMillis());
    try {
      return messageId = org.sakaiproject.kernel.util.StringUtils.sha1Hash(messageId);
    } catch (Exception ex) {
      throw new MessagingException("Unable to create hash.");
    }
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @throws MessagingException
   * 
   * @see org.sakaiproject.kernel.api.message.MessagingService#create(org.apache.sling.api.resource.Resource)
   */
  public Node create(Session session, Map<String, Object> mapProperties, String messageId)
      throws MessagingException {

    Node msg = null;
    if (messageId == null) {
      messageId = generateMessageId();
    }

    String user = session.getUserID();
    String messagePathBase = MessageUtils.getMessagePathBase(user);
    try {
      lockManager.waitForLock(messagePathBase);
    } catch (LockTimeoutException e1) {
      throw new MessagingException("Unable to lock user mailbox");
    }
    try {
      String messagePath = MessageUtils.getMessagePath(user, messageId);
      try {
        msg = JcrUtils.deepGetOrCreateNode(session, messagePath);
        
        for (Entry<String, Object> e : mapProperties.entrySet()) {
          msg.setProperty(e.getKey(), e.getValue().toString());
        }
        
      } catch (RepositoryException e) {
        LOGGER.warn("RepositoryException on trying to save message."
            + e.getMessage());
        e.printStackTrace();
        throw new MessagingException("Unable to save message.");
      }
      return msg;
    } finally {
      lockManager.clearLocks();
    }
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.message.MessagingService#getMessageStorePathFromMessageNode(javax.jcr.Node)
   */
  public String getMessageStorePathFromMessageNode(Node msg)
      throws ValueFormatException, PathNotFoundException,
      ItemNotFoundException, AccessDeniedException, RepositoryException {
    Node n = msg;
    while (!"/".equals(n.getPath())) {
      if (n.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
          && SAKAI_MESSAGESTORE_RT.equals(n.getProperty(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
        return n.getPath();
      }
      n = n.getParent();
    }
    return null;
  }


}
