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

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Allows for bundled POST requests.
 * 
 * Will be fixed soon.
 */
public class BatchPostServlet extends HttpServlet {

  private static final long serialVersionUID = -7984383562558102040L;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String json = request.getParameter("p");
    if (json != null) {
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
            "Failed to parse the JSON parameter");
      }

      try {
        JSONWriter write = new JSONWriter(response.getWriter());
        write.array();

        for (RequestData operation : operations) {
          doRequest(request, response, operation, write);
        }
        write.endArray();
      } catch (JSONException e) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Failed to write JSON response");
      }

    }

  }

  /**
   * Performs a POST request with the data contained in operation. Writes the result of
   * the request back to the jsonwriter.
   * 
   * @param request
   * @param response
   * @param operation
   * @param write
   * @throws JSONException
   */
  private void doRequest(HttpServletRequest request,
      HttpServletResponse response, RequestData operation, JSONWriter write)
      throws JSONException {
    /* 

    RequestWrapper requestWrapper = new RequestWrapper(request);
    requestWrapper.setPostOperation(operation);
    ResponseWrapper responseWrapper = new ResponseWrapper(response);

    try {
      request.getRequestDispatcher(operation.getUrl()).forward(requestWrapper,
          responseWrapper);

      writeResponse(write, operation.getUrl(), responseWrapper
          .getDataAsString(), responseWrapper.getStatus());
    } catch (ServletException e) {
      writeResponse(write, operation.getUrl(), "", 500);
    } catch (IOException e) {
      writeResponse(write, operation.getUrl(), "", 500);
    }
    */

  }

  /**
   * Returns a JSON object with 3 keys: url, body, status
   * 
   * @param write
   * @param url
   * @param body
   * @param status
   * @throws JSONException
   */
  private void writeResponse(JSONWriter write, String url, String body,
      int status) throws JSONException {
    write.object();
    write.key("url");
    write.value(url);
    write.key("body");
    write.value(body);
    write.key("status");
    write.value(status);
    write.endObject();
  }
}
