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

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.resource.VirtualResourceProvider;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

/**
 */
@SlingServlet(resourceTypes = "sakai/messagestore", methods = { "GET", "POST", "PUT",
    "DELETE" })
@Properties(value = {
    @Property(name = "service.description", value = "Provides support for message stores."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
@ServiceDocumentation(
    name = "MessageServlet", shortDescription = "BigStore servlet for messaging", 
    description = "This servlet resends requests to /_user/message to /_user/message/aa/bb/cc/dd/currentuser", 
    methods = {
      @ServiceMethod(name = "GET"), @ServiceMethod(name = "POST"),
      @ServiceMethod(name = "PUT"), @ServiceMethod(name = "DELETE")
    }
)
public class MessageServlet extends AbstractMessageServlet {

  /**
   *
   */
  private static final long serialVersionUID = -2663916166760531044L;
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageServlet.class);

  @Reference
  private transient MessagingService messagingService;

  @Reference
  protected transient VirtualResourceProvider virtualResourceProvider;

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
    LOGGER.info("Request [{}], ResourcePath [{}], Selector [{}], MessageId[{}]",
        new Object[] { request.getRequestURI(), resourcePath, selector, messageId });

    String finalPath = "";
    String storePath = resourcePath.substring(0, resourcePath.lastIndexOf("/"));
    if (storePath.equals(MessageConstants._USER_MESSAGE)) {

      Session session = request.getResourceResolver().adaptTo(Session.class);
      String messagePath = messagingService.getFullPathToMessage(request.getRemoteUser(),
          messageId, session);
      finalPath = messagePath + selector;
    } else {
      finalPath = PathUtils.toInternalHashedPath(storePath, messageId, "");
    }
    LOGGER.info("Processed Path to {} ", finalPath);
    return finalPath;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.resource.AbstractVirtualPathServlet#getVirtualResourceProvider()
   */
  @Override
  protected VirtualResourceProvider getVirtualResourceProvider() {
    return virtualResourceProvider;
  }

}
