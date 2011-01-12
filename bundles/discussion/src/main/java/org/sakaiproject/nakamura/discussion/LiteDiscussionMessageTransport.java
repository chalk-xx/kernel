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

package org.sakaiproject.nakamura.discussion;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.discussion.DiscussionConstants;
import org.sakaiproject.nakamura.api.lite.*;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.locking.LockManager;
import org.sakaiproject.nakamura.api.locking.LockTimeoutException;
import org.sakaiproject.nakamura.api.message.*;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;

import static org.sakaiproject.nakamura.api.discussion.DiscussionConstants.TOPIC_DISCUSSION_MESSAGE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.*;

/**
 * Handler for messages that are sent locally and intended for local delivery. Needs to be
 * started immediately to make sure it registers with JCR as soon as possible.
 */
@Component(immediate = true, label = "%discussion.messageTransport.label", description = "%discussion.messageTransport.desc")
@Service
public class LiteDiscussionMessageTransport implements LiteMessageTransport {
  private static final Logger LOG = LoggerFactory
      .getLogger(DiscussionMessageTransport.class);
  private static final String TYPE = DiscussionConstants.TYPE_DISCUSSION;

  @Reference
  protected transient Repository contentRepository;

  @Reference
  protected transient LiteMessagingService messagingService;

  @Reference
  protected transient LockManager lockManager;

  @Reference
  protected transient EventAdmin eventAdmin;

  @org.apache.felix.scr.annotations.Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  /**
   * Due to the fact that we are setting ACLs it is hard to unit test this class. If this
   * variable is set to true, than the ACL settings will be omitted.
   */
  private boolean testing = false;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.message.MessageTransport#send(org.sakaiproject.nakamura.api.message.MessageRoutes,
   *      org.osgi.service.event.Event, javax.jcr.Node)
   */
  public void send(MessageRoutes routes, Event event, Content originalMessage) {
    Session session = null;
    try {
      // Login as admin.
      session = contentRepository.loginAdministrative();

      for (MessageRoute route : routes) {
        if (DiscussionConstants.TYPE_DISCUSSION.equals(route.getTransport())) {
          String recipient = route.getRcpt();
          // the path were we want to save messages in.
          String messageId = StorageClientUtils.toString(originalMessage
              .getProperty(PROP_SAKAI_ID));
          String toPath = messagingService.getFullPathToMessage(recipient, messageId,
              session);

          try {
            lockManager.waitForLock(toPath);
          } catch (LockTimeoutException e) {
            throw new MessagingException("Unable to lock discussion widget message store");
          }
          // Copy the node to the destination
          Content newMessageNode = new Content(toPath, new HashMap<String, Object>());

          Map<String, Object> messageProps = originalMessage.getProperties();
          for (String propertyKey : messageProps.keySet()) {
            if (!propertyKey.contains("jcr:"))
              newMessageNode.setProperty(propertyKey,
                  StorageClientUtils.toStore(messageProps.get(propertyKey)));
          }

          // Add some extra properties on the just created node.
          newMessageNode.setProperty(PROP_SAKAI_TYPE,
              StorageClientUtils.toStore(route.getTransport()));
          newMessageNode.setProperty(PROP_SAKAI_TO,
              StorageClientUtils.toStore(route.getRcpt()));
          newMessageNode.setProperty(PROP_SAKAI_MESSAGEBOX,
              StorageClientUtils.toStore(BOX_INBOX));
          newMessageNode.setProperty(PROP_SAKAI_SENDSTATE,
              StorageClientUtils.toStore(STATE_NOTIFIED));
          session.getContentManager().update(newMessageNode);

          if (!testing) {
            // This will probably be saved in a site store. Not all the users will have
            // access to their message. So we add an ACL that allows the user to edit and
            // delete it later on.
            String from = StorageClientUtils.toString(originalMessage
                .getProperty(PROP_SAKAI_FROM));
            Authorizable authorizable = session.getAuthorizableManager()
                .findAuthorizable(from);

            List<AclModification> aclModifications = new ArrayList<AclModification>();
            // CAN_ANYTHING means read/write/delete
            AclModification.addAcl(true, Permissions.CAN_ANYTHING, authorizable.getId(),
                aclModifications);

            AclModification[] arrayOfMods = aclModifications
                .toArray(new AclModification[aclModifications.size()]);
            session.getAccessControlManager().setAcl(Security.ZONE_CONTENT, toPath,
                arrayOfMods);
          }

          try {
            // Send an OSGi event. The value of the selector is the last part of the event
            // topic.
            final Dictionary<String, String> properties = new Hashtable<String, String>();
            properties.put(UserConstants.EVENT_PROP_USERID, route.getRcpt());
            properties.put("from",
                StorageClientUtils.toString(newMessageNode.getProperty(PROP_SAKAI_FROM)));
            EventUtils.sendOsgiEvent(properties, TOPIC_DISCUSSION_MESSAGE, eventAdmin);
          } catch (Exception e) {
            // Swallow all exceptions, but leave a note in the error log.
            LOG.error("Failed to send OSGi event for discussion", e);
          }
        }
      }
    } catch (AccessDeniedException e) {
      LOG.error(e.getMessage());
    } catch (ClientPoolException e) {
      LOG.error(e.getMessage());
    } catch (StorageClientException e) {
      LOG.error(e.getMessage());
    } finally {
      lockManager.clearLocks();
    }
  }

  /**
   * Determines what type of messages this handler will process. {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.message.MessageProfileWriter#getType()
   */
  public String getType() {
    return TYPE;
  }

  /**
   * This method should only be called for unit testing purposes. It will disable the ACL
   * settings.
   */
  protected void activateTesting() {
    testing = true;
  }

}
