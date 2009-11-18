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
package org.sakaiproject.kernel.events;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.kernel.api.activemq.ConnectionFactoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * Bridge to send OSGi events onto a JMS topic.
 */
@Component(label = "%bridge.name", description = "%bridge.description", metatype = true)
@Service
public class OsgiJmsBridge implements EventHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(OsgiJmsBridge.class);

  @Property(value = "*", propertyPrivate = true)
  static final String TOPICS = EventConstants.EVENT_TOPIC;

  @Property(value = "vm://localhost:61616")
  static final String BROKER_URL = "bridge.brokerUrl";

  @Property(value = "sakai.event.bridge")
  static final String CONNECTION_CLIENT_ID = "bridge.connectionClientId";

  @Property(boolValue = false, propertyPrivate = true)
  static final String SESSION_TRANSACTED = "bridge.sessionTransacted";

  @Property(intValue = Session.AUTO_ACKNOWLEDGE, propertyPrivate = true)
  static final String ACKNOWLEDGE_MODE = "bridge.acknowledgeMode";

  @Reference
  private ConnectionFactoryService connFactoryService;

  private ConnectionFactory connFactory;
  private Connection conn;
  private Session clientSession;
  private String brokerUrl;
  private boolean transacted;
  private String connectionClientId;
  private int acknowledgeMode;

  /**
   * Default constructor.
   */
  public OsgiJmsBridge() {
    brokerUrl = System.getProperty("activemq.broker.url");
    if (brokerUrl == null) {
      String brokerProtocol = System.getProperty("activemq.broker.protocol", "vm");
      String brokerHost = System.getProperty("activemq.broker.host", "localhost");
      String brokerPort = System.getProperty("activemq.broker.port", "61616");
      brokerUrl = brokerProtocol + "://" + brokerHost + ":" + brokerPort;
    }
  }

  /**
   * Testing constructor to pass in a mocked connection factory.
   *
   * @param connFactory
   *          Connection factory to use when activating.
   * @param brokerUrl
   *          Broker url to use for comparison. This has to match what is passed
   *          in through the context properties or a new connection factory will
   *          be created not using the one passed in.
   */
  protected OsgiJmsBridge(ConnectionFactoryService connFactoryService) {
    this();
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
    String _brokerUrl = (String) props.get(BROKER_URL);

    LOGGER.debug(
        "Broker URL: {}, Session Transacted: {}, Acknowledge Mode: {}, " + "Client ID: {}",
        new Object[] { _brokerUrl, transacted, acknowledgeMode, connectionClientId });

    boolean urlEmpty = _brokerUrl == null || _brokerUrl.trim().length() == 0;
    if (!urlEmpty) {
      if (diff(brokerUrl, _brokerUrl)) {
        LOGGER.info("Creating a new ActiveMQ Connection Factory");
        connFactory = connFactoryService.createFactory(_brokerUrl);
      }
    }

    if (connFactory == null) {
      String msg = "Couldn't create connection factory with empty broker url.";
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
    brokerUrl = _brokerUrl;

    // may want to consider getting this connection from a pool but activity
    // is expected to be high enough that the connection will be used almost
    // constantly or at least with very little delay between messages.
    try {
      conn = connFactory.createConnection();
      conn.setClientID(connectionClientId);

      clientSession = conn.createSession(transacted, acknowledgeMode);
    } catch (JMSException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Called by the OSGi container to deactivate this component.
   *
   * @param ctx
   */
  protected void deactivate(ComponentContext ctx) {
    if (clientSession != null) {
      try {
        clientSession.close();
      } catch (JMSException e) {
        LOGGER.warn(e.getMessage(), e);
      }
    }
    if (conn != null) {
      try {
        conn.close();
      } catch (JMSException e) {
        LOGGER.warn(e.getMessage(), e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
   */
  @SuppressWarnings("unchecked")
  public void handleEvent(Event event) {
    LOGGER.trace("Receiving event");
    if (connFactory != null) {
      LOGGER.debug("Processing event {}", event);
      try {
        // post to JMS
        Topic emailTopic = clientSession.createTopic(event.getTopic());
        MessageProducer client = clientSession.createProducer(emailTopic);
        Message msg = clientSession.createMessage();
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

        clientSession.run();
        conn.start();
        client.send(msg);
      } catch (JMSException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Determine if there is a difference between two objects.
   *
   * @param obj1
   * @param obj2
   * @return true if the objects are different (only one is null or
   *         !obj1.equals(obj2)). false otherwise.
   */
  private boolean diff(Object obj1, Object obj2) {
    boolean diff = true;

    boolean bothNull = obj1 == null && obj2 == null;
    boolean neitherNull = obj1 != null && obj2 != null;

    if (bothNull || (neitherNull && obj1.equals(obj2))) {
      diff = false;
    }

    return diff;
  }
}
