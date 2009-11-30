package org.sakaiproject.kernel.smtp;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.jcr.api.SlingRepository;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

/**
 * @scr.component immediate="true" label="Sakai SMTP Service"
 *                description="Receives incoming mail." name
 *                ="org.sakaiproject.kernel.smtp.SmptServer"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 */
public class SakaiSmtpServer implements SimpleMessageListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(SakaiSmtpServer.class);
  private static final int MAX_PROPERTY_SIZE = 32 * 1024;

  private SMTPServer server;

  /** @scr.reference */
  private MessagingService messagingService;

  /** @scr.reference */
  private SlingRepository slingRepository;
  
  /** @scr.property */
  private static String LOCAL_DOMAINS = "smtp.localdomains";

  private Set<String> domains = new HashSet<String>();

  public void activate(ComponentContext context) throws Exception {
    LOGGER.info("Starting SMTP server");
    server = new SMTPServer(new SimpleMessageListenerAdapter(this));
    server.setPort(8025);
    server.start();
    String localDomains = (String) context.getProperties().get(LOCAL_DOMAINS);
    if ( localDomains == null ) {
      localDomains = "localhost";
    }
    domains.clear();
    for ( String domain : StringUtils.split(localDomains,';') ) {
        domains.add(domain);
    }
  }

  public void deactivate(ComponentContext context) throws Exception {
    LOGGER.info("Stopping SMTP server");
    server.stop();
  }

  /**
   * 
   * {@inheritDoc}
   *
   * @see org.subethamail.smtp.helper.SimpleMessageListener#accept(java.lang.String,
   *      java.lang.String)
   */
  public boolean accept(String from, String recipient) {
    Session session = null;
    try {
      session = slingRepository.loginAdministrative(null);
      List<String> paths = getLocalPath(session, recipient);
      return paths.size() > 0;
    } catch (Exception e) {
      LOGGER.error("Develier message with this handler ", e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
    return false;
  }

  /**
   * @param recipient
   * @return
   */
  private List<String> getLocalPath(Session session, String recipient) {
    // assume recipient is a fully qualified email address of the form xxx@foo.com
    String[] parts = StringUtils.split(recipient, '@');
    List<String> localPaths = new ArrayList<String>();
    if (domains.contains(parts[1])) {
      List<String> recipients = messagingService.expandAliases(parts[0]);
      for (String localRecipient : recipients) {
        try {
          String path = messagingService.getFullPathToStore(parts[0], session);
          if (path != null && path.length() > 0) {
            localPaths.add(path);
          }
        } catch (Exception ex) {
          LOGGER.warn("Failed to expand recipient {} ", localRecipient, ex);
        }
      }
    }
    return null;
  }

  public void deliver(String from, String recipient, InputStream data)
      throws TooMuchDataException, IOException {
    LOGGER.info("Got message FROM: " + from + " TO: " + recipient);
    Session session = null;
    try {
      session = slingRepository.loginAdministrative(null);

      List<String> paths = getLocalPath(session, recipient);
      if (paths.size() > 0) {
        Map<String, Object> mapProperties = new HashMap<String, Object>();
        mapProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            MessageConstants.SAKAI_MESSAGE_RT);
        mapProperties.put(MessageConstants.PROP_SAKAI_READ, false);
        mapProperties.put(MessageConstants.PROP_SAKAI_FROM, from);
        mapProperties.put(MessageConstants.PROP_SAKAI_MESSAGEBOX,
            MessageConstants.BOX_INBOX);

        Node createdMessage = writeMessage(session, mapProperties, data, paths.get(0));
        String messagePath = createdMessage.getPath();
        String messageId = createdMessage.getProperty("message-id").getString();
        LOGGER.info("Created message {} at: {} ", messageId, messagePath);

        // we might want alias expansion
        for (int i = 1; i < paths.size(); i++) {
          String targetPath = paths.get(i);
          messagingService.copyMessageNode(createdMessage, targetPath);
        }
        if (session.hasPendingChanges()) {
          session.save();
        }
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

  private Node writeMessage(Session session, Map<String, Object> mapProperties,
      InputStream data, String storePath) throws IOException, RepositoryException {
    parseSMTPHeaders(mapProperties, data);
    StreamCopier streamCopier = new StreamCopier(data);
    try {
      MimeMultipart multipart = new MimeMultipart(new SMTPDataSource(mapProperties,
          streamCopier));
      Node message = messagingService.create(session, mapProperties,
          (String) mapProperties.get("message-id"), storePath);
      writeMultipartToNode(session, message, multipart);
      return message;
    } catch (MessagingException e) {
      mapProperties.put(MessageConstants.PROP_SAKAI_BODY, streamCopier.getContents());
      return messagingService.create(session, mapProperties);
    }
  }

  private void parseSMTPHeaders(Map<String, Object> mapProperties, InputStream data)
      throws IOException {
    String line;
    while ((line = readHeaderLine(data)) != null && line.length() > 0) {
      String[] headerParts = line.split(": ", 2);
      if (headerParts.length == 2) {
        if ("subject".equalsIgnoreCase(headerParts[0])) {
          mapProperties.put(MessageConstants.PROP_SAKAI_SUBJECT, headerParts[1]);
        } else if (headerParts[0].charAt(0) != ' ') {
          mapProperties.put(headerParts[0].toLowerCase(), headerParts[1]);
        }
      }
    }
  }

  private void writeMultipartToNode(Session session, Node message, MimeMultipart multipart)
      throws RepositoryException, MessagingException, IOException {
    int count = multipart.getCount();
    for (int i = 0; i < count; i++) {
      createChildNodeForPart(session, i, multipart.getBodyPart(i), message);
    }
  }

  private boolean isTextType(BodyPart part) throws MessagingException {
    return part.getSize() < MAX_PROPERTY_SIZE
        && part.getContentType().toLowerCase().startsWith("text/");
  }

  private void createChildNodeForPart(Session session, int index, BodyPart part,
      Node message) throws RepositoryException, MessagingException, IOException {
    String childName = String.format("part%1$03d", index);
    if (part.getContentType().toLowerCase().startsWith("multipart/")) {
      Node childNode = message.addNode(childName);
      writePartPropertiesToNode(part, childNode);
      MimeMultipart multi = new MimeMultipart(new SMTPDataSource(part.getContentType(),
          part.getInputStream()));
      writeMultipartToNode(session, childNode, multi);
      return;
    }

    if (!isTextType(part)) {
      writePartAsFile(session, part, childName, message);
      return;
    }

    Node childNode = message.addNode(childName);
    writePartPropertiesToNode(part, childNode);
    childNode.setProperty(MessageConstants.PROP_SAKAI_BODY, IOUtils.toString(part
        .getInputStream()));
  }

  private void writePartAsFile(Session session, BodyPart part, String nodeName,
      Node parentNode) throws RepositoryException, MessagingException, IOException {
    Node fileNode = parentNode.addNode(nodeName, "nt:file");
    Node resourceNode = fileNode.addNode("jcr:content", "nt:resource");
    resourceNode.setProperty("jcr:mimeType", part.getContentType());
    resourceNode.setProperty("jcr:data", session.getValueFactory().createValue(
        part.getInputStream()));
    resourceNode.setProperty("jcr:lastModified", Calendar.getInstance());
  }

  @SuppressWarnings("unchecked")
  private void writePartPropertiesToNode(BodyPart part, Node childNode)
      throws MessagingException, RepositoryException {
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
          string.append((char) c);
        }
      } else {
        string.append((char) c);
      }
    }
  }

}
