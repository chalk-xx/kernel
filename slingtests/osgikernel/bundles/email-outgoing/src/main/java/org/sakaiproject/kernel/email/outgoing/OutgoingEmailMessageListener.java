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
import org.apache.commons.mail.MultiPartEmail;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.JobContext;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
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

@Component(label = "%email.out.name", description = "%email.out.description", immediate = true, metatype = true)
public class OutgoingEmailMessageListener implements MessageListener {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OutgoingEmailMessageListener.class);

  @Property(value = "tcp://localhost:61616")
  public static final String BROKER_URL = "email.out.brokerUrl";
  @Property(value = "localhost")
  private static final String SMTP_SERVER = "sakai.smtp.server";
  @Property(intValue = 8025, label = "%sakai.smtp.port.name")
  private static final String SMTP_PORT = "sakai.smtp.port";
  @Property(intValue = 240)
  private static final String MAX_RETRIES = "sakai.email.maxRetries";
  @Property(intValue = 30)
  private static final String RETRY_INTERVAL = "sakai.email.retryIntervalMinutes";

  @Reference
  protected SlingRepository repository;
  @Reference
  protected JcrResourceResolverFactory jcrResourceResolverFactory;
  @Reference
  protected Scheduler scheduler;
  @Reference
  protected EventAdmin eventAdmin;

  protected static final String QUEUE_NAME = "sakai.email.outgoing";
  protected static final String NODE_PATH_PROPERTY = "nodePath";

  private Connection connection = null;
  private Session session = null;
  private MessageConsumer consumer = null;
  private String brokerUrl;
  private ConnectionFactory connectionFactory = null;
  private Queue dest = null;
  private Integer maxRetries;
  private Integer smtpPort;
  private String smtpServer;

  private Integer retryInterval;

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
            MultiPartEmail email;
            try {
              email = constructMessage(messageNode);

              email.setSmtpPort(smtpPort);
              email.setHostName(smtpServer);

              email.send();
            } catch (EmailException e) {
              setError(messageNode, e.getMessage());
              // Get the SMTP error code
              // There has to be a better way to do this
              if (e.getCause() != null && e.getCause().getMessage() != null) {
                String smtpError = e.getCause().getMessage().trim();
                try {
                  int errorCode = Integer.parseInt(smtpError.substring(0, 3));
                  // All retry-able SMTP errors should have codes starting with 4
                  scheduleRetry(errorCode, messageNode);
                } catch (NumberFormatException nfe) {
                  // smtpError didn't start with an error code, let's dig for it
                  String searchFor = "response:";
                  int rindex = smtpError.indexOf(searchFor);
                  if (rindex > -1 && (rindex + searchFor.length()) < smtpError.length()) {
                    int errorCode = Integer.parseInt(smtpError.substring(searchFor
                        .length(), searchFor.length() + 3));
                    scheduleRetry(errorCode, messageNode);
                  }
                }
              }
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

  private MultiPartEmail constructMessage(Node messageNode) throws EmailException,
      RepositoryException, PathNotFoundException, ValueFormatException {
    MultiPartEmail email = new MultiPartEmail();
    try {
      email.addTo(messageNode.getProperty(MessageConstants.PROP_SAKAI_TO).getString());
    } catch (ValueFormatException e) {
      for (Value address : messageNode.getProperty(MessageConstants.PROP_SAKAI_TO)
          .getValues()) {
        email.addTo(address.getString());
      }
    }

    email.setFrom(messageNode.getProperty(MessageConstants.PROP_SAKAI_FROM).getString());

    if (messageNode.hasProperty(MessageConstants.PROP_SAKAI_BODY)) {
      email.setMsg(messageNode.getProperty(MessageConstants.PROP_SAKAI_BODY).getString());
    }

    if (messageNode.hasProperty(MessageConstants.PROP_SAKAI_SUBJECT)) {
      email.setSubject(messageNode.getProperty(MessageConstants.PROP_SAKAI_SUBJECT)
          .getString());
    }

    if (messageNode.hasNodes()) {
      NodeIterator ni = messageNode.getNodes();
      while (ni.hasNext()) {
        Node childNode = ni.nextNode();
        String description = null;
        if (childNode.hasProperty(MessageConstants.PROP_SAKAI_ATTACHMENT_DESCRIPTION)) {
          description = childNode.getProperty(
              MessageConstants.PROP_SAKAI_ATTACHMENT_DESCRIPTION).getString();
        }
        JcrEmailDataSource ds = new JcrEmailDataSource(childNode);
        email.attach(ds, childNode.getName(), description);
      }
    }

    return email;
  }

  private void scheduleRetry(int errorCode, Node messageNode) throws RepositoryException {
    // All retry-able SMTP errors should have codes starting with 4
    if ((int) (errorCode / 100) == 4) {
      long retryCount = 0;
      if (messageNode.hasProperty(MessageConstants.PROP_SAKAI_RETRY_COUNT)) {
        retryCount = messageNode.getProperty(MessageConstants.PROP_SAKAI_RETRY_COUNT)
            .getLong();
      }

      if (retryCount < maxRetries) {
        Job job = new Job() {

          public void execute(JobContext jc) {
            Map<String, Serializable> config = jc.getConfiguration();
            Properties eventProps = new Properties();
            eventProps.put(NODE_PATH_PROPERTY, config.get(NODE_PATH_PROPERTY));

            Event retryEvent = new Event(QUEUE_NAME, eventProps);
            eventAdmin.postEvent(retryEvent);

          }
        };

        HashMap<String, Serializable> jobConfig = new HashMap<String, Serializable>();
        jobConfig.put(NODE_PATH_PROPERTY, messageNode.getPath());

        int retryIntervalMillis = retryInterval * 60000;
        Date nextTry = new Date(System.currentTimeMillis() + (retryIntervalMillis));

        try {
          scheduler.fireJobAt(null, job, jobConfig, nextTry);
        } catch (Exception e) {
          LOGGER.error(e.getMessage(), e);
        }
      } else {
        setError(messageNode, "Unable to send message, exhausted SMTP retries.");
      }
    } else {
      LOGGER.warn("Not scheduling a retry for error code not of the form 4xx.");
    }
  }

  protected void activate(ComponentContext ctx) {
    @SuppressWarnings("unchecked")
    Dictionary props = ctx.getProperties();

    Integer _maxRetries = (Integer) props.get(MAX_RETRIES);
    if (_maxRetries != null) {
      if (diff(maxRetries, _maxRetries)) {
        maxRetries = _maxRetries;
      }
    } else {
      LOGGER.error("Maximum times to retry messages not set.");
    }

    Integer _retryInterval = (Integer) props.get(RETRY_INTERVAL);
    if (_retryInterval != null) {
      if (diff(_retryInterval, retryInterval)) {
        retryInterval = _retryInterval;
      }
    } else {
      LOGGER.error("SMTP retry interval not set.");
    }

    if (maxRetries * retryInterval < 4320 /* minutes in 3 days */) {
      LOGGER.warn("SMTP retry window is very short.");
    }

    Integer _smtpPort = (Integer) props.get(SMTP_PORT);
    boolean validPort = _smtpPort != null && _smtpPort >= 0 && _smtpPort <= 65535;
    if (validPort) {
      if (diff(smtpPort, _smtpPort)) {
        smtpPort = _smtpPort;
      }
    } else {
      LOGGER.error("Invalid port set for SMTP");
    }

    String _smtpServer = (String) props.get(SMTP_SERVER);
    boolean smtpServerEmpty = _smtpServer == null || _smtpServer.trim().length() == 0;
    if (!smtpServerEmpty) {
      if (diff(smtpServer, _smtpServer)) {
        smtpServer = _smtpServer;
      }
    } else {
      LOGGER.error("No SMTP server set");
    }

    String _brokerUrl = (String) props.get(BROKER_URL);

    try {
      boolean urlEmpty = _brokerUrl == null || _brokerUrl.trim().length() == 0;
      if (!urlEmpty) {
        if (diff(brokerUrl, _brokerUrl)) {
          LOGGER.info("Creating a new ActiveMQ Connection Factory");
          connectionFactory = new ActiveMQConnectionFactory(_brokerUrl);
        }
      } else {
        LOGGER.error("Cannot create JMS connection factory with an empty URL.");
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
