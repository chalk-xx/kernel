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
package org.sakaiproject.kernel.siteservice;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * This Class implements the post processing operations on resource types.
 */
public abstract class AbstractResourceTypePostProcessor implements SlingPostProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractResourceTypePostProcessor.class);
  private String targetResourceType;

  /**
   * 
   */
  public AbstractResourceTypePostProcessor() {
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
        LOGGER.info("Post Processing for {}  on {} ", targetResourceType, request
            .getRequestURI());
        LOGGER.info("Change from [{}] to [{}] type [{}] ", new Object[] {m.getSource(),
            m.getSource(), m.getType()});
        switch (m.getType()) {
        case COPY:
          onCopy(request, m);
          break;
        case CREATE:
          onCreate(request, m);
          break;
        case DELETE:
          onDelete(request, m);
          break;
        case MODIFY:
          onModify(request, m);
          break;
        case MOVE:
          onMove(request, m);
          break;
        case ORDER:
          onOrder(request, m);
          break;
        default:
          onOther(request, m);
        }
      }

    }

    long en = System.currentTimeMillis();
    LOGGER.info("Resource Type Processing added {} ms ", (en - st));
  }

  /**
   * @param request
   * @return
   */
  private boolean isMatching(SlingHttpServletRequest request, Modification m) {
    if (ModificationType.CREATE.equals(m.getType())) {
      RequestParameter resourceType = request
          .getRequestParameter(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
      if (resourceType != null && targetResourceType.equals(resourceType.getString())) {
        LOGGER.info("Post Processing for {}  on {} ", targetResourceType, request
            .getRequestURI());
        return true;
      }
    } else {
      Session s = request.getResourceResolver().adaptTo(Session.class);
      String path = m.getSource();
      try {
        if (s.itemExists(path)) {
          Item item = s.getItem(path);
          if (item != null && item.isNode()) {
            Node n = (Node) item;

            if (n.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
                && targetResourceType.equals(n.getProperty(
                    JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
              LOGGER.info("Post Processing for {}  on {} ", targetResourceType, request
                  .getRequestURI());
              return true;
            }
          }
        }
      } catch (RepositoryException ex) {
        LOGGER.warn("Failed to resolve resource Type ", ex);
      }
    }
    return false;
  }

  /**
   * @param request
   * @param changes
   */
  protected abstract void onOther(SlingHttpServletRequest request, Modification m);

  /**
   * @param request
   * @param changes
   */
  protected abstract void onOrder(SlingHttpServletRequest request, Modification m);

  /**
   * @param request
   * @param changes
   */
  protected abstract void onMove(SlingHttpServletRequest request, Modification m);

  /**
   * @param request
   * @param m
   */
  protected abstract void onModify(SlingHttpServletRequest request, Modification m);

  /**
   * @param request
   * @param m
   */
  protected abstract void onDelete(SlingHttpServletRequest request, Modification m);

  /**
   * @param request
   * @param m
   */
  protected abstract void onCreate(SlingHttpServletRequest request, Modification m);

  /**
   * @param request
   * @param m
   */
  protected abstract void onCopy(SlingHttpServletRequest request, Modification m);

  /**
   * @return
   */
  protected abstract String getResourceType();

}
