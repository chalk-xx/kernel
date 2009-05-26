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

import org.sakaiproject.kernel.util.PathUtils;

/**
 * 
 * 
 * @scr.component immediate="true" label="PersonalPrivateServiceResourceProvider"
 *                description="PersonalService resource provider"
 * @scr.property name="service.description"
 *               value="Handles requests for Personal Private resources"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="provider.roots" value="/_user/private/"
 * @scr.service interface="org.apache.sling.api.resource.ResourceProvider"
 */
public class PersonalPrivateResourceProvider extends AbstractPersonalResourceProvider {

  /**
   * The base path.
   */
  private static final String BASE_PATH = "/_user/private";

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.personal.AbstractVirtualResourceProvider#getResourcePath(java.lang.String,
   *      java.lang.String)
   */
  @Override
  protected String getResourcePath(String userId, String path) {
    String resourcePath = userFactoryService.getUserPrivatePath(userId)
        + path.substring(BASE_PATH.length());;
    
    if ( resourcePath.endsWith("/") ) {
      resourcePath = resourcePath.substring(0,resourcePath.length()-1);
    }
    return PathUtils.normalizePath(resourcePath);
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
