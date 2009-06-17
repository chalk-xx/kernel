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

package org.sakaiproject.kernel.message.chat;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.Event;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageHandler;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.api.personal.PersonalConstants;
import org.sakaiproject.kernel.api.user.UserFactoryService;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

/**
 * Handler for chat messages. This will also write to a logfile.
 * 
 * @scr.component label="ChatMessageHandler"
 *                description="Handler for internally delivered chat messages."
 *                immediate="true"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.sakaiproject.kernel.api.message.MessageHandler"
 * @scr.reference name="MessagingService"
 *                interface="org.sakaiproject.kernel.api.message.MessagingService"
 *                bind="bindMessagingService" unbind="unbindMessagingService"
 * @scr.reference interface="org.apache.sling.jcr.api.SlingRepository"
 *                name="SlingRepository" bind="bindSlingRepository"
 *                unbind="unbindSlingRepository"
 */
public class ChatMessageHandler implements MessageHandler {
  private static final String CHATLOG = "Chatlog";
  private static final Logger LOG = LoggerFactory
      .getLogger(ChatMessageHandler.class);
  private static final String TYPE = MessageConstants.TYPE_CHAT;

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

  public void bindMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  public void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }

  /**
   * Default constructor
   */
  public ChatMessageHandler() {
  }

  /**
   * This method will place the message in the recipients their message store.
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.message.MessageHandler#handle(org.osgi.service.event.Event,
   *      javax.jcr.Node)
   */
  public void handle(Event event, Node originalMessage) {
    try {
      LOG.info("Started handling the message.");

      // Session session = originalMessage.getSession();
      Session session = slingRepository.loginAdministrative(null);

      String fromPath = messagingService
          .getMessagePathFromMessageStore(originalMessage);

      // Get the recipients. (which are comma separated. )
      Property toProp = originalMessage
          .getProperty(MessageConstants.PROP_SAKAI_TO);
      String toVal = toProp.getString();
      String[] rcpts = StringUtils.split(toVal, ",");

      // Copy the message to each user his message store and place it in the
      // inbox.

      String from = originalMessage.getProperty(
          MessageConstants.PROP_SAKAI_FROM).getString();

      if (rcpts != null) {
        for (String rcpt : rcpts) {
          // the path were we want to save messages in.
          String toPath = buildMessagesPath(rcpt, originalMessage);

          LOG.info("Writing {} to {}", fromPath, toPath);

          // Copy the node into the user his folder.
          JcrUtils.deepGetOrCreateNode(session, toPath);
          session.save();

          /*
           * This gives PathNotFoundExceptions... Workspace workspace =
           * session.getWorkspace(); workspace.copy(originalMessage.getPath(),
           * toPath);
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
          n.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
              MessageConstants.BOX_INBOX);
          n.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
              MessageConstants.STATE_NOTIFIED);
          n.save();

          // Now save this message to the logs.
          saveChatMessageInLog(from, rcpt, originalMessage, session);
          saveChatMessageInLog(rcpt, from, originalMessage, session);
        }
      }

    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  /**
   * Will save a chat message to the log.
   * 
   * @param user
   * @param message
   */
  private void saveChatMessageInLog(String from, String to, Node message, Session session) {
    // TODO
    try {
      Calendar cal = Calendar.getInstance();

      String user1 = (from.compareTo(to) > 0) ? to : from;
      String user2 = (user1.equals(to)) ? from : to;

      // Construct the path to the message we want to save.
      String usersMessageStore = PathUtils
          .toInternalHashedPath(PersonalConstants._USER_PRIVATE, to,
              MessageConstants.FOLDER_MESSAGES);

      // The path from the message store to the message.
      String pathToLogMessage = "/" + MessageConstants.FOLDER_CHATS + "/"
          + cal.get(Calendar.YEAR) + "/" + cal.get(Calendar.MONTH) + "/"
          + cal.get(Calendar.DAY_OF_MONTH) + "/";

      // The filename for this message.
      String logMessage = user1 + "_" + user2;
      String fullPath = usersMessageStore + pathToLogMessage + logMessage;

      Node nodeMessage = null;
      // Get this message.
      if (session.itemExists(fullPath)) {
        nodeMessage = (Node) session.getItem(fullPath);
      } else {
        LOG.info("The log message did not exist yet. Start creating one.");

        // This path does not exist. Create it.
        // createPathToFile(s, to, pathToLogMessage + logMessage);
        nodeMessage = JcrUtils.deepGetOrCreateNode(session, fullPath);

        // Set the basic properties.
        nodeMessage.setProperty(
            JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            MessageConstants.PROP_SAKAI_MESSAGE);
        nodeMessage.setProperty(MessageConstants.PROP_SAKAI_FROM, from);
        nodeMessage.setProperty(MessageConstants.PROP_SAKAI_TO, to);
        nodeMessage.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
            MessageConstants.BOX_INBOX);
        nodeMessage.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
            MessageConstants.STATE_NOTIFIED);
        nodeMessage.setProperty(MessageConstants.PROP_SAKAI_TYPE,
            MessageConstants.TYPE_INTERNAL);
        nodeMessage.setProperty(MessageConstants.PROP_SAKAI_SUBJECT, CHATLOG);

        // This array will hold all the chat messages.
        JSONArray arr = new JSONArray();
        nodeMessage.setProperty(MessageConstants.PROP_SAKAI_BODY, arr
            .toString());
        session.save();
      }

      LOG.info("Appending the information to the log.");
      // Append the current chat to the message
      JSONObject obj = new JSONObject();
      obj.put("from", from);
      obj.put("to", to);
      obj.put("message", message.getProperty(MessageConstants.PROP_SAKAI_BODY)
          .getString());
      /*
       * obj .put("date", message.getProperty(JCRConstants.JCR_CREATED)
       * .getString());
       */
      JSONArray arr = new JSONArray(nodeMessage.getProperty(
          MessageConstants.PROP_SAKAI_BODY).getString());
      arr.put(obj);

      nodeMessage.setProperty(MessageConstants.PROP_SAKAI_BODY, arr.toString());
      nodeMessage.save();

    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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

  /**
   * Constructs a path along the lines of
   * /_user/private/A1/B2/C3/user/messages/cf
   * /df/e1/d4/a4d324a6s5d4a6s4d6a5s4d56as4d
   * 
   * @param user
   * @return
   * @throws RepositoryException
   * @throws AccessDeniedException
   * @throws ItemNotFoundException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
  private String buildMessagesPath(String user, Node originalMessage)
      throws ValueFormatException, PathNotFoundException,
      ItemNotFoundException, AccessDeniedException, RepositoryException {
    String path = PathUtils
        .toInternalHashedPath(PersonalConstants._USER_PRIVATE, user,
            MessageConstants.FOLDER_MESSAGES);
    path += messagingService.getMessagePathFromMessageStore(originalMessage);
    return path;
  }

  public void bindUserFactoryService(UserFactoryService userFactory) {
    // TODO Auto-generated method stub

  }
}
