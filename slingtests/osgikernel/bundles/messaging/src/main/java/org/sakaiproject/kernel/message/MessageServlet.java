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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sakai/messagestore"
 * @scr.property name="sling.servlet.methods" values.0="POST" values.1="PUT"
 *               values.2="DELETE" values.3="GET"
 * @scr.reference interface="org.sakaiproject.kernel.api.MessagingService"
 */
public class MessageServlet extends AbstractMessageServlet {

  /**
   *
   */
  private static final long serialVersionUID = -2663916166760531044L;
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageServlet.class);

  private MessagingService messagingService;
  protected void bindMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }
  protected void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.resource.AbstractVirtualPathServlet#getTargetPath(org.apache.sling.api.resource.Resource,
   *      org.apache.sling.api.SlingHttpServletRequest, SlingHttpServletResponse,
   *      java.lang.String, java.lang.String)
   */
  protected String getTargetPath(Resource baseResource, SlingHttpServletRequest request,
      SlingHttpServletResponse response, String realPath, String virtualPath) {
    RequestPathInfo rpi = request.getRequestPathInfo();
    String resourcePath = rpi.getResourcePath();
    String messageId = PathUtils.lastElement(resourcePath);
    String selector = rpi.getSelectorString();
    if (selector == null) {
      selector = "";
    }
    LOGGER.info("Request [{}], ResourcePath [{}], Selector [{}], MessageId[{}]", new Object[] {
        request.getRequestURI(), resourcePath, selector, messageId });

    String finalPath = "";
    String storePath = resourcePath.substring(0, resourcePath.lastIndexOf("/"));
    if (storePath.equals(MessageConstants._USER_MESSAGE)) {

      Session session = request.getResourceResolver().adaptTo(Session.class);
      String messagePath = messagingService.getFullPathToMessage(request.getRemoteUser(), messageId, session);
      finalPath = messagePath + selector;
    } else {
      finalPath = PathUtils.toInternalHashedPath(storePath, messageId, "");
    }
    LOGGER.info("Processed Path to {} ", finalPath);
    return finalPath;
  }

}
