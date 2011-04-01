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
package org.sakaiproject.nakamura.email.outgoing;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.JobContext;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.activemq.ConnectionFactoryService;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.templates.TemplateService;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;

@Component(immediate = true, metatype = true)
public class LiteOutgoingEmailMessageListener implements MessageListener {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(LiteOutgoingEmailMessageListener.class);

  @Property(value = "localhost")
  private static final String SMTP_SERVER = "sakai.smtp.server";
  @Property(intValue = 25, label = "%sakai.smtp.port.name")
  private static final String SMTP_PORT = "sakai.smtp.port";
  @Property(intValue = 240)
  private static final String MAX_RETRIES = "sakai.email.maxRetries";
  @Property(intValue = 30)
  private static final String RETRY_INTERVAL = "sakai.email.retryIntervalMinutes";

  protected static final String QUEUE_NAME = "org/sakaiproject/nakamura/message/email/outgoing";

  @Reference
  protected SlingRepository repository;
  @Reference
  protected Scheduler scheduler;
  @Reference
  protected EventAdmin eventAdmin;
  @Reference
  protected ConnectionFactoryService connFactoryService;
  @Reference
  protected TemplateService templateService;

  /**
   * If present points to a node
   */
  protected static final String NODE_PATH_PROPERTY = "nodePath";
  /**
   * If present points to a content object.
   */
  public static final String CONTENT_PATH_PROPERTY = "contentPath";

  public static final String RECIPIENTS = "recipients";

  private Connection connection = null;
  private Integer maxRetries;
  private Integer smtpPort;
  private String smtpServer;

  private Integer retryInterval;

  public LiteOutgoingEmailMessageListener() {
  }

  public LiteOutgoingEmailMessageListener(ConnectionFactoryService connFactoryService) {
    this.connFactoryService = connFactoryService;
  }

  @SuppressWarnings("unchecked")
  public void onMessage(Message message) {
    try {
      LOGGER.debug("Started handling email jms message.");

      String nodePath = message.getStringProperty(NODE_PATH_PROPERTY);
      String contentPath = message.getStringProperty(CONTENT_PATH_PROPERTY);
      Object objRcpt = message.getObjectProperty(RECIPIENTS);
      List<String> recipients = null;

      if (objRcpt instanceof List<?>) {
        recipients = (List<String>) objRcpt;
      } else if (objRcpt instanceof String) {
        recipients = new LinkedList<String>();
        String[] rcpts = StringUtils.split((String) objRcpt, ',');
        for (String rcpt : rcpts) {
          recipients.add(rcpt);
        }
      }

      if (contentPath != null && contentPath.length() > 0) {
        javax.jcr.Session adminSession = repository.loginAdministrative(null);
        org.sakaiproject.nakamura.api.lite.Session sparseSession = StorageClientUtils
            .adaptToSession(adminSession);

        try {
          ContentManager contentManager = sparseSession.getContentManager();
          Content messageContent = contentManager.get(contentPath);

          if (objRcpt != null) {
            // validate the message
            if (messageContent != null) {
              if (messageContent.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)
                  && (MessageConstants.BOX_OUTBOX.equals(messageContent
                      .getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)) 
                      || MessageConstants.BOX_PENDING.equals(messageContent.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)))) {
                if (messageContent.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)) {
                  // We're retrying this message, so clear the errors
                  messageContent.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR,
                      (String) null);
                }
                if (messageContent.hasProperty(MessageConstants.PROP_SAKAI_TO)
                    && messageContent.hasProperty(MessageConstants.PROP_SAKAI_FROM)) {
                  // make a commons-email message from the message
                  MultiPartEmail email = null;
                  try {
                    email = constructMessage(messageContent, recipients, adminSession,
                        sparseSession);

                    email.setSmtpPort(smtpPort);
                    email.setHostName(smtpServer);

                    email.send();
                  } catch (EmailException e) {
                    String exMessage = e.getMessage();
                    Throwable cause = e.getCause();

                    setError(messageContent, exMessage);
                    LOGGER.warn("Unable to send email: " + exMessage);

                    // Get the SMTP error code
                    // There has to be a better way to do this
                    boolean rescheduled = false;
                    if (cause != null && cause.getMessage() != null) {
                      String smtpError = cause.getMessage().trim();
                      try {
                        int errorCode = Integer.parseInt(smtpError.substring(0, 3));
                        // All retry-able SMTP errors should have codes starting
                        // with 4
                        scheduleRetry(errorCode, messageContent);
                        rescheduled = true;
                      } catch (NumberFormatException nfe) {
                        // smtpError didn't start with an error code, let's dig for
                        // it
                        String searchFor = "response:";
                        int rindex = smtpError.indexOf(searchFor);
                        if (rindex > -1
                            && (rindex + searchFor.length()) < smtpError.length()) {
                          int errorCode = Integer.parseInt(smtpError.substring(
                              searchFor.length(), searchFor.length() + 3));
                          scheduleRetry(errorCode, messageContent);
                          rescheduled = true;
                        }
                      }
                    }
                    if (rescheduled) {
                      LOGGER.info("Email {} rescheduled for redelivery. ", nodePath);
                    } else {
                      LOGGER
                          .error(
                              "Unable to reschedule email for delivery: "
                                  + e.getMessage(), e);
                    }
                  }
                } else {
                  setError(messageContent, "Message must have a to and from set");
                }
              } else {
                setError(messageContent, "Not an outbox");
              }
              if (!messageContent.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)) {
                messageContent.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
                    MessageConstants.BOX_SENT);
              }
            }
          } else {
            String retval = "null";
            setError(messageContent,
                "Expected recipients to be String or List<String>.  Found " + retval);
          }
        } finally {
          if (adminSession != null) {
            adminSession.logout();
          }
        }
      }
    } catch (PathNotFoundException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (JMSException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (EmailDeliveryException e) {
      LOGGER.error(e.getMessage());
    } catch (ClientPoolException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  private MultiPartEmail constructMessage(Content contentNode, List<String> recipients,
      javax.jcr.Session session, org.sakaiproject.nakamura.api.lite.Session sparseSession)
      throws EmailDeliveryException, StorageClientException, AccessDeniedException,
      PathNotFoundException, RepositoryException {
    MultiPartEmail email = new MultiPartEmail();
    // TODO: the SAKAI_TO may make no sense in an email context
    // and there does not appear to be any distinction between Bcc and To in java mail.

    Set<String> toRecipients = new HashSet<String>();
    Set<String> bccRecipients = new HashSet<String>();
    for (String r : recipients) {
      bccRecipients.add(convertToEmail(r.trim(), sparseSession));
    }

    if (contentNode.hasProperty(MessageConstants.PROP_SAKAI_TO)) {
      String[] tor = StringUtils.split(
          (String) contentNode.getProperty(MessageConstants.PROP_SAKAI_TO), ',');
      for (String r : tor) {
        r = convertToEmail(r.trim(), sparseSession);
        if (bccRecipients.contains(r)) {
          toRecipients.add(r);
          bccRecipients.remove(r);
        }
      }
    }
    for (String r : toRecipients) {
      try {
        email.addTo(convertToEmail(r, sparseSession));
      } catch (EmailException e) {
        throw new EmailDeliveryException("Invalid To Address [" + r
            + "], message is being dropped :" + e.getMessage(), e);
      }
    }
    for (String r : bccRecipients) {
      try {
        email.addBcc(convertToEmail(r, sparseSession));
      } catch (EmailException e) {
        throw new EmailDeliveryException("Invalid Bcc Address [" + r
            + "], message is being dropped :" + e.getMessage(), e);
      }
    }

    if (contentNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)) {
      String from = (String) contentNode.getProperty(MessageConstants.PROP_SAKAI_FROM);
      try {
        email.setFrom(convertToEmail(from, sparseSession));
      } catch (EmailException e) {
        throw new EmailDeliveryException("Invalid From Address [" + from
            + "], message is being dropped :" + e.getMessage(), e);
      }
    } else {
      throw new EmailDeliveryException("Must provide a 'from' address.");
    }

    if (contentNode.hasProperty(MessageConstants.PROP_SAKAI_BODY)) {
      String messageBody = (String) contentNode
          .getProperty(MessageConstants.PROP_SAKAI_BODY);
      // if this message has a template, use it
      LOGGER
          .debug("Checking for sakai:templatePath and sakai:templateParams properties on the outgoing message's node.");
      if (contentNode.hasProperty(MessageConstants.PROP_TEMPLATE_PATH)
          && contentNode.hasProperty(MessageConstants.PROP_TEMPLATE_PARAMS)) {
        Map<String, String> parameters = getTemplateProperties((String) contentNode
            .getProperty(MessageConstants.PROP_TEMPLATE_PARAMS));
        String templatePath = (String) contentNode
            .getProperty(MessageConstants.PROP_TEMPLATE_PATH);
        LOGGER.debug("Got the path '{0}' to the template for this outgoing message.",
            templatePath);
        Node templateNode = session.getNode(templatePath);
        if (templateNode.hasProperty("sakai:template")) {
          String template = templateNode.getProperty("sakai:template").getString();
          LOGGER.debug("Pulled the template body from the template node: {0}", template);
          messageBody = templateService.evaluateTemplate(parameters, template);
          LOGGER.debug("Performed parameter substitution in the template: {0}",
              messageBody);
        }
      } else {
        LOGGER
            .debug(
                "Message node '{0}' does not have sakai:templatePath and sakai:templateParams properties",
                contentNode.getPath());
      }
      try {
        email.setMsg(messageBody);
      } catch (EmailException e) {
        throw new EmailDeliveryException(
            "Invalid Message Body, message is being dropped :" + e.getMessage(), e);
      }
    }

    if (contentNode.hasProperty(MessageConstants.PROP_SAKAI_SUBJECT)) {
      email.setSubject((String) contentNode
          .getProperty(MessageConstants.PROP_SAKAI_SUBJECT));
    }

    ContentManager contentManager = sparseSession.getContentManager();
    for (String streamId : contentNode.listStreams()) {
      String description = null;
      if (contentNode.hasProperty(StorageClientUtils.getAltField(
          MessageConstants.PROP_SAKAI_ATTACHMENT_DESCRIPTION, streamId))) {
        description = (String) contentNode.getProperty(StorageClientUtils.getAltField(
            MessageConstants.PROP_SAKAI_ATTACHMENT_DESCRIPTION, streamId));
      }
      LiteEmailDataSource ds = new LiteEmailDataSource(contentManager, contentNode,
          streamId);
      try {
        email.attach(ds, streamId, description);
      } catch (EmailException e) {
        throw new EmailDeliveryException("Invalid Attachment [" + streamId
            + "] message is being dropped :" + e.getMessage(), e);
      }
    }
    return email;
  }

  private Map<String, String> getTemplateProperties(String templateParameter)
      throws IllegalStateException {
    Map<String, String> rv = new HashMap<String, String>();
    String[] values = templateParameter.split("\\|");
    for (String value : values) {
      String[] keyValuePair = value.split("=");
      rv.put(keyValuePair[0], keyValuePair[1]);
    }
    return rv;
  }

  private String convertToEmail(String address,
      org.sakaiproject.nakamura.api.lite.Session session) throws StorageClientException,
      AccessDeniedException {
    if (address.indexOf('@') < 0) {
      String emailAddress = null;

      Authorizable user = session.getAuthorizableManager().findAuthorizable(address);
      if (user != null) {
        Content profile = session.getContentManager().get(
            LitePersonalUtils.getProfilePath(user.getId()));
        if (profile != null) {
          emailAddress = LitePersonalUtils.getProfilePath(user.getId());
        }
      }
      if (emailAddress != null && emailAddress.trim().length() > 0) {
        address = emailAddress;
      } else {
        address = address + "@" + smtpServer;
      }
    }
    return address;
  }

  private void scheduleRetry(int errorCode, Content contentNode) {
    // All retry-able SMTP errors should have codes starting with 4
    if ((errorCode / 100) == 4) {
      long retryCount = 0;
      if (contentNode.hasProperty(MessageConstants.PROP_SAKAI_RETRY_COUNT)) {
        retryCount = StorageClientUtils.toLong(contentNode
            .getProperty(MessageConstants.PROP_SAKAI_RETRY_COUNT));
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
        jobConfig.put(NODE_PATH_PROPERTY, contentNode.getPath());

        int retryIntervalMillis = retryInterval * 60000;
        Date nextTry = new Date(System.currentTimeMillis() + (retryIntervalMillis));

        try {
          scheduler.fireJobAt(null, job, jobConfig, nextTry);
        } catch (Exception e) {
          LOGGER.error(e.getMessage(), e);
        }
      } else {
        setError(contentNode, "Unable to send message, exhausted SMTP retries.");
      }
    } else {
      LOGGER.warn("Not scheduling a retry for error code not of the form 4xx.");
    }
  }

  protected void activate(ComponentContext ctx) {
    @SuppressWarnings("rawtypes")
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

    try {
      connection = connFactoryService.getDefaultConnectionFactory().createConnection();
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue dest = session.createQueue(QUEUE_NAME);
      MessageConsumer consumer = session.createConsumer(dest);
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

  private void setError(Content node, String error) {
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

}
