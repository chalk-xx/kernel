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
package org.sakaiproject.nakamura.search.solr;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import org.sakaiproject.nakamura.api.solr.SolrServerService;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrQuery;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;


import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;

@ServiceDocumentation(
  name = "Solr Debug Servlet",
  description = "Provide a web interface to perform arbitrary queries against Solr.",
  bindings = {
    @ServiceBinding(
      type = BindingType.PATH,
      bindings = { "/system/query" }
    )
  },
  methods = {
    @ServiceMethod(
      name = "GET",
      description = {"Send a request to Solr.<br>" +
                     "Any parameters provided will be passed through to Solr by calling setParam() on a SolrQuery object. " +
                     "Example queries:<br>" +
                     "<pre>http://localhost:8080/system/query?q=fish&sort=score asc&rows=10&indent=1\n" +
                     "http://localhost:8080/system/query?qt=/admin/luke&fl=email&numTerms=100&indent=1\n" +
                     "</pre>" +
                     "Browse the Solr Wiki at http://wiki.apache.org/solr/ for examples of query types and their corresponding parameters."
                     },
      response = {
        @ServiceResponse(code = 200, description = "The result of returning toString() on the object returned by Solr.")
      }
    )
  }
)
@Component(enabled=false)
@SlingServlet(methods = "GET", paths = "/system/query", generateComponent = false)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Perform arbitrary queries against Solr") })
public class QueryServlet extends SlingSafeMethodsServlet {

  static final long serialVersionUID = -7250872090976232073L;

  @Reference
  SolrServerService solrServerService;


  private class SolrOutputIndenter {

    final int tabwidth = 4;

    private int indent = 0;
    private int list_depth = 0;


    private boolean newline_indent(StringBuilder sb) {
      if (list_depth <= 1) {
        sb.append("\n");
        for (int i = 0; i < (indent * tabwidth); i++) {
          sb.append(" ");
        }

        return true;
      } else {
        return false;
      }
    }


    public String indent(String s) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);

        if (ch == '{') {
          sb.append(ch);
          indent++;
          newline_indent(sb);
        } else if (ch == '[') {
          sb.append(ch);
          list_depth++;
          indent++;
          newline_indent(sb);
        } else if (ch == ']') {
          indent--;
          newline_indent(sb);
          list_depth--;
          sb.append(ch);
        } else if (ch == ',') {
          sb.append(ch);
          if (newline_indent(sb) && s.charAt(i + 1) == ' ') {
            // Eat the space following the comma too.
            i++;
          }
        } else if (ch == '}') {
          indent--;
          newline_indent(sb);
          sb.append(ch);
        } else {
          sb.append(ch);
        }
      }

      return sb.toString();
    }
  }

  @Override
  protected void doGet(SlingHttpServletRequest request,
                       SlingHttpServletResponse response)
    throws ServletException, IOException {

    SolrServer server = solrServerService.getServer();

    SolrQuery q = new SolrQuery();

    @SuppressWarnings("unchecked")
    Map<String, String[]> params = request.getParameterMap();

    for (String param : params.keySet()) {
      q.setParam(param, params.get(param));
    }

    try {
      String result = server.query(q).getResponse().toString();

      if (params.get("indent") != null) {
        result = new SolrOutputIndenter().indent(result);
      }

      response.getWriter().println(result);
    } catch (Exception e) {
      throw new ServletException(e.getMessage(), e);
    }
  }
}