/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
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
package org.apache.sling.jcr.resource;

import org.apache.sling.api.resource.ResourceResolver;


/**
 * Determines the resource type for a path, which may or may not exist. This is only used
 * where the resource does not exist, giving other bundles the opportunity to specify a
 * resource type for non existent resources. If the resource does exist, the resource type
 * can be set using a JcrResourceTypeProvider. Implementations of this interface should be
 * fast and consume few resources, as it will be invoked for all 404 URI's
 */
public interface PathResourceTypeProvider {

  /**
   * Get the resource type for a path.
   * @param resourceResolver the resource resolver.
   * @param absRealPath the absolute URI of the resource.
   * @return the resource type of the path, null if there is no match.
   */
  String getResourceTypeFromPath(ResourceResolver resourceResolver, String absRealPath);

}
