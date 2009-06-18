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

import static org.sakaiproject.kernel.api.message.MessageConstants.SAKAI_MESSAGESTORE_RT;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.message.MessagingException;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * Service for doing operations with messages.
 * 
 * @scr.component immediate="true" label="Sakai User Factory Service"
 *                description="Service for doing operations with messages."
 *                name="org.sakaiproject.kernel.api.message.MessagingService"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.sakaiproject.kernel.api.message.MessagingService"
 */
public class MessagingServiceImpl implements MessagingService {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(MessagingServiceImpl.class);

  /**
   * 
   * {@inheritDoc}
   * 
   * @throws MessagingException
   * 
   * @see org.sakaiproject.kernel.api.message.MessagingService#create(org.apache.sling.api.resource.Resource)
   */
  public Node create(Resource baseResource, Map<String, Object> mapProperties)
      throws MessagingException {

    Node msg = null;
    
    // Create a unique id for the message
    // (08e2a6e89de23e61a101346e134f131f7a94b7ba)
    ResourceMetadata rm = baseResource.getResourceMetadata();

    String pathInfo = String.valueOf(Thread.currentThread().getId())
        + String.valueOf(System.currentTimeMillis());
    try {
      pathInfo = org.sakaiproject.kernel.util.StringUtils.sha1Hash(pathInfo);
    } catch (Exception ex) {
      throw new MessagingException("Unable to create hash.");
    }
    // /_private/D0/33/E2/admin/messages (The messagestore)
    String servletPath = rm.getResolutionPath();

    // 0 = 08e2a6e89de23e61a101346e134f131f7a94b7ba, 1 = ""
    String[] pathParts = PathUtils.getNodePathParts(pathInfo);

    // /_private/D0/33/E2/admin/messages/eb/6e/b7/c7/08e2a6e89de23e61a101346e134f131f7a94b7ba
    // Create a unique path to the message.
    String finalPath = PathUtils.toInternalHashedPath(servletPath,
        pathParts[0], pathParts[1]);

    Node n = (Node) baseResource.adaptTo(Node.class);
    try {
      msg = JcrUtils.deepGetOrCreateNode(n.getSession(), finalPath);

      for (String s : mapProperties.keySet()) {
        msg.setProperty(s, mapProperties.get(s).toString());
      }

      n.getSession().save();

    } catch (RepositoryException e) {
      LOGGER.warn("RepositoryException on trying to save message." + e.getMessage());
      e.printStackTrace();
      throw new MessagingException("Unable to save message.");
    }
    return msg;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.message.MessagingService#getMessageStorePathFromMessageNode(javax.jcr.Node)
   */
  public String getMessageStorePathFromMessageNode(Node msg)
      throws ValueFormatException, PathNotFoundException,
      ItemNotFoundException, AccessDeniedException, RepositoryException {
    Node n = msg;
    while (!"/".equals(n.getPath())) {
      if (n.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
          && SAKAI_MESSAGESTORE_RT.equals(n.getProperty(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
        return n.getPath();
      }
      n = n.getParent();
    }
    return null;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.message.MessagingService#getMessagePathFromMessageStore(javax.jcr.Node)
   */
  public String getMessagePathFromMessageStore(Node msg)
      throws ValueFormatException, PathNotFoundException,
      ItemNotFoundException, AccessDeniedException, RepositoryException {
    String msgPath = "";
    Node n = msg;
    while (!"/".equals(n.getPath())) {
      if (n.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
          && SAKAI_MESSAGESTORE_RT.equals(n.getProperty(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
        return msgPath;
      }
      msgPath = "/" + n.getName() + msgPath;
      n = n.getParent();
    }
    return msgPath;
  }

}
