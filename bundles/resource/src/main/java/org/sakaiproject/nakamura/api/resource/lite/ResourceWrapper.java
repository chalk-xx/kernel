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
package org.sakaiproject.nakamura.api.resource.lite;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.sakaiproject.nakamura.api.lite.content.Content;

/**
 * Wrapper for {@link Content} objects to become a {@link Resource}
 */
public class ResourceWrapper extends AbstractResource {
  private Content content;
  private ResourceResolver resourceResolver;

  public ResourceWrapper(Content content, ResourceResolver resourceResolver) {
    this.content = content;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getPath()
   */
  public String getPath() {
    return content.getPath();
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceType()
   */
  public String getResourceType() {
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
   */
  public String getResourceSuperType() {
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
   */
  public ResourceMetadata getResourceMetadata() {
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceResolver()
   */
  public ResourceResolver getResourceResolver() {
    return resourceResolver;
  }

}
