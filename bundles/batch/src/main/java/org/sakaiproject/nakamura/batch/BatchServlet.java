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
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = { "POST" }, generateService = true, paths = { "/system/batch" })
@ServiceDocumentation(name = "BatchServlet", okForVersion = "0.11",
    shortDescription = "Bundles multiple requests into a single response.",
    description = "Allows multiple requests to be executed in a single request.",
    bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/system/batch"),
    methods = {
      @ServiceMethod(name = "POST",
        description = "Get multiple request responses into a single response. It can do GET, POST and DELETE everything is defined in the json block.",
        parameters = @ServiceParameter(
          name = "requests",
          description = "A JSON string representing a request. <br />Example:" +
            "<pre>[{  \"url\" : \"/foo/bar\",  \"method\" : \"POST\",  \"parameters\" : {    \"val\" : 123,    \"val@TypeHint\" : \"Long\"  }},{  \"url\" : \"/~admin/public/authprofile.json\",  \"method\" : \"GET\"}]</pre>"
        ),
        response = {
          @ServiceResponse(code = 200,
            description = {
              "All requests are successful. <br />",
              "A JSON array is returned containing an object for each resource. Example:",
              "<pre>[\n",
              "{\"url\": \"/~admin/public/authprofile.json\",\n \"body\": \"{\"user\"...\",\n \"success\":true, \"status\": 200,\n \"headers\":{\"Content-Type\":\"application/json\"}\n} \n]</pre>"
            }),
          @ServiceResponse(code = 400, description = "The JSON object for the 'requests' parameter was malformed."),
          @ServiceResponse(code = 500, description = "Unable to get and parse all requests.")
        })
    })
public class BatchServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = 419598445499567027L;

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
    batchRequest(request, response, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    batchRequest(request, response, true);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doDelete(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doDelete(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPut(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPut(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }

  /**
   * Takes the original request and starts the batching.
   *
   * @param request
   * @param response
   * @throws IOException
   * @throws ServletException 
   */
  protected void batchRequest(SlingHttpServletRequest request,
      SlingHttpServletResponse response, boolean allowModify) throws IOException, ServletException {
    // Grab the JSON block out of it and convert it to RequestData objects we can use.
    String json = request.getParameter(REQUESTS_PARAMETER);    
    helper.batchRequest(request, response, json, allowModify);
  }



}
