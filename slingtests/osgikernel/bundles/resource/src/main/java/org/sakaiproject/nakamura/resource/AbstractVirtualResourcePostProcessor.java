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
package org.sakaiproject.nakamura.resource;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This Class implements the post processing operations on resource types.
 */
public abstract class AbstractVirtualResourcePostProcessor implements SlingPostProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractVirtualResourcePostProcessor.class);
  private String targetResourceType;

  /**
   * 
   */
  public AbstractVirtualResourcePostProcessor() {
    targetResourceType = getResourceType();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.servlets.post.SlingPostProcessor#process(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.List)
   */
  public void process(SlingHttpServletRequest request, List<Modification> changes)
      throws Exception {
    // bind processing to the resource type
    long st = System.currentTimeMillis();
    for (Modification m : changes) {
      if (isMatching(request, m)) {
        LOGGER.debug("Post Processing for {}  on {} ", targetResourceType, request
            .getRequestURI());
        LOGGER.debug("Change from [{}] to [{}] type [{}] ", new Object[] {m.getSource(),
            m.getSource(), m.getType()});
        doProcess(request, changes);
        break;
      }
    }
    long en = System.currentTimeMillis();
    LOGGER.debug("Resource Type Processing added {} ms ", (en - st));
  }

  /**
   * @param request
   * @return
   */
  private boolean isMatching(SlingHttpServletRequest request, Modification m) {
    LOGGER.debug("Matching operation {} ", m.getType());
    if (ModificationType.CREATE.equals(m.getType())) {
      RequestParameter resourceType = request
          .getRequestParameter(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
      LOGGER.debug("This is a create operation with resourceType {} matches {} ?",
          resourceType, targetResourceType);
      if (resourceType != null && targetResourceType.equals(resourceType.getString())) {
        LOGGER.debug("Post Processing for {}  on {} ", targetResourceType, request
            .getRequestURI());
        return true;
      }
    }
    return ( targetResourceType.equals(request.getResource().getResourceType()));
  }

  /**
   * @param request
   * @param changes
   */
  protected abstract void doProcess(SlingHttpServletRequest request,
      List<Modification> changes);

  /**
   * @return
   */
  protected abstract String getResourceType();

}
