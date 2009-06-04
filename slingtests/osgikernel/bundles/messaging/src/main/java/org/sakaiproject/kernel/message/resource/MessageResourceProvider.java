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
package org.sakaiproject.kernel.message.resource;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

/**
 * This class generates a synthetic resource with the right resource for all resources
 * under a messagestore. It is registered against / with a classifier.
 * 
 * @offscr.component immediate="true" label="MessageResourceProvider"
 *                description="Message Service resource provider"
 * @offscr.property name="service.description" value="Handles requests for Message resources"
 * @offscr.property name="service.vendor" value="The Sakai Foundation"
 * @offscr.property name="provider.roots" value="/"
 * @offscr.property name="provider.classifier" value="messagestore"
 * @offscr.service interface="org.apache.sling.api.resource.ResourceProvider"
 */
public class MessageResourceProvider implements ResourceProvider {

  /**
   *
   */
  public static final String SAKAI_MESSAGESTORE = "sakai/messagestore";
  private static final Logger LOGGER = LoggerFactory
      .getLogger(MessageResourceProvider.class);

  /**
   * @param session
   * @param path
   * @return
   */
  private Node getMessageStoreRoot(Session session, String path) {
    if (path.endsWith("jcr:content")) {
      return null;
    }
    try {
      Item item = null;
      String lastName = null;
      try {
        item = session.getItem(path);
      } catch (PathNotFoundException ex) {
      }
      String parentPath = path;
      while (item == null) {
        parentPath = PathUtils.getParentReference(parentPath);
        try {
          item = session.getItem(parentPath);
        } catch (PathNotFoundException ex) {
        }
      }
      // convert first item to a node.
      if (!item.isNode()) {
        lastName = item.getName();
        item = item.getParent();
      }

      Node n = (Node) item;
      while (!"/".equals(n.getPath())) {
        if (n.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            && SAKAI_MESSAGESTORE.equals(n.getProperty(
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
          if (".store".equals(lastName)) {
            return null;
          } else {
            LOGGER.info(" {} is a messagestore file, base is {}  ", path, n.getPath());
            return n;
          }
        }
        lastName = n.getName();
        n = n.getParent();
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    LOGGER.debug(" {} is not a messagestore file ", path);
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver,
   *      javax.servlet.http.HttpServletRequest, java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver,
      HttpServletRequest request, String path) {
    return getResource(resourceResolver, path);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver,
   *      java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver, String path) {
    try {
      Session session = resourceResolver.adaptTo(Session.class);
      Node resourceNode = getMessageStoreRoot(session, path);
      if (resourceNode != null) {
        String basePath = resourceNode.getPath();
        String pathInfo = path.substring(basePath.length());
        LOGGER.info(" BasePath [{}] Path Info [{}]", basePath, pathInfo);
        ResourceMetadata resourceMetadata = new ResourceMetadata();
        resourceMetadata.setResolutionPathInfo(pathInfo);
        resourceMetadata.setResolutionPath(basePath);
        Resource resource = new SyntheticResource(resourceResolver, resourceMetadata,
            SAKAI_MESSAGESTORE);
        LOGGER.info("Resource is [{}] Resolved path [{}] Path Info [{}] ", new Object[] {
            resource, resource.getResourceMetadata().getResolutionPath(),
            resource.getResourceMetadata().getResolutionPathInfo()});
        return resource;
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);

    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#listChildren(org.apache.sling.api.resource.Resource)
   */
  public Iterator<Resource> listChildren(Resource parent) {
    LOGGER.debug("Listing Children of {} ", parent);
    if (isMatchingResource(parent.getResourceResolver(), parent.getPath())) {
      Resource parentItemResource = getResource(parent.getResourceResolver(), parent
          .getPath());
      return (parentItemResource != null) ? parentItemResource.getResourceResolver()
          .listChildren(parent) : null;
    }
    return null;
  }

  /**
   * @param resourceResolver
   * @param path
   * @return
   */
  private boolean isMatchingResource(ResourceResolver resourceResolver, String path) {
    Session session = resourceResolver.adaptTo(Session.class);
    boolean matches = (getMessageStoreRoot(session, path) != null);
    LOGGER.debug("Checking {} for match gave {} ", path, matches);
    return matches;
  }

}