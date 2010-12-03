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
package org.sakaiproject.nakamura.files.pool;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;

import java.util.Map;

/**
 * Translate a Pooled Content resource path into an internal node path for use by a search query.
 */
@Service
@Component(immediate = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Translates an external Pooled Content path to a Node path for searching."),
    @Property(name = "sakai.search.provider", value = "PooledContentNode") })
public class PooledContentNodeSearchPropertyProvider implements SearchPropertyProvider {
  public static final String POOLED_CONTENT_PROPERTY = "p";
  public static final String POOLED_CONTENT_NODE_PATH_PROPERTY = "_pNodePath";

  /**
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest, java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request, Map<String, String> propertiesMap) {
    RequestParameter rp = request.getRequestParameter(POOLED_CONTENT_PROPERTY);
    if (rp != null) {
      String resourcePath = rp.getString();
      ResourceResolver resourceResolver = request.getResourceResolver();
      Resource pooledResource = resourceResolver.getResource(resourcePath);
      if (pooledResource != null) {
        // You will get very mysterious search exceptions if you do not encode
        // path segments that begin with a digit.
        String xpathSafePath = ISO9075.encodePath(pooledResource.getPath());
        propertiesMap.put(POOLED_CONTENT_NODE_PATH_PROPERTY, xpathSafePath);
      }
    }
  }

}
