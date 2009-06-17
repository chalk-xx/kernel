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
package org.sakaiproject.kernel.message;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sakai/messagestore"
 * @scr.property name="sling.servlet.methods" value="GET"
 */
public class MessageServlet extends AbstractMessageServlet {

  /**
   *
   */
  private static final long serialVersionUID = -2663916166760531044L;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.message.AbstractMessageServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  protected void hashRequest(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    try {
      /*
       * The type will be sakai/messagestore, other wise the request would not be here,
       * however the resource has not be processed. We need to reprocess the path to find
       * the first existing node, and then calculate the path from that point on, to
       * generate the virtual path. Once we have done that we need to re-resolve the path
       * and re-dispatch.
       */
      Resource baseResource = request.getResource();
      Session session = request.getResourceResolver().adaptTo(Session.class);
      String uriPath = baseResource.getPath();
      Node firstNode = JcrUtils.getFirstExistingNode(session, baseResource.getPath());
      if (firstNode == null) {
        response
            .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to find parent node of resource, even the repository base is missing");
        return;
      }
      String path = firstNode.getPath();
      String pathInfo = uriPath.substring(path.length());
      if (pathInfo.length() == 0 || "/".equals(pathInfo)) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource does not exist");
        return;
      }

      String relativePath = pathInfo.substring(1);
      String[] parts = PathUtils.getNodePathParts(relativePath);
      String resourcePath = PathUtils.toInternalHashedPath(path, parts[0], parts[1]);
      
      Resource resource = request.getResourceResolver().resolve(resourcePath);
      if ( resource instanceof NonExistingResource ) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND,"Resource does not exist (non existant)");
        return;
      }
      if ( resource == null ) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND,"Resource does not exist (null)");
        return;     
      }
      request.getRequestDispatcher(resource).forward(request, response);
      
      
    } catch (RepositoryException ex) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
    }

  }

}
