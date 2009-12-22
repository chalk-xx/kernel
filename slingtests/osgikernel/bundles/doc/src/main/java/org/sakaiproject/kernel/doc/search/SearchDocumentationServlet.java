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
package org.sakaiproject.kernel.doc.search;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.DocumentationConstants;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceParameter;
import org.sakaiproject.kernel.api.doc.ServiceResponse;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;

@SlingServlet(paths = { "/system/doc/search" }, methods = { "GET" })
@ServiceDocumentation(name = "SearchDocumentationServlet", description = "Provides auto documentation of search nodes currently in the repository. Documentation will use the "
    + "node properties."
    + " Requests to this servlet take the form /system/doc/search?p=&lt;searchnodepath&gt where <em>searchnodepath</em>"
    + " is the absolute path of the searchnode deployed into the JCR repository. If the node is "
    + "not present a 404 will be retruned, if the node is present, it will be interogated to extract "
    + "documentation from the node. All documentation is assumed to be HTML encoded. If the browser is "
    + "directed to <a href=\"/system/doc/search\" >/system/doc/search</a> a list of all the search nodes in the system will be displayed ", shortDescription = "Documentation for all the searchnodes in the repository. ", url = "/system/doc/search", bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/system/doc/search"), methods = { @ServiceMethod(name = "GET", description = "GETs to this servlet will produce documentation for the searchnode, "
    + "or an index of all searchnodes.", parameters = @ServiceParameter(name = "p", description = "The absolute path to a searchnode to display the documentation for"), response = {
    @ServiceResponse(code = 200, description = "html page for the requested resource"),
    @ServiceResponse(code = 404, description = "Search node not found") }) })
public class SearchDocumentationServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -5820041368602931242L;
  private static final String PATH_PARAM = "p";

  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {

    RequestParameter path = request.getRequestParameter(PATH_PARAM);
    Session session = request.getResourceResolver().adaptTo(Session.class);
    PrintWriter writer = response.getWriter();

    if (path != null) {
      writeSearchInfo(path.getString(), session, writer);
    } else {
      writeSearchNodes(session, writer);
    }
  }

  private void writeSearchNodes(Session session, PrintWriter writer) {
    try {
      // Write the HTML header.
      writer.append(DocumentationConstants.HTML_HEADER);

      // Begin list
      writer.append("<h1>Search nodes</h1>");
      writer.append("<ul>");

      QueryManager qm = session.getWorkspace().getQueryManager();
      Query q = qm.createQuery("//*[@sling:resourceType='sakai/search']",
          Query.XPATH);
      QueryResult result = q.execute();
      NodeIterator iterator = result.getNodes();
      while (iterator.hasNext()) {
        Node node = iterator.nextNode();
        SearchDocumentation doc = new SearchDocumentation(node);

        writer.append("<li><a href=\"");
        writer.append(DocumentationConstants.PREFIX + "/search");
        writer.append("?p=");
        writer.append(doc.getPath());
        writer.append("\">");
        if (doc.getTitle() == null || "".equals(doc.getTitle())) {
          writer.append(doc.getPath());
        } else {
          writer.append(doc.getTitle());
        }
        writer.append("</a><p>");
        writer.append(doc.getShortDescription());
        writer.append("</p></li>");

      }

      // End list
      writer.append("</ul>");

      // Footer
      writer.append(DocumentationConstants.HTML_FOOTER);
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  private void writeSearchInfo(String path, Session session, PrintWriter writer) {
    try {
      Node node = (Node) session.getItem(path);
      SearchDocumentation doc = new SearchDocumentation(node);
      writer.append(DocumentationConstants.HTML_HEADER);
      writer.append("<h1>Search node: ");
      writer.append(doc.getTitle());
      writer.append("</h1>");
      doc.send(writer);
      writer.append(DocumentationConstants.HTML_FOOTER);
    } catch (PathNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

}
