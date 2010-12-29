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
package org.sakaiproject.nakamura.version.impl.jcr;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.resource.AbstractAllMethodsServletResourceHandler;
import org.sakaiproject.nakamura.api.resource.ServletResourceHandler;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.version.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Saves the current version of the JCR node identified by the Resource and checks out a new 
 * writeable version.
 */

@Component(metatype=true, immediate=true)
@Service(value=ServletResourceHandler.class)
@Property(name="handling.servlet",value="SaveVersionServlet")
public class SaveVersionServletHandler  extends AbstractAllMethodsServletResourceHandler {


  /**
   *
   */
  private static final long serialVersionUID = -7513481862698805983L;
  private static final Logger LOGGER = LoggerFactory.getLogger(SaveVersionServletHandler.class);

  @Reference
  protected transient VersionService versionService;
  
  /**
   * {@inheritDoc}
   * 
   */
  public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Resource resource = request.getResource();
      Node node = resource.adaptTo(Node.class);
      if (node == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      Version version = versionService.saveNode(node, request.getRemoteUser());

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");


      ExtendedJSONWriter write = new ExtendedJSONWriter(response.getWriter());
      write.object();
      write.key("versionName");
      write.value(version.getName());
      ExtendedJSONWriter.writeNodeContentsToWriter(write, version);
      write.endObject();
    } catch (RepositoryException e) {
      LOGGER.info("Failed to save version ",e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    } catch (JSONException e) {
      LOGGER.info("Failed to save version ",e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
  }

  public boolean accepts(SlingHttpServletRequest request) {
    LOGGER.info("Checing accepts ");
    return !(request.getResource() instanceof SparseContentResource);
  }

}
