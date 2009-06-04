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

import org.apache.sling.jcr.resource.JcrResourceTypeProvider;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @scr.component immediate="true" label="ImageResourceTypeProvider"
 *                description="Image Service JCR resource type provider"
 * @scr.property name="service.description" value="Handles requests for Image resources"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.apache.sling.jcr.resource.JcrResourceTypeProvider" 
 * 
 */
public class ImageResourceTypeProvider implements JcrResourceTypeProvider {

  /**
   *
   */
  private static final String SAKAI_IMAGE_TYPE = "sakai:imageType";

  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.resource.JcrResourceTypeProvider#getResourceTypeForNode(javax.jcr.Node)
   */
  public String getResourceTypeForNode(Node node) throws RepositoryException {
    if ( node.hasProperty(SAKAI_IMAGE_TYPE) ) {
      return node.getProperty(SAKAI_IMAGE_TYPE).getString();
    }
    return null;
  }

}
