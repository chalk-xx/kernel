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

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.util.RequestInfo;
import org.sakaiproject.kernel.util.RequestWrapper;
import org.sakaiproject.kernel.util.ResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = { "GET", "POST", "PUT", "DELETE" }, generateService = true, paths = { "/system/batch" })
public class BatchServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = 419598445499567027L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(BatchServlet.class);

  protected static final String REQUESTS_PARAMETER = "requests";

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    hashRequest(request, response);
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
    hashRequest(request, response);
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
    hashRequest(request, response);
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
    hashRequest(request, response);
  }

  /**
   * Takes the original request and starts the batching.
   * 
   * @param request
   * @param response
   * @throws IOException
   */
  protected void hashRequest(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws IOException {
    // Grab the JSON block out of it and convert it to RequestData objects we can use.
    String json = request.getParameter(REQUESTS_PARAMETER);
    List<RequestInfo> batchedRequests = new ArrayList<RequestInfo>();
    try {
      JSONArray arr = new JSONArray(json);
      for (int i = 0; i < arr.length(); i++) {
        JSONObject obj = arr.getJSONObject(i);
        RequestInfo r = new RequestInfo(obj);
        batchedRequests.add(r);
      }
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Failed to parse the " + REQUESTS_PARAMETER + " parameter");
      LOGGER.warn("Failed to parse the " + REQUESTS_PARAMETER + " parameter");
      e.printStackTrace();
      return;
    }

    // Loop over the requests and handle each one.
    try {
      StringWriter sw = new StringWriter();
      JSONWriter write = new JSONWriter(sw);
      write.array();

      for (RequestInfo r : batchedRequests) {
        doRequest(request, response, r, write);
      }
      write.endArray();
      response.setHeader("Content-Type", "application/json");
      response.getWriter().write(sw.getBuffer().toString());
    } catch (JSONException e) {
      LOGGER.warn("Failed to create a JSON response");
      e.printStackTrace();
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to write JSON response");
    }
  }

  private void doRequest(SlingHttpServletRequest request,
      SlingHttpServletResponse response, RequestInfo requestInfo,
      JSONWriter write) throws JSONException {

    // Wrap the request and response so we can read them.
    RequestWrapper requestWrapper = new RequestWrapper(request);
    requestWrapper.setRequestInfo(requestInfo);
    ResponseWrapper responseWrapper = new ResponseWrapper(response);

    try {
      // Get the response
      request.getRequestDispatcher(requestInfo.getUrl()).forward(
          requestWrapper, responseWrapper);

      // Write the response (status, headers, body) back to the client.
      writeResponse(write, responseWrapper, requestInfo);
    } catch (ServletException e) {
      writeFailedRequest(write, requestInfo);
    } catch (IOException e) {
      writeFailedRequest(write, requestInfo);
    }

  }

  private void writeResponse(JSONWriter write, ResponseWrapper responseWrapper,
      RequestInfo requestData) throws JSONException {
    try {
      String body = responseWrapper.getDataAsString();
      write.object();
      write.key("url");
      write.value(requestData.getUrl());
      write.key("succes");
      write.value(true);
      write.key("body");
      write.value(body);
      write.key("status");
      write.value(responseWrapper.getResponseStatus());
      write.key("headers");
      write.object();
      Dictionary<String, String> headers = responseWrapper.getResponseHeaders();
      Enumeration<String> keys = headers.keys();
      while (keys.hasMoreElements()) {
        String k = keys.nextElement();
        write.key(k);
        write.value(headers.get(k));
      }
      write.endObject();
      write.endObject();
    } catch (UnsupportedEncodingException e) {
      writeFailedRequest(write, requestData);
    }
  }

  private void writeFailedRequest(JSONWriter write, RequestInfo requestData)
      throws JSONException {
    write.object();
    write.key("url");
    write.value(requestData.getUrl());
    write.key("succes");
    write.value(false);
    write.endObject();
  }

}
