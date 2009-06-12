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
package org.sakaiproject.kernel.site.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceGetServlet</code>
 * 
 * @scr.component immediate="true" label="SiteGetServlet"
 *                description="Get servlet for site service"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description" value="Renders sites"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sakai/site"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.extensions" value="json"
 */
public class SiteGetJSONServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 1813123907131131930L;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Node node = request.getResource().adaptTo(Node.class);
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.node(node);
      response.setStatus(HttpServletResponse.SC_OK);
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

}
