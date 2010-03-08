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
package org.sakaiproject.nakamura.events;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.activemq.ConnectionFactoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * Bridge to send OSGi events onto a JMS topic.
 */
@Component(label = "%bridge.name", description = "%bridge.description", metatype = true, immediate = true)
@Service
public class OsgiJmsBridge implements EventHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(OsgiJmsBridge.class);

  @Property(value = "*", propertyPrivate = true)
  static final String TOPICS = EventConstants.EVENT_TOPIC;

  @Property(value = "sakai.event.bridge")
  static final String CONNECTION_CLIENT_ID = "bridge.connectionClientId";

  @Property(boolValue = false, propertyPrivate = true)
  static final String SESSION_TRANSACTED = "bridge.sessionTransacted";

  @Property(intValue = Session.AUTO_ACKNOWLEDGE, propertyPrivate = true)
  static final String ACKNOWLEDGE_MODE = "bridge.acknowledgeMode";

  @Reference
  private ConnectionFactoryService connFactoryService;

  private boolean transacted;
  private String connectionClientId;
  private int acknowledgeMode;

  /**
   * Default constructor.
   */
  public OsgiJmsBridge() {
  }

  /**
   * Testing constructor to pass in a mocked connection factory.
   * 
   * @param connFactory
   *          Connection factory to use when activating.
   * @param brokerUrl
   *          Broker url to use for comparison. This has to match what is passed in
   *          through the context properties or a new connection factory will be created
   *          not using the one passed in.
   */
  protected OsgiJmsBridge(ConnectionFactoryService connFactoryService) {
    this.connFactoryService = connFactoryService;
  }

  /**
   * Called by the OSGi container to activate this component.
   * 
   * @param ctx
   */
  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext ctx) {
    Dictionary props = ctx.getProperties();

    transacted = (Boolean) props.get(SESSION_TRANSACTED);
    acknowledgeMode = (Integer) props.get(ACKNOWLEDGE_MODE);
    connectionClientId = (String) props.get(CONNECTION_CLIENT_ID);

    LOGGER.info("Session Transacted: {}, Acknowledge Mode: {}, " + "Client ID: {}",
        new Object[] { transacted, acknowledgeMode, connectionClientId });
  }

  /**
   * Called by the OSGi container to deactivate this component.
   * 
   * @param ctx
   */
  protected void deactivate(ComponentContext ctx) {
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
   */
  @SuppressWarnings("unchecked")
  public void handleEvent(Event event) {
    LOGGER.trace("Receiving event");
    Connection conn = null;

    LOGGER.debug("Processing event {}", event);
    Session clientSession = null;
    try {
      
      conn = connFactoryService.getDefaultPooledConnectionFactory().createConnection();
      // conn.setClientID(connectionClientId);
      // post to JMS
      // Sessions are not thread safe, so we need to create and destroy a session, for
      // sending.
      clientSession = conn.createSession(transacted, acknowledgeMode);

      Topic emailTopic = clientSession.createTopic(event.getTopic());
      MessageProducer client = clientSession.createProducer(emailTopic);
      Message msg = clientSession.createMessage();
      // may need to set a delivery mode eg persistent for certain types of messages.
      // this should be specified in the OSGi event.
      msg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
      msg.setJMSType(event.getTopic());
      for (String name : event.getPropertyNames()) {
        Object obj = event.getProperty(name);
        // "Only objectified primitive objects, String, Map and List types are
        // allowed" as stated by an exception when putting something into the
        // message that was not of one of these types.
        if (obj instanceof Byte || obj instanceof Boolean || obj instanceof Character
            || obj instanceof Number || obj instanceof Map || obj instanceof String
            || obj instanceof List) {
          msg.setObjectProperty(name, obj);
        }
      }

      client.send(msg);
    } catch (JMSException e) {
      LOGGER.error(e.getMessage(), e);
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
      } catch (Exception e) {
        LOGGER.error(e.getMessage(), e);
      }
      try {
        if (clientSession != null) {
          clientSession.close();
        }
      } catch (Exception e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
  }

}
