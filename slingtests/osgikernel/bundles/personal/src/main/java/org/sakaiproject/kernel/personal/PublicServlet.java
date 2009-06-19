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

import static org.sakaiproject.kernel.api.personal.PersonalConstants._USER_PUBLIC;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/personalPublic"
 * @scr.property name="sling.servlet.methods" values.0="GET" values.1="POST" values.2="PUT"
 *               values.3="DELETE"
 */
public class PublicServlet extends AbstractPersonalServlet {

  /**
   *
   */
  private static final long serialVersionUID = -2663916166760531044L;
  private static final Logger LOGGER = LoggerFactory.getLogger(PublicServlet.class);

  protected void hashRequest(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws IOException, ServletException {
    /*
     * Process the path to expand based on the user, then dispatch to the resource at that
     * location.
     */
    
    
    LOGGER.info("+++++++++++++++++++++++++++++++=========== In public servlet ");
    Resource baseResource = request.getResource();
    String uriPath = baseResource.getPath();
    String relativePath = uriPath.substring(_USER_PUBLIC.length());
    String[] pathParts = PathUtils.getNodePathParts(relativePath);
    String resourcePath = PathUtils.toInternalHashedPath(_USER_PUBLIC, pathParts[0], pathParts[1]);

    Resource resource = request.getResourceResolver().resolve(resourcePath);
    if (resource instanceof NonExistingResource) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND,
          "Resource does not exist (non existant)");
      return;
    }
    if (resource == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND,
          "Resource does not exist (null)");
      return;
    }
    request.getRequestDispatcher(resource).forward(request, response);

  }

}
