package org.sakaiproject.kernel.smtp;

import org.apache.commons.io.IOUtils;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.mail.BodyPart;
import javax.mail.Header;
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
  private static final int MAX_PROPERTY_SIZE = 32 * 1024;
  
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
        Session userSession = session.impersonate(new SimpleCredentials(authorizable.getID(), "dummy".toCharArray()));
        writeMessage(userSession, mapProperties, data);
        userSession.save();
      } else {
        LOGGER.warn("Rejecting e-mail for unknown user: " + recipient);
      }
    } catch (RepositoryException e) {
      LOGGER.error("Unable to write message", e);
      throw new IOException("Message can not be written to repository");
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  private void writeMessage(Session session, Map<String, Object> mapProperties,
      InputStream data) throws IOException, RepositoryException {
    parseSMTPHeaders(mapProperties, data);
    StreamCopier streamCopier = new StreamCopier(data);
    try {
      MimeMultipart multipart = new MimeMultipart(new SMTPDataSource(mapProperties, streamCopier));
      Node message = messagingService.create(session, mapProperties, (String)mapProperties.get("message-id"));
      writeMultipartToNode(session, message, multipart);
    } catch (MessagingException e) {
      mapProperties.put(MessageConstants.PROP_SAKAI_BODY, streamCopier.getContents());
      messagingService.create(session, mapProperties);
    }
  }

  private void parseSMTPHeaders(Map<String,Object> mapProperties, InputStream data) throws IOException {
    String line;
    while ((line = readHeaderLine(data)) != null && line.length() > 0) {
      String[] headerParts = line.split(": ", 2);
      if (headerParts.length == 2) {
        if ("subject".equalsIgnoreCase(headerParts[0])) {
          mapProperties.put(MessageConstants.PROP_SAKAI_SUBJECT, headerParts[1]);
        } else if (headerParts[0].charAt(0) != ' '){
          mapProperties.put(headerParts[0].toLowerCase(), headerParts[1]);
        }
      }
    }    
  }
  
  private void writeMultipartToNode(Session session, Node message, MimeMultipart multipart) throws RepositoryException, MessagingException, IOException {
    int count = multipart.getCount();
    for (int i=0; i<count; i++) {
      createChildNodeForPart(session, i, multipart.getBodyPart(i), message);
    }
  }
  
  private boolean isTextType(BodyPart part) throws MessagingException {
    return part.getSize() < MAX_PROPERTY_SIZE && part.getContentType().toLowerCase().startsWith("text/");
  }

  private void createChildNodeForPart(Session session, int index, BodyPart part, Node message) throws RepositoryException, MessagingException, IOException {
    String childName = String.format("part%1$03d", index);
    if (part.getContentType().toLowerCase().startsWith("multipart/")) {
      Node childNode = message.addNode(childName);
      writePartPropertiesToNode(part, childNode);
      MimeMultipart multi = new MimeMultipart(new SMTPDataSource(part.getContentType(), part.getInputStream()));
      writeMultipartToNode(session, childNode, multi);
      return;
    }
    
    if (!isTextType(part)) {
      writePartAsFile(session, part, childName, message);
      return;
    }
    
    Node childNode = message.addNode(childName);
    writePartPropertiesToNode(part, childNode);
    childNode.setProperty(MessageConstants.PROP_SAKAI_BODY, IOUtils.toString(part.getInputStream()));
  }

  private void writePartAsFile(Session session, BodyPart part, String nodeName, Node parentNode) throws RepositoryException, MessagingException, IOException {
    Node fileNode = parentNode.addNode(nodeName, "nt:file");
    Node resourceNode = fileNode.addNode("jcr:content", "nt:resource");
    resourceNode.setProperty("jcr:mimeType", part.getContentType());
    resourceNode.setProperty("jcr:data", session.getValueFactory().createValue(part.getInputStream()));
    resourceNode.setProperty("jcr:lastModified", Calendar.getInstance());
  }

  @SuppressWarnings("unchecked")
  private void writePartPropertiesToNode(BodyPart part, Node childNode) throws MessagingException, RepositoryException {
    Enumeration<Header> headers = part.getAllHeaders();
    while (headers.hasMoreElements()) {
      Header header = headers.nextElement();
      childNode.setProperty(header.getName(), header.getValue());
    }
  }

  private String readHeaderLine(InputStream data) throws IOException {
    StringBuilder string = new StringBuilder("");
    while (true) {
      int c = data.read();
      if (c == -1)
        return string.toString();
      if (c == '\r') {
        c = data.read();
        if (c == -1) {
          string.append('\r');
          return string.toString();
        }
        if (c == '\n') {
          return string.toString();
        } else {
          string.append('\r');
          string.append((char)c);
        }
      } else {
        string.append((char)c);
      }
    }
  }

  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

}
