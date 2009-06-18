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
package org.sakaiprojet.kernel.events;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * Bridge to pipe OSGi events into a JMS topic.
 *
 * @scr.component
 * @scr.service
 */
public class OsgiJmsBridge implements EventHandler {
  private static final Logger LOG = LoggerFactory.getLogger(OsgiJmsBridge.class);

  /** @scr.property value="*" */
  static final String TOPICS = EventConstants.EVENT_TOPIC;

  /** @scr.property value="sakai.event.bridge" */
  static final String EVENT_JMS_TOPIC = "eventJmsTopic";

  /** @scr.property value="false" */
  static final String SESSION_TRANSACTED = "sessionTransacted";

  /** @scr.property valueRef="javax.jms.Session.AUTO_ACKNOWLEDGE" */
  static final String ACKNOWLEDGE_MODE = "acknowledgeMode";

  private ConnectionFactory connFactory;
  private String eventTopicName;
  private boolean transacted;
  private int acknowledgeMode;

  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext ctx) {
    Dictionary props = ctx.getProperties();
    eventTopicName = (String) props.get(EVENT_JMS_TOPIC);
    transacted = Boolean.parseBoolean((String) props.get(SESSION_TRANSACTED));
    acknowledgeMode = Integer.parseInt((String) props.get(ACKNOWLEDGE_MODE));
  }

  public void handleEvent(Event event) {
    Connection conn = null;
    try {
      // post to JMS
      conn = connFactory.createConnection();
      conn.setClientID("sakai.event.bridge");
      Session clientSession = conn.createSession(transacted, acknowledgeMode);
      Destination emailTopic = clientSession.createTopic(eventTopicName);
      MessageProducer client = clientSession.createProducer(emailTopic);
      MapMessage msg = clientSession.createMapMessage();
      msg.setJMSType(event.getTopic());
      for (String name : event.getPropertyNames()) {
        Object obj = event.getProperty(name);
        msg.setObject(name, obj);
      }
      client.send(msg);
    } catch (JMSException e) {
      LOG.error("Unable to cross post event from OSGi to JMS: " + e.getMessage(), e);
    } finally {
      if (conn != null) {
        try {
          conn.close();
        } catch (JMSException e) {
          LOG.error("Unable to close JMS connection: " + e.getMessage(), e);
        }
      }
    }
  }
}
