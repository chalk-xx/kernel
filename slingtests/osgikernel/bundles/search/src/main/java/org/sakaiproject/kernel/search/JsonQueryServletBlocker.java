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
package org.sakaiproject.kernel.search;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>JsonQueryServletBlocker</code> blocks the default sling JsonQueryServlet
 * 
 * @scr.component immediate="true" label="JsonQueryServletBlocker"
 *                description="Blocks the default sling JsonQueryServlet."
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="Blocks the default sling JsonQueryServlet."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sling/servlet/default"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.extensions" value="json"
 * @scr.property name="sling.servlet.selectors" value="query"
 * @scr.property name="sling.servlet.prefix" value="-1"
 */
public class JsonQueryServletBlocker extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 9135814126478411413L;

  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
  }
}
