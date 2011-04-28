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
package org.sakaiproject.nakamura.activity;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "RecentActivityServlet", shortDescription = "Creates a feed of recent system activity.", description = "Creates a feed of recent system activity in JSON format.", bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/var/search/public/recentactivity"), methods = @ServiceMethod(name = "GET", description = "Returns a feed of recent system activity.", response = {
    @ServiceResponse(code = 200, description = "Request for information was successful. <br />"),
    @ServiceResponse(code = 401, description = "Unauthorized: credentials provided were not acceptable to return information for."),
    @ServiceResponse(code = 500, description = "Unable to return information about current user.") }))
@SlingServlet(paths = { "/var/search/public/recentactivity" }, generateComponent = true, generateService = true, methods = { "GET" })
public class RecentActivityServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -3786472219389695181L;
  private static final Logger LOG = LoggerFactory.getLogger(RecentActivityServlet.class);

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      List<ValueMap> results = getResults();
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      PrintWriter w = response.getWriter();
      ExtendedJSONWriter writer = new ExtendedJSONWriter(w);
      writer.object();
      // pages info
      writer.key("items");
      writer.value(255);

      writer.key("results");
      writer.array();
      for(ValueMap result : results) {
        ExtendedJSONWriter.writeValueMap(writer, result);
      }
      writer.endArray();
      writer.key("total");
      writer.value(Integer.valueOf(results.size()));

      writer.endObject();
    } catch (JSONException e) {
      LOG.error("Failed to create proper JSON response in /var/search/public/recentactivity", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to create proper JSON response.");
    }

  }

  private List<ValueMap> getResults() {
    List<ValueMap> rv = new ArrayList<ValueMap>();
    Map<String, Object> aResult = new HashMap<String, Object>();
    aResult.put("iconimage", "http://example.com/images/cara.jpg");
    aResult.put("who", "Cara McCann");
    aResult.put("what", "Uploaded a new version of:");
    aResult.put("with", "Programming Algorithm");
    rv.add(new ValueMapDecorator(aResult));
    
    aResult = new HashMap<String,Object>();
    aResult.put("iconimage", "http://example.com/images/sarah.jpg");
    aResult.put("who", "Sarah Primrose");
    aResult.put("what", "thanks for this!!");
    aResult.put("with", "Lessons Learned");
    rv.add(new ValueMapDecorator(aResult));
    
    aResult.put("iconimage", "http://example.com/images/stuart.jpg");
    aResult.put("who", "Stuart Mario");
    aResult.put("what", "Added new content to Energy Sustainability group");
    aResult.put("with", "Sustainable Earth");
    rv.add(new ValueMapDecorator(aResult));
    
    aResult.put("iconimage", "http://example.com/images/mark.jpg");
    aResult.put("who", "Mark Finlay");
    aResult.put("what", "This is a great example of the kind of journalism...");
    aResult.put("with", "Responsible Science");
    rv.add(new ValueMapDecorator(aResult));
    
    aResult.put("iconimage", "http://example.com/images/kyle.jpg");
    aResult.put("who", "Kyle Manson");
    aResult.put("what", "Uploaded a new version of:");
    aResult.put("with", "Programming Algorithm");
    rv.add(new ValueMapDecorator(aResult));
    return rv;
  }
}