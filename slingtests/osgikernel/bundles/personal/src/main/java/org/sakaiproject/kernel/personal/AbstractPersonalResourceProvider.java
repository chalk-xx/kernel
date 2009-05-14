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
package org.sakaiproject.kernel.personal;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.sakaiproject.kernel.api.user.UserFactoryService;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

/**
 * Manages personal space for the user.
 */
public abstract class AbstractPersonalResourceProvider implements ResourceProvider {

  /**
   * The user factory service, injected.
   */
  protected UserFactoryService userFactoryService;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver,
   *      java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver, String path) {
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
    if (path.startsWith(getBasePath())) {
      String userId = request.getRemoteUser();
      String resourcePath = getResourcePath(userId, path);
      if (resourcePath != null) {
        return resourceResolver.resolve(resourcePath);
      }
    }
    return null;
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
   * @return a transpated resource path.
   */
  protected abstract String getResourcePath(String userId, String path);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#listChildren(org.apache.sling.api.resource.Resource)
   */
  public Iterator<Resource> listChildren(Resource parent) {
    return null;
  }

  public void bindUserFactoryService(UserFactoryService userFactoryService) {
    this.userFactoryService = userFactoryService;
  }

  public void unbindUserFactoryService(UserFactoryService userFactoryService) {
    this.userFactoryService = null;
  }

}
