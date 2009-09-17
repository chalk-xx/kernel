/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.kernel.mailman.impl;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.kernel.api.user.AuthorizableEvent;
import org.sakaiproject.kernel.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.kernel.mailman.MailmanManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import javax.jcr.RepositoryException;

/**
 * @scr.component immediate="true" label="MailManagerImpl"
 *                description="Interface to mailman"
 * @scr.property name="service.description"
 *                value="Handles management of mailman integration"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="event.topics" values.0="org/apache/sling/jackrabbit/usermanager/event/create"
 *                                   values.1="org/apache/sling/jackrabbit/usermanager/event/delete"
 *                                   values.2="org/apache/sling/jackrabbit/usermanager/event/join"
 *                                   values.3="org/apache/sling/jackrabbit/usermanager/event/part"
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
    Operation operation = (Operation) event.getProperty(AuthorizableEvent.OPERATION);
    String principalName = event.getProperty(AuthorizableEvent.PRINCIPAL_NAME).toString();
    switch (operation) {
      case create:
        LOGGER.info("Got authorizable creation: " + principalName);
        if (principalName.startsWith("g-")) {
          LOGGER.info("Got group creation. Creating mailman list");
          try {
            mailmanManager.createList(principalName, principalName + "@example.com", listManagementPassword);
          } catch (Exception e) {
            LOGGER.error("Unable to create mailman list for group", e);
          }
        }
        break;
      case delete:
        LOGGER.info("Got authorizable deletion: " + principalName);
        if (principalName.startsWith("g-")) {
          LOGGER.info("Got group deletion. Deleting mailman list");
          try {
            mailmanManager.deleteList(principalName, listManagementPassword);
          } catch (Exception e) {
            LOGGER.error("Unable to delete mailman list for group", e);
          }
        }
        break;
      case join:
      {
        LOGGER.info("Got group join event");
        Group group = (Group)event.getProperty(AuthorizableEvent.GROUP);
        User user = (User)event.getProperty(AuthorizableEvent.USER);
        try {
          LOGGER.info("Adding " + user.getID() + " to mailman group " + group.getID());
          mailmanManager.addMember(group.getID(), listManagementPassword, user.getID() + "@example.com");
        } catch (RepositoryException e) {
          LOGGER.error("Repository exception adding user to mailman group", e);
        } catch (MailmanException e) {
          LOGGER.error("Mailman exception adding user to mailman group", e);
        }
      }
        break;
      case part:
      {
        LOGGER.info("Got group join event");
        Group group = (Group)event.getProperty(AuthorizableEvent.GROUP);
        User user = (User)event.getProperty(AuthorizableEvent.USER);
        try {
          LOGGER.info("Adding " + user.getID() + " to mailman group " + group.getID());
          mailmanManager.removeMember(group.getID(), listManagementPassword, user.getID() + "@example.com");
        } catch (RepositoryException e) {
          LOGGER.error("Repository exception removing user from mailman group", e);
        } catch (MailmanException e) {
          LOGGER.error("Mailman exception removing user from mailman group", e);
        }
      }
        break;
    }
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary config) throws ConfigurationException {
    LOGGER.info("Got config update");
    listManagementPassword = (String) config.get(LIST_MANAGEMENT_PASSWORD);
  }
  
  protected void activate(ComponentContext componentContext) {
    LOGGER.info("Got component initialization");
    listManagementPassword = (String)componentContext.getProperties().get(LIST_MANAGEMENT_PASSWORD);
  }

}
