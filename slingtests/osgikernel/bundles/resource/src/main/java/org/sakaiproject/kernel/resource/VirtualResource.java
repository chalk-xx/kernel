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
package org.sakaiproject.kernel.resource;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;

/**
 * 
 */
public class VirtualResource extends ResourceWrapper {

  private ResourceMetadata resourceMetaData;
  private String resourcePath;

  /**
   * @param resource
   * @param resourcePath 
   */
  public VirtualResource(Resource resource, String resourcePath) {
    super(resource);
    resourceMetaData = resource.getResourceMetadata();
    this.resourcePath = resourcePath;
  }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.sling.api.resource.ResourceWrapper#getPath()
     */
    @Override
    public String getPath() {
      return resourcePath;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.sling.api.resource.ResourceWrapper#getResourceType()
     */
    @Override
    public String getResourceType() {
      return "sling/servlet/default";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.sling.api.resource.ResourceWrapper#getResourceMetadata()
     */
    @Override
    public ResourceMetadata getResourceMetadata() {
      return resourceMetaData;
    }


}
