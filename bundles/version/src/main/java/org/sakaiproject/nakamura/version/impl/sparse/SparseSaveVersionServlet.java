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
package org.sakaiproject.nakamura.version.impl.sparse;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Saves the current version of the JCR node identified by the Resource and checks out a new 
 * writeable version.
 */

@ServiceDocumentation(name="Save a version Servlet",
    description="Saves a new version of a resource",
    shortDescription="List versions of a resource",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sling/servlet/default", "selector save"},
        selectors=@ServiceSelector(name="save", description="Saves  the current version of a resource creating a new version."),
        extensions=@ServiceExtension(name="json", description="A json tree containing the name of the saved version.")),
    methods=@ServiceMethod(name="POST",
        description={"Lists previous versions of a resource. The url is of the form " +
            "http://host/resource.save.json ",
            "Example<br>" +
            "<pre>curl http://localhost:8080/sresource/resource.save.json</pre>"
        },
        response={
          @ServiceResponse(code=200,description="Success a body is returned containing a json ove the name of the version saved"),
          @ServiceResponse(code=404,description="Resource was not found."),
          @ServiceResponse(code=500,description="Failure with HTML explanation.")}
    )) 

@SlingServlet(resourceTypes = "sling/servlet/default", methods = "POST", selectors = "save", extensions = "json")
public class SparseSaveVersionServlet extends SlingAllMethodsServlet implements OptingServlet {


  /**
   *
   */
  private static final long serialVersionUID = -7513481862698805983L;
  private static final Logger LOGGER = LoggerFactory.getLogger(SparseSaveVersionServlet.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Resource resource = request.getResource();
      Content content = resource.adaptTo(Content.class);
      ContentManager contentManager = resource.adaptTo(ContentManager.class);
      if (content == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      
      String versionId  = contentManager.saveVersion(content.getPath());
      Content savedVersion = contentManager.getVersion(content.getPath(), versionId);

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");


      ExtendedJSONWriter write = new ExtendedJSONWriter(response.getWriter());
      write.object();
      write.key("versionName");
      write.value(versionId);
      ExtendedJSONWriter.writeNodeContentsToWriter(write, savedVersion);
      write.endObject();
    } catch (JSONException e) {
      LOGGER.info("Failed to save version ",e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    } catch (StorageClientException e) {
      LOGGER.info("Failed to save version ",e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    } catch (AccessDeniedException e) {
      LOGGER.info("Failed to save version ",e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
  }

  public boolean accepts(SlingHttpServletRequest request) {
    return (request.getResource() instanceof SparseContentResource);
  }

}
