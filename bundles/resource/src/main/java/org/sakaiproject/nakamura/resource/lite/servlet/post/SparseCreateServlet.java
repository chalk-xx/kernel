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
package org.sakaiproject.nakamura.resource.lite.servlet.post;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.api.resource.lite.SparseNonExistingResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet allows creating Sparse-stored children beneath a Sparse-stored path.
 * It inspects a POST to a non-existing Sling Resource, and if the new Resource's nearest
 * existing parent is stored in Sparse, then the new Resource is directed to Sparse
 * as well. If the nearest existing parent is stored elsewhere, the servlet opts out
 * and lets Sling do its default handling.
 */
@SlingServlet(methods = "POST", resourceTypes = {"sling/nonexisting"})
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Handle non-existing resources whose path puts them in sparse.")})
public class SparseCreateServlet extends SlingAllMethodsServlet implements OptingServlet {
  private static final long serialVersionUID = -6590959255525049482L;
  private static final Logger LOGGER = LoggerFactory.getLogger(SparseCreateServlet.class);
  public static final String CONTENT_TARGET_PATH_ATTRIBUTE = SparseCreateServlet.class.getName() + ".contentTargetPath";

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    final Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
    final ContentManager contentManager;
    try {
      contentManager = session.getContentManager();
    } catch (StorageClientException e) {
      LOGGER.warn("No content manager", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
    final String targetPath = (String) request.getAttribute(CONTENT_TARGET_PATH_ATTRIBUTE);
    SparseNonExistingResource resourceWrapper = new SparseNonExistingResource(request.getResource(),
        targetPath, session, contentManager);
    RequestDispatcherOptions options = new RequestDispatcherOptions();
    request.getRequestDispatcher(resourceWrapper, options).forward(request, response);
  }

  public boolean accepts(SlingHttpServletRequest request) {
    ResourceResolver resourceResolver = request.getResourceResolver();
    Resource resource = request.getResource();
    String path = resource.getPath();
    if ((path != null) && (path.length() > 0)) {
      // Search for the nearest parent.
      String nearestPath = path;
      Resource nearestResource = null;
      while ((nearestResource == null) && (nearestPath.length() > 0)) {
        nearestResource = resourceResolver.getResource(nearestPath);
        if (nearestResource == null) {
          int pos = nearestPath.lastIndexOf('/');
          if (pos > 0) {
            nearestPath = nearestPath.substring(0, pos);
          } else {
            break;
          }
        }
      }
      if (nearestResource != null) {
        if (SparseContentResource.SPARSE_CONTENT_RT.equals(nearestResource.getResourceSuperType())) {
          String childPath = path.substring(nearestPath.length());
          // Because the final path of the resolved parent Resource might differ
          // from the path we started out with, we do not use the original raw path
          // obtained from the request. For example, the original URL may specify the
          // parent path "/~somebody/private" but then be resolved to "a:somebody/private".
          Content nearestContent = nearestResource.adaptTo(Content.class);
          String contentTargetPath = nearestContent.getPath() + childPath;
          LOGGER.info("Going to create Resource {} with Content {} starting from Resource {} with child path {}",
              new Object[] {resource.getPath(), contentTargetPath, nearestResource.getPath(), childPath});
          request.setAttribute(CONTENT_TARGET_PATH_ATTRIBUTE, contentTargetPath);
          return true;
        }
      }
    }
    return false;
  }
}
