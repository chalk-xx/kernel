package org.sakaiproject.kernel.smtp;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
/**
 * @scr.component immediate="true" label="Sakai SMTP Service"
 *                description="Receives incoming mail." name
 *                ="org.sakaiproject.kernel.smtp.SmptServer"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.reference name="SlingRepository"
 *                interface="org.apache.sling.jcr.api.SlingRepository"
 */
public class SakaiSmtpServer implements SimpleMessageListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(SakaiSmtpServer.class);
  private SMTPServer server;
  
  /** @scr.reference */
  private MessagingService messagingService;
  
  private SlingRepository slingRepository;
  
  public void activate(ComponentContext context) throws Exception {
    LOGGER.info("Starting SMTP server");
    server = new SMTPServer(new SimpleMessageListenerAdapter(this));
    server.setPort(8025);
    server.start();
  }

  public void deactivate(ComponentContext context) throws Exception {
    LOGGER.info("Stopping SMTP server");
    server.stop();
  }

  /**
   * 
   * {@inheritDoc}
   * @see org.subethamail.smtp.helper.SimpleMessageListener#accept(java.lang.String, java.lang.String)
   */
  public boolean accept(String from, String recipient) {
    String principalName = parseRecipient(recipient);
    Session session = null;
    try {
      session = slingRepository.loginAdministrative(null);
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable authorizable = userManager.getAuthorizable(principalName);
      if (authorizable != null) {
        return true;
      } else {
        LOGGER.warn("Rejecting e-mail for unknown user: " + recipient);
      }
    } catch (RepositoryException e) {
      LOGGER.error("Unable to look up user", e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
    return false;
  }

  private String parseRecipient(String recipient) {
    String[] parts = recipient.split("@", 2);
    return parts[0];
  }

  public void deliver(String from, String recipient, InputStream data)
      throws TooMuchDataException, IOException {
    LOGGER.info("Got message FROM: " + from + " TO: " + recipient);
    String principalName = parseRecipient(recipient);
    Session session = null;
    try {
      session = slingRepository.loginAdministrative(null);
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable authorizable = userManager.getAuthorizable(principalName);
      if (authorizable != null) {
        Map<String, Object> mapProperties = new HashMap<String, Object>();
        mapProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            MessageConstants.SAKAI_MESSAGE_RT);
        mapProperties.put(MessageConstants.PROP_SAKAI_READ, false);
        mapProperties.put(MessageConstants.PROP_SAKAI_FROM, from);
        mapProperties.put(MessageConstants.PROP_SAKAI_MESSAGEBOX, MessageConstants.BOX_INBOX);
        parseMessageToMap(mapProperties, data);
        Session userSession = session.impersonate(new SimpleCredentials(authorizable.getID(), "dummy".toCharArray()));
        messagingService.create(userSession, mapProperties);
        userSession.save();
      } else {
        LOGGER.warn("Rejecting e-mail for unknown user: " + recipient);
      }
    } catch (RepositoryException e) {
      LOGGER.error("Unable to write message", e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }
  
  private void parseMessageToMap(Map<String, Object> mapProperties, InputStream data) throws IOException {
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(data));
    String line = null;
    while ((line = bufferedReader.readLine()) != null && line.length() > 0) {
      String[] headerParts = line.split(": ", 2);
      if (headerParts.length == 2) {
        if ("subject".equalsIgnoreCase(headerParts[0])) {
          mapProperties.put(MessageConstants.PROP_SAKAI_SUBJECT, headerParts[1]);
        } else if (headerParts[0].charAt(0) != ' '){
          mapProperties.put(headerParts[0].toLowerCase(), headerParts[1]);
        }
      }
    }
    StringBuffer messageBody = new StringBuffer("");
    while ((line = bufferedReader.readLine()) != null) {
      messageBody.append(line);
      messageBody.append("\n\r");
    }
    try {
      MimeMultipart multipart = new MimeMultipart(new SMTPDataSource(mapProperties, messageBody.toString()));
      int count = multipart.getCount();
      for (int i=0; i<count; i++) {
        BodyPart part = multipart.getBodyPart(i);
        mapProperties.put("Mime part " + i, part.getContent());
      }
    } catch (MessagingException e) {
      mapProperties.put(MessageConstants.PROP_SAKAI_BODY, messageBody);
    }
  }

  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

}
