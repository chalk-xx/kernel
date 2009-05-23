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
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * A Virtual Resource that wrapps a target resource and uses Virtual Resource Metadata.
 * 
 */
public class VirtualResource implements Resource {

  private static final Logger LOGGER = LoggerFactory.getLogger(VirtualResource.class);
  private ResourceMetadata resourceMetadata;
  private Resource resource;

  
  /**
   * @param resource
   * @throws RepositoryException 
   */
  public VirtualResource(Resource resource) {
    this.resource = resource;
    resourceMetadata = new VirtualResourceMetadata(resource.getResourceMetadata());
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.SyntheticResource#getResourceMetadata()
   */
  public ResourceMetadata getResourceMetadata() {
    return resourceMetadata;
  }
  
  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.SyntheticResource#getResourceSuperType()
   */
  public String getResourceSuperType() {
    return resource.getResourceSuperType();
  }
  
  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.SyntheticResource#adaptTo(java.lang.Class)
   */
  public <Type> Type adaptTo(Class<Type> type) {
    LOGGER.info("Trying to adapt to {} ",type);
    return resource.adaptTo(type);
  }
  
  
  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.resource.internal.helper.jcr.JcrNodeResource#getResourceType()
   */
  public String getResourceType() {
    return resource.getResourceType();
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getPath()
   */
  public String getPath() {
    return resource.getPath();
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceResolver()
   */
  public ResourceResolver getResourceResolver() {
    return resource.getResourceResolver();
  }
  
  
  
  
}
