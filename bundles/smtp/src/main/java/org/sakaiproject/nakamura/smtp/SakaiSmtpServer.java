package org.sakaiproject.nakamura.smtp;


import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
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

import javax.jcr.RepositoryException;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMultipart;

@Component(immediate = true, metatype = true)
public class SakaiSmtpServer implements SimpleMessageListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(SakaiSmtpServer.class);
  private static final int MAX_PROPERTY_SIZE = 32 * 1024;

  private SMTPServer server;

  @Reference
  protected LiteMessagingService messagingService;

  @Reference
  protected Repository contentRepository;

  @Property
  private static String LOCAL_DOMAINS = "smtp.localdomains";

  @Property(intValue=8025)
  private static String SMTP_SERVER_PORT = "smtp.port";

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  private Set<String> domains = new HashSet<String>();

  public void activate(ComponentContext context) throws Exception {
    Integer port = (Integer) context.getProperties().get(SMTP_SERVER_PORT);
    if ( port == null ) {
      port = 8025;
    }
    LOGGER.info("Starting SMTP server on port {}", port);
    server = new SMTPServer(new SimpleMessageListenerAdapter(this));
    server.setPort(port);
    server.start();
    String localDomains = (String) context.getProperties().get(LOCAL_DOMAINS);
    if (localDomains == null) {
      localDomains = "localhost";
    }
    domains.clear();
    for (String domain : StringUtils.split(localDomains, ';')) {
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
      session = contentRepository.loginAdministrative();
      List<String> paths = getLocalPath(session, recipient);
      return paths.size() > 0;
    } catch (Exception e) {
      LOGGER.error("Develier message with this handler ", e);
    } finally {
      if (session != null) {
        try {
          session.logout();
        } catch (ClientPoolException e) {
          throw new RuntimeException("Failed to logout session.", e);
        }
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
          String path = messagingService.getFullPathToStore(localRecipient, session);
          if (path != null && path.length() > 0) {
            localPaths.add(path);
          }
        } catch (Exception ex) {
          LOGGER.warn("Failed to expand recipient {} ", localRecipient, ex);
        }
      }
    }
    return localPaths;
  }

  public void deliver(String from, String recipient, InputStream data)
      throws TooMuchDataException, IOException {
    LOGGER.info("Got message FROM: " + from + " TO: " + recipient);
    Session session = null;
    try {
      session = contentRepository.loginAdministrative();

      List<String> paths = getLocalPath(session, recipient);
      if (paths.size() > 0) {
        Map<String, Object> mapProperties = new HashMap<String, Object>();
        mapProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            MessageConstants.SAKAI_MESSAGE_RT);
        mapProperties.put(MessageConstants.PROP_SAKAI_READ, false);
        mapProperties.put(MessageConstants.PROP_SAKAI_FROM, from);
        mapProperties.put(MessageConstants.PROP_SAKAI_MESSAGEBOX,
            MessageConstants.BOX_INBOX);
        Content createdMessage = writeMessage(session, mapProperties, data, paths.get(0));
        if (createdMessage != null) {
          String messagePath = createdMessage.getPath();
          String messageId = (String) createdMessage.getProperty("message-id");
          LOGGER.info("Created message {} at: {} ", messageId, messagePath);

          // we might want alias expansion
          for (int i = 1; i < paths.size(); i++) {
            String targetPath = paths.get(i);
            messagingService.copyMessageNode(createdMessage, targetPath, session);
          }
        }

      }
    } catch (RepositoryException e) {
      LOGGER.error("Unable to write message", e);
      throw new IOException("Message can not be written to repository");
    } catch (MessagingException e) {
      LOGGER.error("Unable to write message", e);
      throw new IOException("Message can not be written to repository");
    } catch (StorageClientException e) {
      LOGGER.error("Unable to write message", e);
    } catch (AccessDeniedException e) {
      LOGGER.error("Unable to write message", e);
    } finally {
      if (session != null) {
        try {
          session.logout();
        } catch (ClientPoolException e) {
          LOGGER.error(e.getLocalizedMessage(), e);
          throw new RuntimeException("Failed to logout session.", e);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Content writeMessage(Session session, Map<String, Object> mapProperties,
      InputStream data, String storePath) throws MessagingException, AccessDeniedException, StorageClientException, RepositoryException, IOException {
    InternetHeaders internetHeaders = new InternetHeaders(data);
    // process the headers into a map.
    for ( Enumeration<Header> e = internetHeaders.getAllHeaders(); e.hasMoreElements(); ) {
      Header h = e.nextElement();
      String name = h.getName();
      String[] values = internetHeaders.getHeader(name);
      if ( values != null) {
        if ( values.length == 1 ) {
          mapProperties.put("sakai:"+name.toLowerCase(), values[0]);
        } else {
          mapProperties.put("sakai:"+name.toLowerCase(), values);
        }
      }
    }
    String[] contentType = internetHeaders.getHeader("content-type");
    if (contentType != null && contentType.length > 0
        && contentType[0].contains("boundary") && contentType[0].contains("multipart/")) {
        MimeMultipart multipart = new MimeMultipart(new SMTPDataSource(contentType[0],
            data));
        Content message = messagingService.create(session, mapProperties,
            (String) mapProperties.get("sakai:message-id"), storePath);
        writeMultipartToNode(session, message, multipart);
        return message;
    } else {
      Content node = messagingService.create(session, mapProperties);
      // set up to stream the body.
      // TODO: Store the body as a conetnt stream ? on disk, not as a property ?
      node.setProperty(MessageConstants.PROP_SAKAI_BODY, data);
      return node;
    }
  }

  private void writeMultipartToNode(Session session, Content message, MimeMultipart multipart) throws MessagingException, AccessDeniedException, StorageClientException, RepositoryException, IOException
      {
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
      Content message) throws MessagingException, AccessDeniedException, StorageClientException, RepositoryException, IOException {
    ContentManager contentManager = session.getContentManager();
    String childName = String.format("part%1$03d", index);
    String childPath = message.getPath() + "/" + childName;
    if (part.getContentType().toLowerCase().startsWith("multipart/")) {
      contentManager.update(new Content(childPath, new HashMap<String, Object>()));
      Content childNode = contentManager.get(childPath);
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

    contentManager.update(new Content(childPath, new HashMap<String, Object>()));
    Content childNode = contentManager.get(childPath);
    writePartPropertiesToNode(part, childNode);
    // TODO: FIXME THIS WILL FAIL should store as a stream on the content 
    childNode.setProperty(MessageConstants.PROP_SAKAI_BODY, part.getInputStream());
  }

  private void writePartAsFile(Session session, BodyPart part, String nodeName,
      Content parentNode) throws AccessDeniedException, StorageClientException, MessagingException, IOException {
    String filePath = parentNode.getPath() + "/nt:file";
    // TODO: FIXME, this is all wrong for sparse
    String fileContentPath = filePath + "/jcr:content";
    session.getContentManager().update(new Content(filePath, new HashMap<String, Object>()));
    session.getContentManager().update(new Content(fileContentPath, new HashMap<String, Object>()));
    Content resourceNode = session.getContentManager().get(fileContentPath);
    resourceNode.setProperty("jcr:primaryType", "nt:resource");
    resourceNode.setProperty("jcr:mimeType", part.getContentType());
    resourceNode.setProperty("jcr:data", part.getInputStream());
    resourceNode.setProperty("jcr:lastModified", Calendar.getInstance());
  }

  @SuppressWarnings("unchecked")
  private void writePartPropertiesToNode(BodyPart part, Content childNode)
      throws MessagingException, RepositoryException {
    Enumeration<Header> headers = part.getAllHeaders();
    while (headers.hasMoreElements()) {
      Header header = headers.nextElement();
      childNode.setProperty(header.getName(), header.getValue());
    }
  }

}
