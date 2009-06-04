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
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SearchServlet</code> uses nodes from the 
 * 
 * @scr.component immediate="true" label="SearchServlet"
 *                description="a generic resource driven search servlet"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="Perfoms searchs based on the associated node."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sakai/search"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.extensions" value="json" 
 */
public class SearchServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final String RESULTS = "results";
  /**
   *
   */
  private static final String QUERY = "query";
  /**
   *
   */
  private static final String PARAMS_ITEMS = "items";
  /**
   *
   */
  private static final String SAKAI_QUERY_LANGUAGE = "sakai:query-language";
  /**
   *
   */
  private static final String SAKAI_QUERY_TEMPLATE = "sakai:query-template";
  /**
   *
   */
  private static final long serialVersionUID = 4130126304725079596L;
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchServlet.class);

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Resource resource = request.getResource();
      Node node = resource.adaptTo(Node.class);
      if (node != null && node.hasProperty(SAKAI_QUERY_TEMPLATE)) {
        String queryTemplate = node.getProperty(SAKAI_QUERY_TEMPLATE).getString();
        String queryLanguage = Query.SQL;
        if (node.hasProperty(SAKAI_QUERY_LANGUAGE)) {
          queryLanguage = node.getProperty(SAKAI_QUERY_LANGUAGE).getString();
        }
        int nitems = 25;
        RequestParameter nitemsRequestParameter = request.getRequestParameter(PARAMS_ITEMS);
        if (nitemsRequestParameter != null) {
          try {
            nitems = Integer.parseInt(nitemsRequestParameter.getString());
          } catch (NumberFormatException e) {
            LOGGER.warn("nitems parameter ("+nitemsRequestParameter.getString()+") is invalid defaulting to 25 items ",e);
          }
        }
        String queryString = processQueryTemplate(request, queryTemplate, queryLanguage);
        QueryManager queryManager = node.getSession().getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, queryLanguage);
        QueryResult result = query.execute();
        int i = 0;
        
        JSONWriter write = new JSONWriter(response.getWriter());
        write.object();
        write.key(QUERY);
        write.value(queryString);
        write.key(PARAMS_ITEMS);
        write.value(nitems);
        write.key(RESULTS);
        write.array();
        for (NodeIterator ni = result.getNodes(); i < nitems && ni.hasNext();) {
          Node resultNode = ni.nextNode();
          write.value(resultNode);
        }
        write.endArray();
        write.endObject();
      }
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Processes a template of the form select * from y where x = {q} so that strings
   * enclosed in { and } are replaced by the same property in the request.
   * 
   * @param request the request.
   * @param queryTemplate the query template.
   * @return A processed query template.
   */
  protected String processQueryTemplate(SlingHttpServletRequest request,
      String queryTemplate, String queryLanguage) {
    StringBuilder sb = new StringBuilder();
    boolean escape = false;
    int vstart = -1;
    char[] ca = queryTemplate.toCharArray();
    for (int i = 0; i < ca.length; i++) {
      char c = ca[i];
      if (escape) {
        sb.append(c);
        escape = false;
      } else if (vstart > 0) {
        if (c == '}') {
          String v = new String(ca, vstart + 1, i - vstart - 1);
          RequestParameter rp = request.getRequestParameter(v);
          if (rp != null) {
            sb.append(escapeString(rp.getString(), queryLanguage));
          }
          vstart = -1;
        }
      } else {
        switch (c) {
        case '{':
          vstart = i;
          break;
        case '\\':
          escape = true;
          break;
        default:
          sb.append(c);
        }
      }
    }
    return sb.toString();
  }

  private String escapeString(String value, String queryLanguage) {
    return value.replaceAll("\\\\", "\\\\").replaceAll("'", "\\\\'");
  }

}
