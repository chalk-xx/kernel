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
package org.sakaiproject.kernel.message.comment;

import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.Event;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageHandler;
import org.sakaiproject.kernel.util.DateUtils;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

/**
 * Handler for commenting messages.
 * 
 * @scr.component label="CommentMessageHandler"
 *                description="Handler for delivering comments to a site."
 *                immediate="true"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.sakaiproject.kernel.api.message.MessageHandler"
 */
public class CommentMessageHandler implements MessageHandler {

  /**
   * The type of files we are going to handle.
   */
  private static final String TYPE = "comment";

  private static final String PROP_WRITETO = "sakai:writeto";

  public static final Logger LOG = LoggerFactory.getLogger(CommentMessageHandler.class);

  public String getType() {
    return TYPE;
  }

  public void handle(Event event, Node node) {
    try {
      LOG.info("Handling comment message {}" + node.getPath());
      Session session = node.getSession();

      // Were does this node have to go to.
      String writeTo = node.getProperty(PROP_WRITETO).getString();

      // The ID for this message.
      String messageId = node.getProperty(MessageConstants.PROP_SAKAI_ID).getString();

      // Make sure the path exists and it is a messagestore.
      if (session.itemExists(writeTo)) {
        Node storeNode = (Node) session.getItem(writeTo);
        if (storeNode.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            && storeNode.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString()
                .equals(MessageConstants.SAKAI_MESSAGESTORE_RT)) {

          // This is a messagestore.
          // Now check if the user has access to this store.
          AccessControlManager acm = AccessControlUtil.getAccessControlManager(session);
          Privilege[] privs = { acm.privilegeFromName(Privilege.JCR_WRITE) };
          if (acm.hasPrivileges(writeTo, privs)) {

            // the user has access, copy the message to this store.
            LOG.info("User has access.");
            
            // Create the path to the message.
            String targetMessage = PathUtils.toInternalHashedPath(writeTo, messageId, "");
            String parent = targetMessage
                .substring(0, targetMessage.lastIndexOf("/"));
            JcrUtils.deepGetOrCreateNode(session, parent);

            // Save the session so the parent gets created.
            session.save();
            
            // Copy the message over.
            session.getWorkspace().copy(node.getPath(), targetMessage);
            
            // Get the newly created message and set some extra properties on it.
            Node newMessage = (Node) session.getItem(targetMessage);

            // TODO: This should be jcr:created, but this doesn't seem to get created.
            newMessage.setProperty("sakai:created", DateUtils.rfc3339());
            newMessage.setProperty(MessageConstants.PROP_SAKAI_SENDSTATE,
                MessageConstants.STATE_NOTIFIED);

            if (session.hasPendingChanges()) {
              session.save();
            }

            LOG.info("Sucessfully wrote a comment message to the store.");

          }
        }
      }

    } catch (ValueFormatException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (PathNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }  
}