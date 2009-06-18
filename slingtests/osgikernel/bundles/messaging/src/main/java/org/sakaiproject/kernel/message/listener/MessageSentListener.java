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
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageHandler;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.message.internal.InternalMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * 
 * @scr.component inherit="true" label="%sakai-event.name" immediate="true"
 * @scr.service interface="org.osgi.service.event.EventHandler"
 * @scr.property name="service.description"
 *               value="Event Handler Listening to Pending Messages Events"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="event.topics"
 *               value="org/sakaiproject/kernel/message/pending"
 * @scr.reference name="MessageHandler"
 *                interface="org.sakaiproject.kernel.api.message.MessageHandler"
 *                policy="dynamic" cardinality="0..n" bind="bindHandler"
 *                unbind="unbindHandler"
 * @scr.reference name="MessagingService" policy="dynamic"
 *                interface="org.sakaiproject.kernel.api.message.MessagingService"
 *                bind="bindMessagingService" unbind="unbindMessagingService"
 */
public class MessageSentListener implements EventHandler {
  private static final Logger LOG = LoggerFactory
      .getLogger(MessageSentListener.class);

  /**
   * This will contain all the handlers we have for every type.
   */
  private Map<String, MessageHandler> handlers = new ConcurrentHashMap<String, MessageHandler>();
  private ComponentContext osgiComponentContext;
  private List<ServiceReference> delayedReferences = new ArrayList<ServiceReference>();
  /**
   * If no handler is found for a message we will fall back to this one.
   */
  private InternalMessageHandler defaultHandler = new InternalMessageHandler();

  /**
   * 
   */

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
        Property msgTypeProp = n.getProperty(MessageConstants.PROP_SAKAI_TYPE);
        String msgType = msgTypeProp.getString();
        LOG.info("The type for this message is {}", msgType);
        boolean handled = false;
        // Find the correct messagehandler for this type of message and let it
        // handle the message..
        if (msgType != null && handlers != null) {
          MessageHandler handler = defaultHandler;
          if (handlers.containsKey(msgType)) {
            LOG.info("Found a message handler for type [{}]", msgType);
            handler = handlers.get(msgType);
          }
          handler.bindMessagingService(messagingService);
          handler.handle(event, n);
          handled = true;
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

  private MessagingService messagingService;

  public void bindMessagingService(MessagingService messagingService) {
    System.out.println("Bound MessageService : " + messagingService);
    this.messagingService = messagingService;
  }

  public void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }

  protected void bindHandler(ServiceReference serviceReference) {
    System.out.println("Binding a serviceReference.");
    synchronized (delayedReferences) {
      if (osgiComponentContext == null) {
        delayedReferences.add(serviceReference);
      } else {
        addHandler(serviceReference);
      }
    }

  }

  protected void unbindHandler(ServiceReference serviceReference) {
    synchronized (delayedReferences) {
      if (osgiComponentContext == null) {
        delayedReferences.remove(serviceReference);
      } else {
        removeHandler(serviceReference);
      }
    }

  }

  /**
   * @param serviceReference
   */
  private void removeHandler(ServiceReference serviceReference) {
    MessageHandler handler = (MessageHandler) osgiComponentContext
        .locateService("MessageHandler", serviceReference);
    handlers.remove(handler.getType());
  }

  /**
   * @param serviceReference
   */
  private void addHandler(ServiceReference serviceReference) {
    MessageHandler handler = (MessageHandler) osgiComponentContext
        .locateService("MessageHandler", serviceReference);
    System.out.println("Binding handler in addHandler - " + handler.getType());
    handlers.put(handler.getType(), handler);
  }

  /**
   * @param componentContext
   */
  protected void activate(ComponentContext componentContext) {

    synchronized (delayedReferences) {
      osgiComponentContext = componentContext;
      for (ServiceReference ref : delayedReferences) {
        addHandler(ref);
      }
      delayedReferences.clear();
    }
  }

  /**
   * @return
   */
  public Collection<MessageHandler> getProcessors() {
    return handlers.values();
  }

}
