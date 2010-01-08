/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.kernel.batch;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This filter will look for batch requests.
 * 
 * @scr.component metatype="no"
 * @scr.property name="filter.scope" value="request" private="true"
 * @scr.property name="filter.order" value="-2500" type="Integer" private="true"
 * @scr.service interface="javax.servlet.Filter"
 */
public class BatchFilter implements Filter {

  /**
   *
   */
  private static final long serialVersionUID = 9159034894038200948L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(BatchFilter.class);

  private static final String REQUESTS_PARAMETER = "requests";
  private static final String IS_BATCH_PARAMETER = "isBatch";

  public void doFilter(ServletRequest req, ServletResponse resp,
      FilterChain chain) throws IOException, ServletException {

    // Grab the HTTP request;
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;

    // We only check this request if it is a POST and it has a parameter isBatch = true
    if (request.getMethod().equals("POST")
        && "true".equals(request.getParameter(IS_BATCH_PARAMETER))) {

      // Grab the JSON block out of it and convert it to RequestData objects we can use.
      String json = request.getParameter(REQUESTS_PARAMETER);
      List<RequestData> operations = new ArrayList<RequestData>();
      try {
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
          JSONObject obj = arr.getJSONObject(i);
          RequestData operation = new RequestData(obj);
          operations.add(operation);
        }
      } catch (JSONException e) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Failed to parse the " + REQUESTS_PARAMETER + " parameter");
        LOGGER.warn("Failed to parse the " + REQUESTS_PARAMETER + " parameter");
        e.printStackTrace();
      }

      // Loop over the requests and handle each one.
      try {
        JSONWriter write = new JSONWriter(response.getWriter());
        write.array();

        for (RequestData operation : operations) {
          doRequest(request, response, operation, write);
        }
        write.endArray();
      } catch (JSONException e) {
        LOGGER.warn("Failed to create a JSON response");
        e.printStackTrace();
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Failed to write JSON response");
      }

    } else {
      // We don't have anything to do with this request.
      chain.doFilter(request, response);
    }
  }

  private void doRequest(HttpServletRequest request,
      HttpServletResponse response, RequestData requestData, JSONWriter write)
      throws JSONException {

    // Wrap the request and response so we can read them.
    RequestWrapper requestWrapper = new RequestWrapper(request);
    requestWrapper.setRequestData(requestData);
    ResponseWrapper responseWrapper = new ResponseWrapper(response);

    try {
      // Get the response
      request.getRequestDispatcher(requestData.getUrl()).forward(
          requestWrapper, responseWrapper);

      // Write the response (status, headers, body) back to the client.
      writeResponse(write, responseWrapper, requestData);
    } catch (ServletException e) {
      writeFailedRequest(write, requestData);
    } catch (IOException e) {
      writeFailedRequest(write, requestData);
    }

  }

  private void writeResponse(JSONWriter write, ResponseWrapper responseWrapper,
      RequestData requestData) throws JSONException {
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

  private void writeFailedRequest(JSONWriter write, RequestData requestData)
      throws JSONException {
    write.object();
    write.key("url");
    write.value(requestData.getUrl());
    write.key("succes");
    write.value(false);
    write.endObject();
  }

  public void init(FilterConfig filterConfig) throws ServletException {
  }

  public void destroy() {
  }

}