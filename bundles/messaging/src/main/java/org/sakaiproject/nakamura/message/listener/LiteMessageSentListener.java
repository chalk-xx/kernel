package org.sakaiproject.nakamura.message.listener;

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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.lite.*;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(inherit = true, label = "%sakai-event.name", immediate = true)
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Event Handler Listening to Pending Messages Events."),
    @Property(name = "event.topics", value = "org/sakaiproject/nakamura/message/pending")})
@Reference(name = "MessageTransport", referenceInterface = LiteMessageTransport.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "addTransport", unbind = "removeTransport")
public class LiteMessageSentListener implements EventHandler {
  private static final Logger LOG = LoggerFactory.getLogger(LiteMessageSentListener.class);

  /**
   * This will contain all the transports.
   */
  private Map<LiteMessageTransport, LiteMessageTransport> transports =
      new ConcurrentHashMap<LiteMessageTransport, LiteMessageTransport>();

  @Reference
  private transient LiteMessageRouterManager messageRouterManager;

  @Reference
  private transient Repository contentRepository;

  /**
   * Default constructor for use by OSGi container.
   */
  public LiteMessageSentListener() {
  }

  /**
   * Parameterized constructor for use in unit tests.
   *
   * @param messageRouterManager
   * @param contentRepository
   */
  LiteMessageSentListener(LiteMessageRouterManager messageRouterManager,
      Repository contentRepository) {
    this.messageRouterManager = messageRouterManager;
    this.contentRepository = contentRepository;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
   */
  public void handleEvent(Event event) {

    // Get the message
    // get the content item, call up the appropriate handler and pass off based on
    // message type
    LOG.debug("handleEvent called");
    Session session = null;
    try {
      session = contentRepository.loginAdministrative();
      String path = (String) event.getProperty(MessageConstants.EVENT_LOCATION);
      Content message = session.getContentManager().get(path);
      String resourceType = (String) message.getProperty(
          JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
      if (MessageConstants.SAKAI_MESSAGE_RT.equals(resourceType)) {

        MessageRoutes routes = messageRouterManager.getMessageRouting(message);

        for (LiteMessageTransport transport : transports.values()) {
          transport.send(routes, event, message);
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

  /**
   * @param transport
   */
  protected void removeTransport(LiteMessageTransport transport) {
    transports.remove(transport);
  }

  /**
   * @param transport
   */
  protected void addTransport(LiteMessageTransport transport) {
    transports.put(transport,transport);
  }


}
