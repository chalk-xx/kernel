/*
* Licensed to the Sakai Foundation (SF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The SF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceParameter;
import org.sakaiproject.kernel.api.doc.ServiceResponse;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.sakaiproject.kernel.util.ResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import java.io.IOException;
import java.io.UnsupportedEncodingException;
 
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
 
/**
* The <code>SearchServlet</code> uses nodes from the
*
* @scr.component immediate="true" label="BatchGetServlet"
* description="servlet to return multiple resources"
* @scr.service interface="javax.servlet.Servlet"
* @scr.property name="service.description"
* value="Bundles multiple resource requests into a single response."
* @scr.property name="service.vendor" value="The Sakai Foundation"
* @scr.property name="sling.servlet.paths" value="/system/batch/get"
* @scr.property name="sling.servlet.methods" value="GET"
*/
@ServiceDocumentation(
    name = "BatchGetServlet",
    shortDescription = "Bundles multiple resource requests into a single response.",
    description = "Allows you to fetch multiple resources in one single request.",
    bindings = @ServiceBinding(
        type = BindingType.PATH,
        bindings = "/system/batch/get"
    ),
    methods = @ServiceMethod(
        name = "GET",
        description = "Get multiple resource requests into a single response.",
        parameters = @ServiceParameter(
            name = "resources",
            description = "Multi valued parameter that contains absolute paths to the needed resources. <br />Example:" +
                "<pre>curl -d\"resources=/system/me\" -d\"resources=/devwidgets.json\" -d\"resources=/some/none/existing/resource.json\" -G http://localhost:8080/system/batch/get</pre>"
        ),
        response = {@ServiceResponse(
            code = 200,
            description = "All requests are succesfull. <br />" +
                "A JSON array is returning which holds an object for each resource. Example:" +
                "<pre>[\n" +
                "{\"path\": \"/dev/.json\", \"data\": \"{\"user\"...\"}, \n" +
                "{\"path\": \"/devwidgets.json\", \"data\": \"{\"jcr:created\":\"Wed Dec 16 2009 10:39:19 GMT+0000\",\"jcr:primaryType\":\"sling:Folder\"}\"},\n" +
                "{\"path\": \"/some/none/existing/resource.json\", \"data\": 404}\n]</pre>"
          ),
          @ServiceResponse(
            code = 400,
            description = "Either there was no 'resources' parameter found or it contained a non-absolute path."
          ),
          @ServiceResponse(
            code = 500,
            description = "Unable to get and parse all the requests."
          )
        }
    )
)
public class BatchGetServlet extends SlingAllMethodsServlet {
 
  /**
*
*/
  private static final long serialVersionUID = 9159034894038200948L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(BatchGetServlet.class);
 
  public static final String RESOURCE_PATH_PARAMETER = "resources";
 
  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    String[] requestedResources = request
        .getParameterValues(RESOURCE_PATH_PARAMETER);
    if (requestedResources == null || requestedResources.length == 0) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Must specify resources to fetch using the '"
              + RESOURCE_PATH_PARAMETER + "' parameter");
      return;
    }
 
    for (String resourcePath : requestedResources) {
      if (!resourcePath.startsWith("/")) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Resources must be absolute paths");
        return;
      }
 
      SlingRequestPathInfo pathInfo = new SlingRequestPathInfo(resourcePath,
          request.getResourceResolver());
      Resource resource = pathInfo.getResource();
      if (resource == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND,
            "No such resource: " + resourcePath);
        return;
      }
    }
 
    ExtendedJSONWriter write = new ExtendedJSONWriter(response.getWriter());
    try {
      write.array();
      for (String resourcePath : requestedResources) {
        write.object();
        write.key("path");
        write.value(resourcePath);
        write.key("data");
        try {
          outputResource(request, response, write, resourcePath);
        } catch (ResourceNotFoundException e) {
          write.value(404);
          LOGGER.warn("Unable to get data for resource: " + resourcePath, e);
        } catch (Exception e) {
          write.value(500);
          LOGGER.warn("Unable to get data for resource: " + resourcePath, e);
        }
        write.endObject();
      }
      write.endArray();
    } catch (JSONException e) {
      LOGGER.error("Error encoding output as JSON", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Unable to encode content as JSON");
    }
  }
 
  private void outputResource(SlingHttpServletRequest request,
      SlingHttpServletResponse response, ExtendedJSONWriter write,
      String resourcePath) throws ServletException, IOException, JSONException {
    ResponseWrapper responseWrapper = new ResponseWrapper(response);
 
    request.getRequestDispatcher(resourcePath)
        .forward(request, responseWrapper);
 
    outputResponseAsJSON(responseWrapper, write);
  }
 
  private void outputResponseAsJSON(ResponseWrapper responseWrapper,
      ExtendedJSONWriter write) throws UnsupportedEncodingException,
      JSONException {
    write.value(responseWrapper.getDataAsString());
    write.key("status");
    write.value(responseWrapper.getResponseStatus());
  }
 
}