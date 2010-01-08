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
package org.sakaiproject.kernel.batch;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceParameter;
import org.sakaiproject.kernel.api.doc.ServiceResponse;
import org.sakaiproject.kernel.util.URIExpander;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Delete multiple resource requests and give a useful response
 * 
 * @scr.component immediate="true" label="BatchDeleteServlet"
 *                description="servlet to delete multiple resources"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="Delete multiple resource requests and give a useful response."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.paths" value="/system/batch/delete"
 * @scr.property name="sling.servlet.methods" value="POST"
 */
@ServiceDocumentation(
    name = "BatchDeleteServlet",
    shortDescription = "Deletes multiple resource in one request.",
    description = "Allows you to delete multiple resources in one single request.",
    bindings = @ServiceBinding(
        type = BindingType.PATH,
        bindings = "/system/batch/delete"
    ),
    methods = @ServiceMethod(
        name = "POST", 
        description = "Delete multiple resource requests in a single request.",
        parameters = @ServiceParameter(
            name = "resources",
            description = "Multi valued parameter that contains absolute paths to the resources. <br />Example:" +
                "<pre>curl -d\"resources=/_user/public/admin/fileA.doc\" -d\"resources=/_user/public/admin/fileA.doc\" -G http://admin:admin@localhost:8080/system/batch/delete</pre>"
        ),
        response = {@ServiceResponse(
            code = 200,
            description = "Looped over all the resources and attempted to delete each one.<br />" +
            		"This will return a JSON array with an object for each resource. This object will have a path and succes key.<br />" +
            		"The success value can be one of the following:" +
            		"<ul><li>200: Resource was removed succesfully.</li>" +
            		"<li>401: Current user does not have appropriate accessrights to this resource.</li>" +
            		"<li>404: This resource was not found.</li>" +
            		"<li>500: Something went wrong.</li>"
          ),
          @ServiceResponse(
              code = 500,
              description = "Unable to parse a proper JSON string."
            )
        }
    )
)
public class BatchDeleteServlet extends SlingAllMethodsServlet {

  public static final Logger log = LoggerFactory.getLogger(BatchDeleteServlet.class);
  private static final long serialVersionUID = 6387824420269087079L;
  public static final String RESOURCE_PATH_PARAMETER = "resources";

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    String[] requestedResources = request.getParameterValues(RESOURCE_PATH_PARAMETER);
    if (requestedResources == null || requestedResources.length == 0) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Must specify resources to fetch using the '" + RESOURCE_PATH_PARAMETER
              + "' parameter");
      return;
    }
    Session session = request.getResourceResolver().adaptTo(Session.class);
    ExtendedJSONWriter write = new ExtendedJSONWriter(response.getWriter());
    try {
      write.array();
      for (String resourcePath : requestedResources) {
        write.object();
        write.key("path");
        write.value(resourcePath);
        write.key("succes");
        try {
          removeResource(resourcePath, session, request);
          write.value(200);
        } catch (AccessDeniedException e) {
          write.value(401);
        } catch (PathNotFoundException e) {
          write.value(404);
        } catch (RepositoryException e) {
          write.value(500);
        }
        write.endObject();
      }
      write.endArray();
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to parse JSON");
    }
  }

  /**
   * Removes a resource
   * 
   * @param resourcePath
   * @param session
   * @throws RepositoryException
   */
  private void removeResource(String resourcePath, Session session,
      SlingHttpServletRequest request) throws AccessDeniedException,
      PathNotFoundException, RepositoryException {
    try {
      if (session.itemExists(resourcePath)) {
        Item i = session.getItem(resourcePath);
        i.remove();
        session.save();
      } else {
        // The path doesn't exists in JCR, maybe it exists in a bigstore..
        String absPath = URIExpander.expandStorePath(session, resourcePath);

        log.info("Trying to delete: " + absPath);

        if (session.itemExists(absPath)) {
          Item i = session.getItem(absPath);
          i.remove();
          session.save();
        } else {
          throw new PathNotFoundException();
        }
      }
    } catch (AccessDeniedException e) {
      throw e;
    }

  }
}
