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
package org.sakaiproject.kernel.message;

import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_USER_PATH;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.personal.PersonalConstants;
import org.sakaiproject.kernel.api.user.UserPostProcessor;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * This PostProcessor listens to post operations on User objects and creates a
 * message store.
 * 
 * @scr.service interface="org.sakaiproject.kernel.api.user.UserPostProcessor"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.component immediate="true" label="MessageUserPostProcessor" description=
 *                "Post Processor for User and Group operations to create a message stores"
 *                metatype="no"
 * @scr.property name="service.description"
 *               value="Post Processes User and Group operations"
 * @scr.reference interface="org.apache.sling.jcr.api.SlingRepository"
 *                name="SlingRepository" bind="bindSlingRepository"
 *                unbind="unbindSlingRepository"
 * 
 */
public class MessageUserPostProcessor implements UserPostProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(MessageUserPostProcessor.class);

  /**
   * The JCR Repository we access to update profile.
   * 
   */
  private SlingRepository slingRepository;

  public void process(SlingHttpServletRequest request,
      List<Modification> changes) throws Exception {
    LOGGER.info("Starting MessageUserPostProcessor process");
    Session session = slingRepository.loginAdministrative(null);
    String resourcePath = request.getRequestPathInfo().getResourcePath();
    String principalName = null;
    if (resourcePath.equals(SYSTEM_USER_MANAGER_USER_PATH)) {
      RequestParameter rpid = request
          .getRequestParameter(SlingPostConstants.RP_NODE_NAME);
      if (rpid != null) {
        principalName = rpid.getString();
        String pathPrivate = PathUtils.toInternalHashedPath(
            PersonalConstants._USER_PRIVATE, principalName,
            MessageConstants.FOLDER_MESSAGES);
        System.out
            .println("Getting/creating private profile node with messages: "
                + pathPrivate);
        Node messageStore = null;
        if (session.itemExists(pathPrivate)) {
          messageStore = (Node) session.getItem(pathPrivate);
        }
        messageStore = JcrUtils.deepGetOrCreateNode(session, pathPrivate);
        messageStore.setProperty(
            JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            MessageConstants.SAKAI_MESSAGESTORE_RT);
        session.save();
        session.logout();

      }
    }

  }

  /**
   * @param slingRepository
   *          the slingRepository to set
   */
  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  /**
   * @param slingRepository
   *          the slingRepository to set
   */
  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

}
