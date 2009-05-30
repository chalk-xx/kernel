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
package org.sakaiproject.kernel.virtual;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

/**
 * Manages personal space for the user.
 */
public abstract class AbstractVirtualResourceProvider implements ResourceProvider {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractVirtualResourceProvider.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver,
   *      java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver, String path) {
    LOGGER.debug("getResource([{}],[{}])", resourceResolver, path);
    return createResource(resourceResolver, path);
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
   * @return the base path for the resource.
   */
  protected abstract String getBasePath();

  /**
   * gets the resource path for a userid and subpath.
   * 
   * @param userId
   *          the user id
   * @param path
   *          the path starting with the BasePath
   * @param resourceResolver
   *          the Resource resolver for this request.
   * @return a transpated resource path.
   */
  protected abstract String getResourcePath(ResourceResolver resourceResolver,
      String userId, String path);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#listChildren(org.apache.sling.api.resource.Resource)
   */
  public Iterator<Resource> listChildren(Resource parent) {
    Resource parentItemResource = getResource(parent.getResourceResolver(), parent
        .getPath());
    return (parentItemResource != null) ? parentItemResource.getResourceResolver()
        .listChildren(parent) : null;
  }

  /**
   * @param resourceResolver
   * @param path
   * @return
   */
  private Resource createResource(ResourceResolver resourceResolver, String path) {
    if (path.startsWith(getBasePath())) {
      Session session = resourceResolver.adaptTo(Session.class);
      String userId = session.getUserID();
      String resourcePath = getResourcePath(resourceResolver, userId, path);
      Resource resource = resourceResolver.resolve(resourcePath);
      if (resource != null) {
        String pathInfo = resource.getResourceMetadata().getResolutionPathInfo();
        LOGGER.debug("Resolving resource for [{}] gave [{}] with [{}] ", new Object[] {
            resourcePath, resource, pathInfo});

        resource = new VirtualResource(resource);
        pathInfo = resource.getResourceMetadata().getResolutionPathInfo();
        LOGGER.debug("Final resource for [{}] gave [{}] with [{}] ", new Object[] {
            resourcePath, resource, pathInfo});
        return resource;
      } else {
        resource = new SyntheticResource(resourceResolver, resourcePath, "");
        // did this resolve to the real resource where the resourcepath is empty ?
        String pathInfo = resource.getResourceMetadata().getResolutionPathInfo();
        LOGGER.debug("Failed to resolve resource for [{}] gave [{}]  with [{}]",
            new Object[] {resourcePath, resource, pathInfo});
        return resource;
      }
    } else {
      LOGGER.debug("Base Path doesnt match ");
    }
    return null;
  }

}
