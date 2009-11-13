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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;

/**
 *
 */
public abstract class AbstractVirtualResourceType implements VirtualResourceType {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractVirtualResourceType.class);

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.resource.VirtualResourceType#getResource(org.apache.sling.api.resource.ResourceResolver, javax.servlet.http.HttpServletRequest, javax.jcr.Node, javax.jcr.Node, java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver,
      HttpServletRequest request, Node n, Node firstRealNode, String absRealPath) {
    LOGGER.debug("Created Resoruce for Virtual path  {}", absRealPath);
    return new SyntheticResource(resourceResolver, absRealPath, getResourceType());
  }


}
