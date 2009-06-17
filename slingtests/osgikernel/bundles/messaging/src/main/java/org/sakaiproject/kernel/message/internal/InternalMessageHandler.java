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

package org.sakaiproject.kernel.message.internal;

import static org.sakaiproject.kernel.api.message.MessageConstants.SAKAI_MESSAGESTORE_RT;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.Event;
import org.sakaiproject.kernel.api.message.Message;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageHandler;
import org.sakaiproject.kernel.api.user.UserFactoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Handler for messages that are sent locally and intended for local delivery.
 * Needs to be started immediately to make sure it registers with JCR as soon as
 * possible.
 * 
 * @scr.component label="InternalMessageHandler"
 *                description="Handler for internally delivered messages."
 *                immediate="true"
 */
public class InternalMessageHandler implements MessageHandler {
  private static final Logger LOG = LoggerFactory
      .getLogger(InternalMessageHandler.class);
  private static final String TYPE = Message.Type.INTERNAL.toString();

  private UserFactoryService userFactory;

  /**
   * 
   * @param userFactory
   */
  public void bindUserFactoryService(UserFactoryService userFactory) {
    System.out.println("Bound userFactoryService to InternalMessageHandler.");
    this.userFactory = userFactory;
  }

  /**
   * 
   * @param userFactory
   */
  public void unbindUserFactoryService(UserFactoryService userFactory) {
    this.userFactory = null;
  }

  /**
   * Default constructor
   */
  public InternalMessageHandler() {
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
      LOG.info("Started handle in InternalMessageHandler.");
      String filePath = event.getProperty(MessageConstants.EVENT_LOCATION)
          .toString();

      Session session = originalMessage.getSession();

      // Get the recipients. (which are comma separated. )
      Property toProp = originalMessage
          .getProperty(Message.Field.TO.toString());
      String toVal = toProp.getString();
      String[] rcpts = StringUtils.split(toVal, ",");

      // Copy the message to each user his message store and place it in the
      // inbox.
      if (rcpts != null) {
        for (String rcpt : rcpts) {
          String msgPath = buildMessagesPath(rcpt, originalMessage);
          LOG.info("Writing {} to {}", filePath, msgPath);
          createPathToFile(session, rcpt,
              getPartAfterMessageStore(originalMessage));
          // workspace.copy(filePath, msgPath);
          Node n = (Node) session.getItem(msgPath);
          PropertyIterator pi = originalMessage.getProperties();
          while (pi.hasNext()) {
            Property p = pi.nextProperty();
            if (!p.getName().contains("jcr:")) {
              n.setProperty(p.getName(), p.getValue());
            }
          }
          n.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
              MessageConstants.BOX_INBOX);
          n.save();
        }
      }
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  /**
   * This method will create all the nodes till it reaches the end.
   * 
   * @param msgPath
   */
  private void createPathToFile(Session session, String user,
      String pathToMessageFromStore) {
    try {
      String path = userFactory.getUserPrivatePath(user) + "/"
          + MessageConstants.FOLDER_MESSAGES;
      // Create each dir to the message path.
      Node n = (Node) session.getItem(path);
      String[] dirs = pathToMessageFromStore.split("/");
      String createdPath = path;
      for (String dir : dirs) {
        if (dir.length() > 0) {
          createdPath += "/" + dir;
          if (!session.itemExists(createdPath)) {
            // This directory doesn't exist yet. Make it.
            n.addNode(dir);
            System.out.println("Created path for: " + createdPath);
          }
          n = n.getNode(dir);
        }
      }
      session.save();

    } catch (PathNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RepositoryException e) {
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
   * /_private/A1/B2/C3/user/messages/cf/df/e1/d4/a4d324a6s5d4a6s4d6a5s4d56as4d
   * 
   * @param user
   * @return
   */
  private String buildMessagesPath(String user, Node originalMessage) {
    String path = userFactory.getUserPrivatePath(user);
    path += "/" + MessageConstants.FOLDER_MESSAGES;
    path += getPartAfterMessageStore(originalMessage);
    return path;
  }

  /**
   * Gets the part after the messagestore:
   * /cf/df/e1/d4/a4d324a6s5d4a6s4d6a5s4d56as4d
   * 
   * @param n
   * @return
   */
  private String getPartAfterMessageStore(Node n) {
    String msgPath = "";
    try {
      while (!"/".equals(n.getPath())) {
        if (n.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            && SAKAI_MESSAGESTORE_RT.equals(n.getProperty(
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
          return msgPath;
        }
        msgPath = "/" + n.getName() + msgPath;
        n = n.getParent();
      }
    } catch (ValueFormatException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (PathNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ItemNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (AccessDeniedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return msgPath;
  }
}
