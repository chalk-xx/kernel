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
package org.sakaiproject.nakamura.api.message;

import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.content.Content;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * A messaging service that provides methods specific to messaging. There is some binding
 * to common types of messaging within this service, although the intention is that this
 * service is a generic service, not bound to any one implementation of messaging. There
 * will be only one implementaiton of this service within an instance of K2.
 */
public interface LiteMessagingService {

  /**
   * Creates a new message for the user associated with the provided session. Message
   * properties are extracted from the supplied map. The messageId supplied must be
   * guaranteed unique
   *
   * @param resource
   * @param mapProperties
   * @param messageId
   *          Globally unique message identifier
   * @return
   * @throws MessagingException
   */
  public Content create(Session session, Map<String, Object> mapProperties, String messageId)
      throws MessagingException;

  /**
   * Creates a new message for the user associated with the provided session. Message
   * properties are extracted from the supplied map
   *
   * @param resource
   * @param mapProperties
   * @return
   * @throws MessagingException
   */
  public Content create(Session session, Map<String, Object> mapProperties)
      throws MessagingException;

  /**
   * @param session
   * @param mapProperties
   * @param messageId
   * @param messagePathBase
   * @return
   * @throws MessagingException
   */
  Content create(Session session, Map<String, Object> mapProperties, String messageId,
      String messagePathBase) throws MessagingException;

  /**
   * Gets the full JCR path for a given recipient and a message ID.
   * @param rcpt The recipient. Can be a group or a user.
   * @param messageId The ID of the message.
   * @param session
   * @return The JCR path to that message.
   * @throws MessagingException
   */
  public String getFullPathToMessage(String rcpt, String messageId, Session session) throws MessagingException;

  /**
   * Gets the full JCR path to a store for a recipient.
   * @param rcpt The recipient. Can be a group or a user.
   * @param session
   * @return The JCR path to the store.
   * @throws MessagingException
   */
  public String getFullPathToStore(String rcpt, Session session) throws MessagingException;

  /**
   * Copies a message with id <em>messageId</em> from <em>source</em> to <em>target</em>
   * @param sourceMessage
   * @param targetMessageStore
   * @param session
   *
   * @throws RepositoryException
   * @throws PathNotFoundException
   */
  public void copyMessageNode(Content sourceMessage, String targetMessageStore, Session session) throws StorageClientException, AccessDeniedException, IOException;

  /**
   * Expand a local deliver alias into a list of recipients, these are all local (ie no @ or domain), external
   * routing is not performed by the messaging service.
   *
   * @param localRecipient
   * @return a list of local recipients
   */
  public List<String> expandAliases(String localRecipient);

  /**
   * Check that the the message should be delivered.
   * @param recipient the recipient that delivery is being attempted to.
   * @param originalMessage the original message.
   * @return true if the sender has permission to deliver the message, false if not.
   */
  public boolean checkDeliveryAccessOk(String recipient, Content originalMessage, Session session);


}
