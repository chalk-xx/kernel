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
package org.sakaiproject.nakamura.mailman.impl;

import java.util.logging.Level;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.nakamura.mailman.MailmanManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;

import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;

@Component(immediate = true, metatype = true, label = "%mail.manager.impl.label", description = "%mail.manager.impl.desc")
@Service(value = EventHandler.class)
public class MailmanGroupManager implements EventHandler, ManagedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailmanGroupManager.class);
    @SuppressWarnings("unused")
    @Property(value = "The Sakai Foundation")
    private static final String SERVICE_VENDOR = "service.vendor";
    @SuppressWarnings("unused")
    @Property(value = "Handles management of mailman integration")
    private static final String SERVICE_DESCRIPTION = "service.description";
    @SuppressWarnings("unused")
    @Property(value = {"org/sakaiproject/nakamura/lite/authorizables/ADDED","org/sakaiproject/nakamura/lite/authorizables/UPDATED"})
    private static final String EVENT_TOPICS = "event.topics";
    @Property(value = "password")
    private static final String LIST_MANAGEMENT_PASSWORD = "mailman.listmanagement.password";
    @Reference
    private MailmanManager mailmanManager;
    @Reference
    private Repository repository;
    private Session session; // fetched from the repository
    private AuthorizableManager authorizableManager = null; // fetchs from the session
    private String listManagementPassword;

    public MailmanGroupManager() {
    }

    public MailmanGroupManager(MailmanManager mailmanManager, Repository repository) {
        this.mailmanManager = mailmanManager;
        this.repository = repository;
    }

    @Activate
    public void activate(Map<?, ?> props) throws ClientPoolException, StorageClientException, AccessDeniedException {
        LOGGER.info("Got component initialization");
        listManagementPassword = (String) props.get(LIST_MANAGEMENT_PASSWORD);

        session = this.repository.loginAdministrative();
        authorizableManager = session.getAuthorizableManager();
    }

    @Deactivate
    public void deactivate() {
        try {
            session.logout();
        } catch (Exception e) {
            LOGGER.error("Error logging out of the session", e);
        } finally {
            session = null;
        }
    }

    @Modified
    @SuppressWarnings("unchecked")
    public void updated(Dictionary config) throws ConfigurationException {
        LOGGER.info("Got config update");
        listManagementPassword = (String) config.get(LIST_MANAGEMENT_PASSWORD);
    }

    public void handleEvent(Event event) {
        if (!"group".equalsIgnoreCase(event.getProperty("type").toString())) {
            return; // we only need the events with type: group
        }
        LOGGER.info("Got event on topic: " + event.getTopic());

        Operation operation = null;
        if (event.getProperty("added") != null) {
            operation = Operation.join;
        } else if (event.getProperty("removed") != null) {
            operation = Operation.part;
        } else {
            operation = Operation.create;
        }

        String principalName = event.getProperty("path").toString();
        switch (operation) {
            case create:
                LOGGER.info("Got authorizable creation: " + principalName);

                try {
                    mailmanManager.createList(principalName, listManagementPassword);
                } catch (Exception e) {
                    LOGGER.error("Unable to create mailman list for group", e);
                }
                break;
            case delete:
                LOGGER.info("Got authorizable deletion: " + principalName);
                try {
                    mailmanManager.deleteList(principalName, listManagementPassword);
                } catch (Exception e) {
                    LOGGER.error("Unable to delete mailman list for group", e);
                }
                break;
            case join: {
                String addedId = event.getProperty("added").toString();
                LOGGER.info("Got group join event ***" + addedId + "***");
                String emailAddress = null;
                try {
                    if (!isSubgroup(addedId)) {
                        emailAddress = getEmailForUser(addedId);
                        if (emailAddress != null) {
                            // add the user to the subgroup
                            mailmanManager.addMember(principalName, listManagementPassword, emailAddress);
                            LOGGER.info("Added: " + addedId + " to mailman group " + principalName);
                            // add the user to the maingroup
                            String[] splittedPrincipalName = principalName.split("-");
                            principalName = "";
                            for (int i = 0; i < splittedPrincipalName.length; i++) {
                                if (i < (splittedPrincipalName.length - 1)) {
                                    principalName += splittedPrincipalName[i];
                                    if (i < (splittedPrincipalName.length - 2)) {
                                        principalName += "-";
                                    }
                                }
                            }
                            LOGGER.info("Added: " + addedId + " to mailman group " + principalName);
                            mailmanManager.addMember(principalName, listManagementPassword, emailAddress);
                        } else {
                            LOGGER.warn("No email address recorded for user: " + addedId + ". Not adding to mailman list");
                            return;
                        }
                    }
                } catch (RepositoryException ex) {
                    java.util.logging.Logger.getLogger(MailmanGroupManager.class.getName()).log(Level.SEVERE, null, ex);
                } catch (AccessDeniedException ex) {
                    java.util.logging.Logger.getLogger(MailmanGroupManager.class.getName()).log(Level.SEVERE, null, ex);
                } catch (StorageClientException ex) {
                    java.util.logging.Logger.getLogger(MailmanGroupManager.class.getName()).log(Level.SEVERE, null, ex);
                } catch (MailmanException e) {
                    LOGGER.error("Mailman exception adding user to mailman group", e);
                }
            }
            break;
            case part: {
                LOGGER.info("Got group join event");
                String userId = event.getProperty("removed").toString();
                try {
                    String emailAddress = getEmailForUser(userId);
                    if (emailAddress != null) {
                        LOGGER.info("Removing user: " + userId + " to mailman group " + principalName);
                        mailmanManager.removeMember(userId, listManagementPassword, emailAddress);
                    } else {
                        LOGGER.warn("No email address recorded for user: " + userId + ". Not removing from mailman list");
                    }
                } catch (AccessDeniedException ex) {
                    java.util.logging.Logger.getLogger(MailmanGroupManager.class.getName()).log(Level.SEVERE, null, ex);
                } catch (StorageClientException ex) {
                    java.util.logging.Logger.getLogger(MailmanGroupManager.class.getName()).log(Level.SEVERE, null, ex);
                } catch (RepositoryException e) {
                    LOGGER.error("Repository exception removing user from mailman group", e);
                } catch (MailmanException e) {
                    LOGGER.error("Mailman exception removing user from mailman group", e);
                }
            }
            break;
        }
    }

    private Boolean isSubgroup(String groupId) throws AccessDeniedException, StorageClientException {
        Authorizable authorizable = authorizableManager.findAuthorizable(groupId);
        return authorizable.isGroup();
    }

    private String getEmailForUser(String userId) throws RepositoryException, AccessDeniedException, StorageClientException {
        Authorizable authorizable = authorizableManager.findAuthorizable(userId);
        String email = (String) authorizable.getProperty("email");
        return email;
    }
}
