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

import static org.sakaiproject.kernel.api.search.SearchConstants.JSON_QUERY;
import static org.sakaiproject.kernel.api.search.SearchConstants.JSON_RESULTS;
import static org.sakaiproject.kernel.api.search.SearchConstants.PARAMS_ITEMS;
import static org.sakaiproject.kernel.api.search.SearchConstants.REG_PROCESSOR_NAMES;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_QUERY_LANGUAGE;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_QUERY_TEMPLATE;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_RESULTPROCESSOR;
import static org.sakaiproject.kernel.api.search.SearchConstants.SEARCH_RESULT_PROCESSOR;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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
 * @scr.reference name="SearchResultProcessor"
 *                interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 *                bind="bindSearchResultProcessor" unbind="unbindSearchResultProcessor"
 *                cardinality="0..n" policy="dynamic"
 */
public class SearchServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 4130126304725079596L;
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchServlet.class);
  private SearchResultProcessor defaultSearchProcessor = new SearchResultProcessor() {

    public void output(JSONWriter write, QueryResult result, int nitems)
        throws RepositoryException, JSONException {
      int i = 0;
      for (NodeIterator ni = result.getNodes(); i < nitems && ni.hasNext();) {
        Node resultNode = ni.nextNode();
        write.value(resultNode);
        i++;
      }
    }

  };
  private Map<String, SearchResultProcessor> processors = new ConcurrentHashMap<String, SearchResultProcessor>();
  private Map<Long, SearchResultProcessor> processorsById = new ConcurrentHashMap<Long, SearchResultProcessor>();
  private ComponentContext osgiComponentContext;
  private List<ServiceReference> delayedReferences = new ArrayList<ServiceReference>();

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
        RequestParameter nitemsRequestParameter = request
            .getRequestParameter(PARAMS_ITEMS);
        if (nitemsRequestParameter != null) {
          try {
            nitems = Integer.parseInt(nitemsRequestParameter.getString());
          } catch (NumberFormatException e) {
            LOGGER.warn("nitems parameter (" + nitemsRequestParameter.getString()
                + ") is invalid defaulting to 25 items ", e);
          }
        }

        String queryString = processQueryTemplate(request, queryTemplate, queryLanguage);
        LOGGER.info("Posting Query {} ", queryString);
        QueryManager queryManager = node.getSession().getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, queryLanguage);
        QueryResult result = query.execute();

        JSONWriter write = new JSONWriter(response.getWriter());
        write.object();
        write.key(JSON_QUERY);
        write.value(queryString);
        write.key(PARAMS_ITEMS);
        write.value(nitems);
        write.key(JSON_RESULTS);
        write.array();
        SearchResultProcessor searchProcessor = defaultSearchProcessor;
        if (node.hasProperty(SAKAI_RESULTPROCESSOR)) {
          searchProcessor = processors.get(node.getProperty(SAKAI_RESULTPROCESSOR)
              .getString());
          if (searchProcessor == null) {
            searchProcessor = defaultSearchProcessor;
          }
        }
        searchProcessor.output(write, result, nitems);
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
   * @param request
   *          the request.
   * @param queryTemplate
   *          the query template.
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
      } else if (vstart >= 0) {
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
    if (value == null) {
      return "null";
    }
    return value.replaceAll("\\\\", "\\\\").replaceAll("'", "\\\\'");
  }

  protected void bindSearchResultProcessor(ServiceReference serviceReference) {
    synchronized (delayedReferences) {
      if (osgiComponentContext == null) {
        delayedReferences.add(serviceReference);
      } else {
        addProcessor(serviceReference);
      }
    }

  }

  protected void unbindSearchResultProcessor(ServiceReference serviceReference) {
    synchronized (delayedReferences) {
      if (osgiComponentContext == null) {
        delayedReferences.remove(serviceReference);
      } else {
        removeProcessor(serviceReference);
      }
    }

  }

  /**
   * @param serviceReference
   */
  private void removeProcessor(ServiceReference serviceReference) {
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
    SearchResultProcessor processor = processorsById.get(serviceId);
    if (processor != null) {
      List<String> toRemove = new ArrayList<String>();
      for (Entry<String, SearchResultProcessor> e : processors.entrySet()) {
        if (processor.equals(e.getValue())) {
          toRemove.add(e.getKey());
        }
      }
      for (String r : toRemove) {
        processors.remove(r);
      }
    }
  }

  /**
   * @param serviceReference
   */
  private void addProcessor(ServiceReference serviceReference) {
    SearchResultProcessor processor = (SearchResultProcessor) osgiComponentContext
        .locateService(SEARCH_RESULT_PROCESSOR, serviceReference);
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);

    processorsById.put(serviceId, processor);
    String[] processorNames = OsgiUtil.toStringArray(serviceReference
        .getProperty(REG_PROCESSOR_NAMES));

    for (String processorName : processorNames) {
      processors.put(processorName, processor);
    }
  }

  protected void activate(ComponentContext componentContext) {

    synchronized (delayedReferences) {
      osgiComponentContext = componentContext;
      for (ServiceReference ref : delayedReferences) {
        addProcessor(ref);
      }
      delayedReferences.clear();
    }
  }

}
