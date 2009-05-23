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

import org.apache.sling.api.resource.ResourceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource Metadata that can only be constructed, and not modified. VirtualResourceMetadata
 */
public class VirtualResourceMetadata extends ResourceMetadata {

  /**
   *
   */
  private static final long serialVersionUID = -5128305634809339630L;
  private static final Logger LOGGER = LoggerFactory.getLogger(VirtualResourceMetadata.class);
  private boolean sealed;


  /**
   * @param resourceMetadata
   */
  public VirtualResourceMetadata(ResourceMetadata resourceMetadata) {
    super.putAll(resourceMetadata);
    sealed = true;
  }
  
  /**
   * Make the p
   * {@inheritDoc}
   * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
   */
  @Override
  public Object put(String key, Object value) {
    if ( sealed && ResourceMetadata.RESOLUTION_PATH_INFO.equals(key) ) {
      LOGGER.info("Metadata has been sealed, ignoring change {} to {} ",key,value);
      return null;
    }
    return super.put(key,value);
  }

}
