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
package org.sakaiproject.kernel.email.outgoing;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;

@Component(label = "%email.out.name", description = "%email.out.description", immediate = true)
public class OutgoingEmailMessageListener implements MessageListener {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OutgoingEmailMessageListener.class);

  @Property(value = {"tcp://localhost:61616"})
  static final String BROKER_URL = "email.out.brokerUrl";
  @Property(value = {"sakai.email.outgoing"})
  static final String QUEUE_NAME = "email.out.queueName";
  @Property(value = {"sakai.smtp.server"})
  static final String SMTP_SERVER = "localhost";
  @Property(value = {"sakai.smtp.port"})
  static final int SMTP_PORT = 8025;

  @Reference
  protected SlingRepository repository;
  @Reference
  protected JcrResourceResolverFactory jcrResourceResolverFactory;

  private static final String NODE_PATH_PROPERTY = "nodePath";

  private Connection connection = null;
  private Session session = null;
  private MessageConsumer consumer = null;
  private String brokerUrl;
  private ConnectionFactory connectionFactory = null;
  private Queue dest = null;

  public void onMessage(Message message) {
    try {
      String nodePath = message.getStringProperty(NODE_PATH_PROPERTY);

      javax.jcr.Session adminSession = repository.loginAdministrative(null);
      ResourceResolver resolver = jcrResourceResolverFactory
          .getResourceResolver(adminSession);

      Node messageNode = resolver.getResource(nodePath).adaptTo(Node.class);

      // validate the message
      if (messageNode != null) {
        if (messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)
            && MessageConstants.BOX_OUTBOX.equals(messageNode.getProperty(
                MessageConstants.PROP_SAKAI_MESSAGEBOX).getString())) {
          if (messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)) {
            // We're retrying this message, so clear the errors
            messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR,
                (String) null);
          }
          if (messageNode.hasProperty(MessageConstants.PROP_SAKAI_TO)
              && messageNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)) {
            // make a commons-email message from the message
            SimpleEmail email = new SimpleEmail();
            try {
              try {
                email.addTo(messageNode.getProperty(MessageConstants.PROP_SAKAI_TO)
                    .getString());
              } catch (ValueFormatException e) {
                for (Value address : messageNode.getProperty(
                    MessageConstants.PROP_SAKAI_TO).getValues()) {
                  email.addTo(address.getString());
                }
              }

              email.setFrom(messageNode.getProperty(MessageConstants.PROP_SAKAI_FROM)
                  .getString());

              if (messageNode.hasProperty(MessageConstants.PROP_SAKAI_BODY)) {
                email.setMsg(messageNode.getProperty(MessageConstants.PROP_SAKAI_BODY)
                    .getString());
              }

              if (messageNode.hasProperty(MessageConstants.PROP_SAKAI_SUBJECT)) {
                email.setSubject(messageNode.getProperty(
                    MessageConstants.PROP_SAKAI_SUBJECT).getString());
              }

              email.setSmtpPort(SMTP_PORT);
              email.setHostName(SMTP_SERVER);

              email.send();
            } catch (EmailException e) {
              setError(messageNode, e.getMessage());
            }
          } else {
            setError(messageNode, "Message must have a to and from set");
          }
        } else {
          setError(messageNode, "Not an outbox");
        }
        if (!messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)) {
          messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
              MessageConstants.BOX_SENT);
        }
      }
    } catch (JMSException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  protected void activate(ComponentContext ctx) {
    @SuppressWarnings("unchecked")
    Dictionary props = ctx.getProperties();
    String _brokerUrl = (String) props.get(BROKER_URL);

    try {
      boolean urlEmpty = _brokerUrl == null || _brokerUrl.trim().length() == 0;
      if (!urlEmpty) {
        if (diff(brokerUrl, _brokerUrl)) {
          LOGGER.info("Creating a new ActiveMQ Connection Factory");
          connectionFactory = new ActiveMQConnectionFactory(_brokerUrl);
        }
      } else {
        LOGGER.warn("Cannot create JMS connection factory with an empty URL.");
      }

      brokerUrl = _brokerUrl;
      connection = connectionFactory.createConnection();
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      dest = session.createQueue(QUEUE_NAME);
      consumer = session.createConsumer(dest);
      consumer.setMessageListener(this);
      connection.start();

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

  private void setError(Node node, String error) throws RepositoryException {
    node.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, error);
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

  protected void bindRepository(SlingRepository repository) {
    this.repository = repository;
  }

  protected void bindJcrResourceResolverFactory(
      JcrResourceResolverFactory jcrResourceResolverFactory) {
    this.jcrResourceResolverFactory = jcrResourceResolverFactory;
  }
}
