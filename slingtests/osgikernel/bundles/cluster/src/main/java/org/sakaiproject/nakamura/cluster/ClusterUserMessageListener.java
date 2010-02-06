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
package org.sakaiproject.nakamura.cluster;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.activemq.ConnectionFactoryService;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;

/**
 *
 */
@Component(immediate = true)
public class ClusterUserMessageListener implements MessageListener {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ClusterUserMessageListener.class);
  @Property(value = "vm://localhost:61616")
  public static final String BROKER_URL = "cluster.jms.brokerUrl";

  @Reference
  protected ConnectionFactoryService connFactoryService;
  @Reference
  private ClusterTrackingService clusterTrackingService;

  private String brokerUrl;
  private ConnectionFactory connectionFactory;
  private Connection connection;
  private ClusterTrackingServiceImpl clusterTrackingServiceImpl;

  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext componentContext) {
    Dictionary props = componentContext.getProperties();
    String _brokerUrl = (String) props.get(BROKER_URL);

    String serverId = clusterTrackingService.getCurrentServerId();

    clusterTrackingServiceImpl = (ClusterTrackingServiceImpl) clusterTrackingService;

    try {
      boolean urlEmpty = brokerUrl == null || brokerUrl.trim().length() == 0;
      if (!urlEmpty) {
        if (diff(brokerUrl, _brokerUrl)) {
          LOGGER.info("Creating a new ActiveMQ Connection Factory");
          connectionFactory = connFactoryService.createFactory(_brokerUrl);
        }

        if (connectionFactory != null) {
          connection = connectionFactory.createConnection();
          Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
          Topic dest = session.createTopic(ClusterTrackingService.EVENT_PING_CLUSTER_USER
              + "/" + serverId);
          MessageConsumer consumer = session.createConsumer(dest);
          consumer.setMessageListener(this);
          connection.start();
        }
      } else {
        LOGGER.error("Cannot create JMS connection factory with an empty URL.");
      }
      brokerUrl = _brokerUrl;
    } catch (JMSException e) {
      LOGGER.error(e.getMessage(), e);
      if (connection != null) {
        try {
          connection.close();
        } catch (JMSException e1) {
        }
      }
    }

  }

  protected void deactivate(ComponentContext ctx) {
    if (connection != null) {
      try {
        connection.close();
      } catch (JMSException e) {
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
   */
  public void onMessage(Message message) {
    try {
      String fromServer = message
          .getStringProperty(ClusterTrackingService.EVENT_FROM_SERVER);
      String toServer = message.getStringProperty(ClusterTrackingService.EVENT_TO_SERVER);
      String trackingCookie = message
          .getStringProperty(ClusterTrackingService.EVENT_TRACKING_COOKIE);
      String remoteUser = message.getStringProperty(ClusterTrackingService.EVENT_USER);
      LOGGER.info(
          "Started handling cluster user jms message. from:{} to:{} cookie:{} user:{}",
          new Object[] { fromServer, toServer, trackingCookie, remoteUser });
      clusterTrackingServiceImpl.pingTracking(trackingCookie, remoteUser, false);
    } catch (PingRemoteTrackingFailedException e) {
      LOGGER.error(e.getMessage());
    } catch (JMSException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  /**
   * Determine if there is a difference between two objects.
   *
   * @param obj1
   * @param obj2
   * @return true if the objects are different (only one is null or !obj1.equals(obj2)).
   *         false otherwise.
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
