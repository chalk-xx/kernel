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
package org.sakaiproject.nakamura.batch;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.templates.TemplateService;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = { "GET" }, resourceTypes={"sakai/batch-template"},  selectors={"batch"}, extensions={"json"})
@ServiceDocumentation(name = "BatchTemplateServlet", okForVersion = "0.11",
    shortDescription = "Bundles multiple requests into a single response, using a template",
    description = "Allows multiple requests to be executed in a single request.",
    bindings = @ServiceBinding(
        type = BindingType.TYPE,
        selectors= {@ServiceSelector(name="batch")},
        extensions= {@ServiceExtension(name="json")},
        bindings = "sakai/batch-template"
    ),
    methods = @ServiceMethod(
        name = "GET",
        description = "Get multiple request responses into a single response, only GET operations.",
        response = {
          @ServiceResponse(code = 200,
            description = {
              "All requests are successful. <br />",
               "A JSON array is returned containing an object for each resource. Example:",
               "<pre>[\n",
               "{\"url\": \"/~admin/public/authprofile.json\",\n \"body\": \"{\"user\"...\",\n \"success\":true, \"status\": 200,\n \"headers\":{\"Content-Type\":\"application/json\"}\n} \n]</pre>"
            }
          ),
          @ServiceResponse(code = 400, description = "The JSON object for the 'requests' parameter was malformed."),
          @ServiceResponse(code = 500,description = "Unable to get and parse all requests.")
        }))
public class BatchTemplateServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 419598445499567027L;

  protected TemplateService templateService;
  
  protected static final String REQUESTS_PARAMETER = "requests";
  
  private BatchHelper helper = new BatchHelper();

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    try {
      batchRequest(request, response, false);
    } catch (RepositoryException e) {
      throw new ServletException(e.getMessage(),e);
    }
  }


  /**
   * Takes the original request and starts the batching.
   *
   * @param request
   * @param response
   * @throws IOException
   * @throws ServletException 
   * @throws RepositoryException 
   */
  protected void batchRequest(SlingHttpServletRequest request,
      SlingHttpServletResponse response, boolean allowModify) throws IOException, ServletException, RepositoryException {
    // Grab the JSON block out of it and convert it to RequestData objects we can use.
    Resource resource = request.getResource();
    Node node = resource.adaptTo(Node.class);
    if ( node == null || !node.hasProperty("sakai:template") ) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not a batch template");
      return;
    }
    String template = node.getProperty("sakai:template").getString();
    request.getParameterMap();
    @SuppressWarnings("unchecked")
    String json = templateService.evaluateTemplate(request.getParameterMap(), template);
    helper.batchRequest(request, response, json, allowModify);
  }



}
