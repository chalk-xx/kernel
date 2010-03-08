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
package org.sakaiproject.nakamura.message;

import static org.sakaiproject.nakamura.api.user.UserConstants.SYSTEM_USER_MANAGER_USER_PATH;
import static org.sakaiproject.nakamura.util.ACLUtils.ADD_CHILD_NODES_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.READ_DENIED;
import static org.sakaiproject.nakamura.util.ACLUtils.WRITE_DENIED;
import static org.sakaiproject.nakamura.util.ACLUtils.MODIFY_PROPERTIES_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.REMOVE_CHILD_NODES_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.REMOVE_NODE_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.WRITE_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.READ_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.addEntry;

import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.api.user.UserPostProcessor;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * This PostProcessor listens to post operations on User objects and creates a message
 * store.
 * 
 * @scr.service interface="org.sakaiproject.nakamura.api.user.UserPostProcessor"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.component immediate="true" label="MessageUserPostProcessor" description=
 *                "Post Processor for User and Group operations to create a message stores"
 *                metatype="no"
 * @scr.property name="service.description"
 *               value="Post Processes User and Group operations"
 * 
 */
public class MessageUserPostProcessor implements UserPostProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(MessageUserPostProcessor.class);

  public void process(Authorizable authorizable, Session session,
      SlingHttpServletRequest request, List<Modification> changes) throws Exception {
    LOGGER.debug("Starting MessageUserPostProcessor process");
    String resourcePath = request.getRequestPathInfo().getResourcePath();
    if (resourcePath.equals(SYSTEM_USER_MANAGER_USER_PATH)) {
      PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
      String pathPrivate = PersonalUtils.getHomeFolder(authorizable) + "/"
          + MessageConstants.FOLDER_MESSAGES;
      LOGGER
          .debug("Getting/creating private profile node with messages: {}", pathPrivate);

      Node messageStore = JcrUtils.deepGetOrCreateNode(session, pathPrivate);
      messageStore.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
          MessageConstants.SAKAI_MESSAGESTORE_RT);
      // ACL's are managed by the Personal User Post processor.
      Principal anon = new Principal() {

        public String getName() {
          return UserConstants.ANON_USERID;
        }

      };
      Principal everyone = principalManager.getEveryone();

      addEntry(messageStore.getPath(), authorizable.getPrincipal(), session,
          READ_GRANTED, WRITE_GRANTED, REMOVE_CHILD_NODES_GRANTED,
          MODIFY_PROPERTIES_GRANTED, ADD_CHILD_NODES_GRANTED, REMOVE_NODE_GRANTED);

      // explicitly deny anon and everyone, this is private space.
      addEntry(messageStore.getPath(), anon, session, READ_DENIED, WRITE_DENIED);
      addEntry(messageStore.getPath(), everyone, session, READ_DENIED, WRITE_DENIED);

    }
  }

}
