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
package org.sakaiproject.kernel.api.message;

import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.query.InvalidQueryException;

/**
 * A messaging service that provides methods specific to messaging. There is some binding
 * to common types of messaging within this service, although the intention is that this
 * service is a generic service, not bound to any one implementation of messaging. There
 * will be only one implementaiton of this service within an instance of K2.
 */
public interface MessagingService {

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
  public Node create(Session session, Map<String, Object> mapProperties, String messageId)
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
  public Node create(Session session, Map<String, Object> mapProperties)
      throws MessagingException;

  /**
   * Gets the absolute path to the message store from a message. ex:
   * /_private/D0/33/E2/admin/messages
   * 
   * @param msg
   *          A message node
   * @return
   */
  public String getMessageStorePathFromMessageNode(Node msg) throws ValueFormatException,
      PathNotFoundException, ItemNotFoundException, AccessDeniedException,
      RepositoryException;

  /**
   * Gets the path for the message starting at the message store. ex:
   * /fd/e1/df/h1/45fsdf4sd453uy4ods4fa45r4
   * 
   * @param msg
   * @return
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws ItemNotFoundException
   * @throws AccessDeniedException
   * @throws RepositoryException
   */
  /*
   * public String getMessagePathFromMessageStore(Node msg) throws ValueFormatException,
   * PathNotFoundException, ItemNotFoundException, AccessDeniedException,
   * RepositoryException;
   */

  /**
   * Searches for mailboxes on the system associated with a supplied e-mail address
   * 
   * @param session
   *          The session from which to execute the search
   * @param emailAddress
   *          The email address for which to search
   * @return A list of the mailbox / principal names
   */
  public List<String> getMailboxesForEmailAddress(Session session, String emailAddress)
      throws InvalidQueryException, RepositoryException;

  /**
   * Gets the full JCR path for a given recipient and a message ID.
   * @param rcpt The recipient. Can be either a site, group or a user. Sites should be prefixed with s-, groups with g-.
   * @param messageId The ID of the message.
   * @param session
   * @return The JCR path to that message.
   * @throws MessagingException
   */
  public String getFullPathToMessage(String rcpt, String messageId, Session session) throws MessagingException;

  /**
   * Gets the full JCR path to a store for a recipient.
   * @param rcpt The recipient. Can be either a site, group or a user. Sites should be prefixed with s-, groups with g-.
   * @param session
   * @return The JCR path to the store.
   * @throws MessagingException
   */
  public String getFullPathToStore(String rcpt, Session session) throws MessagingException;

  /**
   * Returns the URI to a message.
   * @param rcpt
   * @param messageId
   * @param session
   * @return The URI to a message. ex: /_user/message/user1/a123fd4564ed15468641
   * @throws MessagingException
   */
  public String getUriToMessage(String rcpt, String messageId, Session session) throws MessagingException;

  /**
   * Gets the URI to a message store for a certain rcpt.
   * @param rcpt
   * @param session
   * @return The URI to a message store. ex: /_group/message/g-dummygroup
   * @throws MessagingException
   */
  public String getUriToStore(String rcpt, Session session) throws MessagingException;



  /**
   * Copies a message with id <em>messageId</em> from <em>source</em> to <em>target</em>
   * 
   * @param adminSession
   * @param target
   * @param source
   * @param messageId
   * @throws RepositoryException
   * @throws PathNotFoundException
   */
  public void copyMessage(Session adminSession, String target, String source,
      String messageId) throws PathNotFoundException, RepositoryException;

  /**
   * Checks if the provided node is a message store node.
   * 
   * @param n
   * @return
   */
  public boolean isMessageStore(Node n);

}
