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
import org.apache.sling.api.resource.ResourceWrapper;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;

/**
 *
 */
public class SparseNonExistingResource extends ResourceWrapper {
  /**
   * Placeholder to indicate that this Resource does not yet exist in Sparse
   * storage. (More or less takes the place of Resource.RESOURCE_TYPE_NON_EXISTING.)
   */
  public static final String SPARSE_CONTENT_NON_EXISTING_RT = "sparse/nonexisting";
  private String targetContentPath;
  private Session session;
  private ContentManager contentManager;

  public SparseNonExistingResource(Resource wrappedResource, String targetContentPath,
      Session session, ContentManager contentManager) {
    super(wrappedResource);
    this.targetContentPath = targetContentPath;
    this.session = session;
    this.contentManager = contentManager;
  }

  public String getTargetContentPath() {
    return targetContentPath;
  }

  @Override
  public String getResourceType() {
    return SPARSE_CONTENT_NON_EXISTING_RT;
  }

  @Override
  public String getResourceSuperType() {
    return SparseContentResource.SPARSE_CONTENT_RT;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
    AdapterType retval = null;
    if (type == ContentManager.class) {
      retval = (AdapterType) contentManager;
    } else if (type == Session.class) {
      retval = (AdapterType) session;
    } else {
      retval = super.adaptTo(type);
    }
    return retval;
  }

}
