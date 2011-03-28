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
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.SolrQueryResponseWrapper;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.Query.Type;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;

/**
 * A servlet for making solr queries. This is intended for development and troubleshooting only
 * Since you wouldn't want open access to everything in solr.
 *
 * It is disabled by default, so you have to turn it on in the felix web console.
 */
@Component(enabled=false)
@SlingServlet(methods={"GET"}, paths = { "/system/query" }, generateComponent = false)
public class QueryServlet extends SlingSafeMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 15682213804941716L;
  /**
   * Reference uses property set on NodeSearchResultProcessor. Other processors can become
   * the default by setting {@link SearchResultProcessor.DEFAULT_PROCESOR_PROP} to true.
   */
  private static final String DEFAULT_BATCH_SEARCH_PROC_TARGET = "(&("
    + SolrSearchBatchResultProcessor.DEFAULT_BATCH_PROCESSOR_PROP + "=true))";
@Reference(target = DEFAULT_BATCH_SEARCH_PROC_TARGET)
protected transient SolrSearchBatchResultProcessor defaultSearchBatchProcessor;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");
    RequestParameter queryParameter = request.getRequestParameter("q");
    if (queryParameter == null) {
      response.sendError(400, "A query parameter 'q' is required to service this request.");
      return;
    }
    Query query = new Query(Type.SOLR, queryParameter.getString(), null);

    SolrSearchResultSet rs = null;
    try {
      rs = defaultSearchBatchProcessor.getSearchResultSet(request, query);
    } catch (SolrSearchException e) {
      response.sendError(e.getCode(), e.getMessage());
      return;
    }

    PrintWriter w = response.getWriter();
    w.write(((SolrQueryResponseWrapper)rs).getQueryResponse().toString());
  }

}
