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
package org.sakaiproject.kernel.messaging.resource;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.util.PathUtils;
import org.sakaiproject.kernel.virtual.AbstractVirtualResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * 
 */
public class MessageResourceProvider extends AbstractVirtualResourceProvider {

  /**
   *
   */
  private static final String SAKAI_MESSAGES = "sakai/messages";
  private static final Logger LOGGER = LoggerFactory
      .getLogger(MessageResourceProvider.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.virtual.AbstractVirtualResourceProvider#getBasePath()
   */
  @Override
  protected String getBasePath() {
    return "/";
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.virtual.AbstractVirtualResourceProvider#getResourcePath(java.lang.String,
   *      java.lang.String)
   */
  @Override
  protected String getResourcePath(ResourceResolver resourceResolver, String userId,
      String path) {
    // work back up until we
    try {
      Resource resource = resourceResolver.resolve(path);
      Node node = resource.adaptTo(Node.class);
      Node rootNode = node.getSession().getRootNode();
      Node resourceNode = node;
      while (!rootNode.equals(resourceNode)) {
        if (resourceNode.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            && SAKAI_MESSAGES.equals(resourceNode.getProperty(
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
          // this node is the resource node marked with the resourceType, so this forms
          // the base
          return getResourcePath(resourceNode.getPath(), path);
        }
        resourceNode = resourceNode.getParent();
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);

    }
    return null;
  }

  /**
   * @param resourceNode
   * @param path
   * @return
   */
  private String getResourcePath(String resourcePath, String path) {

    if (path.startsWith(resourcePath)) {
      String basePath = path.substring(0, resourcePath.length());
      String pathInfo = path.substring(resourcePath.length());
      basePath = PathUtils.normalizePath(basePath);
      pathInfo = PathUtils.normalizePath(pathInfo);
      return basePath + PathUtils.getHashedPath(pathInfo, 4);
    }
    return null;
  }
}