package org.sakaiproject.kernel.message.listener;

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

import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.kernel.api.message.Message;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageHandler;
import org.sakaiproject.kernel.api.user.UserFactoryService;
import org.sakaiproject.kernel.message.internal.InternalMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * 
 * @scr.component inherit="true" label="%sakai-event.name"
 * @scr.service interface="org.osgi.service.event.EventHandler"
 * @scr.property name="service.description"
 *               value="Event Handler Listening to Pending Messages Events"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="event.topics"
 *               value="org/sakaiproject/kernel/message/pending"
 *               
 * @scr.reference name="UserFactoryService" interface="org.sakaiproject.kernel.api.user.UserFactoryService"
 */
public class MessageSentListener implements EventHandler {
  private static final Logger LOG = LoggerFactory
      .getLogger(MessageSentListener.class);


  /**
   * @scr.reference 
   *                    interface="org.sakaiproject.kernel.api.message.MessageHandler"
   *                    policy="dynamic" cardinality="0..n" bind="addHandler"
   *                    unbind="removeHandler"
   */
  private Set<MessageHandler> handlers = new HashSet<MessageHandler>();

  /**
   * Binder for adding message handlers.
   * 
   * @param handler
   */
  protected void addHandler(MessageHandler handler) {
    System.out.println("Added a handler to MessageSentListener : " + handler);
    handlers.add(handler);
  }

  /**
   * Unbinder for removing message handlers.
   * 
   * @param handler
   */
  protected void removeHandler(MessageHandler handler) {
    handlers.remove(handler);
  }
  
  private UserFactoryService userFactory;
  
  /**
   * 
   * @param userFactory
   */
  public void bindUserFactoryService(UserFactoryService userFactory) {
    System.out.println("Bound userFactoryService to MessageSentListener.");
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
   * {@inheritDoc}
   * 
   * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
   */
  public void handleEvent(Event event) {
    System.out.println("Handled event in MessageSentListener - " + event);

    // Get the message
    // get the node, call up the appropriate handler and pass off based on
    // message type

    try {
      Node n = (Node) event.getProperty(MessageConstants.EVENT_LOCATION);
      String resourceType = n.getProperty(
          JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString();
      if (resourceType.equals(MessageConstants.PROP_SAKAI_MESSAGE)) {
        Property msgTypeProp = n.getProperty(Message.Field.TYPE.toString());
        String msgType = msgTypeProp.getString();
        LOG.info("Got a message with type: " + msgType);
        boolean handled = false;
        if (msgType != null && handlers != null) {
          InternalMessageHandler handler = new InternalMessageHandler();
          handler.bindUserFactoryService(userFactory);
          handler.handle(event, n);
          
          /*
          for (MessageHandler handler : handlers) {
            if (msgType.equalsIgnoreCase(handler.getType())) {
              handler.handle(event, n);
              handled = true;
            }
          }*/
        }
        if (!handled) {
          LOG.warn("No handler found for message type [{}]", msgType);
        }
      }
    } catch (LoginException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

}
