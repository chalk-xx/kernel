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
package org.sakaiproject.nakamura.user.lite.resource;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;

import java.util.Map;


/**
 * Resource implementation for Authorizable
 */
public class LiteAuthorizableResource extends AbstractResource implements Resource {
  private Authorizable authorizable = null;

  private ResourceResolver resourceResolver = null;

  private final String path;

  private final String resourceType;

  private final ResourceMetadata metadata;

  public LiteAuthorizableResource(Authorizable authorizable,
      ResourceResolver resourceResolver, String path) {
    super();

    this.resourceResolver = resourceResolver;
    this.authorizable = authorizable;
    this.path = path;
    if (authorizable instanceof Group) {
      this.resourceType = "sparse/group";
    } else {
      this.resourceType = "sparse/user";
    }

    this.metadata = new ResourceMetadata();
    metadata.setResolutionPath(path);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.sling.api.resource.Resource#getPath()
   */
  public String getPath() {
    return path;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
   */
  public ResourceMetadata getResourceMetadata() {
    return metadata;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.sling.api.resource.Resource#getResourceResolver()
   */
  public ResourceResolver getResourceResolver() {
    return resourceResolver;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
   */
  public String getResourceSuperType() {
    return "sparse/authorizable";
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.sling.api.resource.Resource#getResourceType()
   */
  public String getResourceType() {
    return resourceType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.sling.api.adapter.Adaptable#adaptTo(java.lang.Class)
   */
  @SuppressWarnings("unchecked")
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
    if (type == Map.class || type == ValueMap.class) {
      return (AdapterType) new LiteAuthorizableValueMap(authorizable); // unchecked
      // cast
    } else if (type == Authorizable.class
        || (type == User.class && !(authorizable instanceof Group))
        || (type == Group.class && (authorizable instanceof Group))) {
      return (AdapterType) authorizable;
    }

    return super.adaptTo(type);
  }

  public String toString() {
    String id = null;
    if (authorizable != null) {
       id = authorizable.getId();
    }
    return getClass().getSimpleName() + ", id=" + id + ", path=" + getPath();
  }
}
