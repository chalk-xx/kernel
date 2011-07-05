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
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.templates.TemplateService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(resourceTypes = { "sakai/template" }, methods = { "GET" })
@ServiceDocumentation(name = "FulfillTemplateServlet documentation", okForVersion = "0.11",
  shortDescription = "Fills the requested template with the parameters in the query.",
  description = {
    "Fills the requested template with the parameters in the query.",
    "You can create a template like this:",
    "<pre>curl --referer http://localhost:8080 -u admin:secretpassword -Fsling:resourceType=sakai/template -Fsakai:content-type=text/html -F\"sakai:template=Hello, $name\" http://localhost:8080/var/templates/greeting</pre>",
    "(Depending on your shell, you may need to escape the $ character in the curl command) Then, anyone can get a document based on this template like this:",
    "<pre>curl http://localhost:8080/var/templates/greeting.html?name=World</pre>",
    "We use Apache Velocity as our templating engine, so look here for a guide to templates: <a href='http://velocity.apache.org/engine/releases/velocity-1.5/user-guide.html'>http://velocity.apache.org/engine/releases/velocity-1.5/user-guide.html</a>"
  },
  bindings = @ServiceBinding(type = BindingType.TYPE, bindings = "sakai/template"),
  methods = {
    @ServiceMethod(name = "GET", description = "Fulfills the requested template with the parameters on the request.",
      response = {
        @ServiceResponse(code = HttpServletResponse.SC_OK, description = "Request has been processed successfully."),
        @ServiceResponse(code = HttpServletResponse.SC_BAD_REQUEST, description = "If the requested path is not a template node, or is missing the sakai:template property, or if the request does not have all the parameters specified by the template."),
        @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Resource could not be found."),
        @ServiceResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to process request due to a runtime error.")
      })
})
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
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
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
