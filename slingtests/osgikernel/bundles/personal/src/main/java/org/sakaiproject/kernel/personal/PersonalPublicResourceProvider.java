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

import org.apache.sling.api.resource.ResourceResolver;
import org.sakaiproject.kernel.util.PathUtils;

/**
 * 
 * 
 * @scr.component immediate="true" label="PersonalPublicResourceProvider"
 *                description="Personal Public Service resource provider"
 * @scr.property name="service.description"
 *               value="Handles requests for Personal Public resources"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="provider.roots" value="/_user/public/"
 * @scr.service interface="org.apache.sling.api.resource.ResourceProvider"
 */
public class PersonalPublicResourceProvider extends AbstractPersonalResourceProvider {

  /**
   * The base path.
   */
  private static final String BASE_PATH = "/_user/public";

  @Override
  protected String getResourcePath(ResourceResolver resourceResolver, String userId, String path) {
    // ignore the userid, and use the first element in the path.
    String resourcePath = path.substring(BASE_PATH.length());
    resourcePath = PathUtils.normalizePath(resourcePath);
    int i = resourcePath.indexOf('/');
    if ( i > 0 ) {
      String pathUserId = resourcePath.substring(0,i);
      String remainingPath = resourcePath.substring(i);
      return userFactoryService.getUserSharedPrivatePath(pathUserId)
      + remainingPath;
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.personal.AbstractVirtualResourceProvider#getBasePath()
   */
  @Override
  protected String getBasePath() {
    return BASE_PATH;
  }

}
