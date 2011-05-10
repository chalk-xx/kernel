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
package org.sakaiproject.nakamura.templates.servlet;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.nakamura.api.templates.TemplateService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(resourceTypes = { "sakai/template" }, methods = { "GET" })
public class FulfillTemplateServlet extends SlingSafeMethodsServlet {

  /** Default serial ID to hush IDE */
  private static final long serialVersionUID = 1L;

  @Reference
  protected TemplateService templateService;


  @Override
  @SuppressWarnings("unchecked")
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    try {
      // get text of the template
      Resource requestResource = request.getResource();
      Node templateNode = requestResource.adaptTo(Node.class);
      String templateText = "";
      if (templateNode != null && templateNode.hasProperty("sakai:template")) {
        templateText = templateNode.getProperty("sakai:template").getString();
      } else {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Requested path does not contain a Sakai template");
        return;
      }
      Collection<String> missingTerms = templateService.missingTerms(
          request.getRequestParameterMap(), templateText);
      if (!missingTerms.isEmpty()) {
        response.sendError(
            HttpServletResponse.SC_BAD_REQUEST,
            "Your request is missing parameters for the template: "
                + StringUtils.join(missingTerms, ", "));
      }

      if (templateNode.hasProperty("sakai:content-type")) {
        response.setContentType(templateNode.getProperty("sakai:content-type")
            .getString());
      }

      PrintWriter writer = response.getWriter();
      writer.append(templateService.evaluateTemplate(request.getParameterMap(),
          templateText));
      writer.flush();


    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          e.getLocalizedMessage());
    }

  }

}
