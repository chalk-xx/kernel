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

import org.apache.sling.api.resource.ResourceMetadata;

/**
 * Creates ResourceMetadata objects parsing and setting the properties as appropriate
 */
public class ResourceMetatdatFactory {


  /**
   * This factory method creates Resource Metadata making the assumption that the selector
   * element is the first element with a . in the element.
   * 
   * @param absRealPath
   *          the absolute path to the resource.
   * @return a ResoruceMetadata with ResourcePath and ResourcePathInfo set to reflect the
   *         structure of the path.
   */
  public static ResourceMetadata createMetadata(String absRealPath) {
    ResourceMetadata resourceMetadata = new ResourceMetadata();
    int i = absRealPath.indexOf('.');
    if ( i < 0 ) {
      resourceMetadata.setResolutionPath(absRealPath);
      resourceMetadata.setResolutionPathInfo("");
    } else if ( i == 0 ) {
      resourceMetadata.setResolutionPath("/");
      resourceMetadata.setResolutionPathInfo(absRealPath);
    } else {
      resourceMetadata.setResolutionPath(absRealPath.substring(0, i));
      resourceMetadata.setResolutionPathInfo(absRealPath.substring(i));      
    }
    return resourceMetadata;
  }

}
