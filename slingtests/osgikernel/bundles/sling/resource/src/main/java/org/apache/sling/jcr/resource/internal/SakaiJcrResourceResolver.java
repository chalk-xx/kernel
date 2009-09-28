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
package org.apache.sling.jcr.resource.internal;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.jcr.resource.PathResourceTypeProvider;
import org.apache.sling.jcr.resource.internal.helper.MapEntries;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SakaiJcrResourceResolver extends JcrResourceResolver2 {

  private static final Logger log = LoggerFactory.getLogger(SakaiJcrResourceResolver.class);
  private SakaiJcrResourceResolverFactoryImpl factory;

  
  
  /**
   * @param rootProvider
   * @param factory
   * @param resourceMapper
   */
  public SakaiJcrResourceResolver(JcrResourceProviderEntry rootProvider,
      SakaiJcrResourceResolverFactoryImpl factory, MapEntries resourceMapper) {
    super(rootProvider, factory, resourceMapper);
    this.factory = factory;
  }
  
  
  
  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.resource.internal.JcrResourceResolver2#resolveNonExistingResoruce(java.lang.String[])
   */
  @Override
  protected Resource resolveNonExistingResoruce(String absRealPath) {
    String resourceType = getPathResourceType(absRealPath);
    if ( resourceType == null ) {
      return super.resolveNonExistingResoruce(absRealPath);
    } else {
      ResourceMetadata resourceMetaData = ResourceMetatdatFactory.createMetadata(absRealPath);
      return new SakaiNonExistingResource(this, absRealPath, resourceMetaData, resourceType);
    }
  }

  /**
   * Gets the resource type from the path consulting providers, returning the first non
   * null match.
   * 
   * @param absRealPath
   *          the abs real URI of the respource, that may or may not exist.
   * @return null if there is no Path based resoruce type, otherwise the first matching
   *         resource type.
   */
  private String getPathResourceType(String absRealPath) {
    PathResourceTypeProvider[] pathResourceTypeProviders = factory
        .getPathResourceTypeProviders();
    if (pathResourceTypeProviders != null) {
      for (PathResourceTypeProvider prp : factory.getPathResourceTypeProviders()) {
        log.debug("Trying  {}", prp);
        String resourceType = prp.getResourceTypeFromPath(this, absRealPath);
        if (resourceType != null) {
          log.debug("Got  {}", resourceType);
          return resourceType;
        }
      }
    } else {
      log.info("No ResourcePathTypeResolvers ");
    }
    return null;
  }

}
