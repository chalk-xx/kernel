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

/**
 * 
 */
public class PersonalResource implements Resource {

  private static final Logger LOGGER = LoggerFactory.getLogger(PersonalResource.class);
  private Resource resource;
  private ResourceMetadata resourceMetadata;

  
  /**
   * @param resource
   */
  public PersonalResource(Resource resource) {
    this.resource = resource;
    this.resourceMetadata = new PersonalResourceMetaData(resource.getResourceMetadata());
    LOGGER.info("Created Resource Metadata as {} code {} ",new Object[]{resourceMetadata, resourceMetadata.hashCode()});
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getPath()
   */
  public String getPath() {
    String path = resource.getPath();
    LOGGER.info("Getting path "+path);
    return path;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
   */
  public ResourceMetadata getResourceMetadata() {
    LOGGER.info("Getting Resource Metadata {} code {} class {}", new Object[] {resourceMetadata, resourceMetadata.hashCode(), resourceMetadata.getClass()});
    return resourceMetadata;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceResolver()
   */
  public ResourceResolver getResourceResolver() {
    return resource.getResourceResolver();
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
   */
  public String getResourceSuperType() {
    String superType = resource.getResourceSuperType();
    LOGGER.info("Getting Resource Super Type "+superType);
    return superType;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.Resource#getResourceType()
   */
  public String getResourceType() {
    String resourceType = resource.getResourceType();
    LOGGER.info("Getting Resource Type "+resourceType);
    return resourceType;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.adapter.Adaptable#adaptTo(java.lang.Class)
   */
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
    AdapterType t = resource.adaptTo(type);
    LOGGER.info("Converting to {} resulted in {} "+new Object[] {type, t});
    return t;
  }

}
