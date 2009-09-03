package org.sakaiproject.kernel.mailman.impl;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.kernel.api.user.AuthorizableEvent;
import org.sakaiproject.kernel.mailman.MailmanManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/**
 * @scr.component immediate="true" label="MailManagerImpl"
 *                description="Interface to mailman"
 * @scr.property name="service.description"
 *                value="Handles management of mailman integration"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="event.topics" values.0="org/apache/sling/jackrabbit/usermanager/event/create"
 *                                   values.1="org/apache/sling/jackrabbit/usermanager/event/delete"
 * @scr.service interface="org.osgi.service.event.EventHandler"
 */
public class MailmanGroupManager implements EventHandler, ManagedService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MailmanGroupManager.class);
  
  /** @scr.property value="password" type="String" */
  private static final String LIST_MANAGEMENT_PASSWORD = "mailman.listmanagement.password";

  /** @scr.reference */
  private MailmanManager mailmanManager;

  private String listManagementPassword;
  
  public void handleEvent(Event event) {
    LOGGER.info("Got event on topic: " + event.getTopic());
    if ((AuthorizableEvent.TOPIC + "create").equals(event.getTopic())) {
      String principalName = event.getProperty(AuthorizableEvent.PRINCIPAL_NAME).toString();
      LOGGER.info("Got authorizable creation: " + principalName);
      if (principalName.startsWith("g-")) {
        LOGGER.info("Got group creation. Creating mailman list");
        try {
          mailmanManager.createList(principalName, principalName + "@example.com", listManagementPassword);
        } catch (Exception e) {
          LOGGER.error("Unable to create mailman list for group", e);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary config) throws ConfigurationException {
    listManagementPassword = (String) config.get(LIST_MANAGEMENT_PASSWORD);
  }
}
