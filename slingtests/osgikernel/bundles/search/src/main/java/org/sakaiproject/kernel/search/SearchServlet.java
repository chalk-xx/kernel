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
import static org.sakaiproject.kernel.api.search.SearchConstants.PARAMS_ITEMS_PER_PAGE;
import static org.sakaiproject.kernel.api.search.SearchConstants.PARAMS_PAGE;
import static org.sakaiproject.kernel.api.search.SearchConstants.REG_PROCESSOR_NAMES;
import static org.sakaiproject.kernel.api.search.SearchConstants.REG_PROVIDER_NAMES;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_PROPERTY_PROVIDER;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_QUERY_LANGUAGE;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_QUERY_TEMPLATE;
import static org.sakaiproject.kernel.api.search.SearchConstants.SAKAI_RESULTPROCESSOR;
import static org.sakaiproject.kernel.api.search.SearchConstants.SEARCH_PROPERTY_PROVIDER;
import static org.sakaiproject.kernel.api.search.SearchConstants.SEARCH_RESULT_PROCESSOR;
import static org.sakaiproject.kernel.api.search.SearchConstants.SEARCH_BATCH_RESULT_PROCESSOR;
import static org.sakaiproject.kernel.api.search.SearchConstants.REG_BATCH_PROCESSOR_NAMES;
import static org.sakaiproject.kernel.api.search.SearchConstants.TOTAL;

import org.apache.jackrabbit.util.ISO9075;
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
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.search.SearchBatchResultProcessor;
import org.sakaiproject.kernel.api.search.SearchConstants;
import org.sakaiproject.kernel.api.search.SearchPropertyProvider;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
 *                bind="bindSearchResultProcessor"
 *                unbind="unbindSearchResultProcessor" cardinality="0..n"
 *                policy="dynamic"
 * @scr.reference name="SearchBatchResultProcessor"
 *                interface="org.sakaiproject.kernel.api.search.SearchBatchResultProcessor"
 *                bind="bindSearchBatchResultProcessor"
 *                unbind="unbindSearchBatchResultProcessor" cardinality="0..n"
 *                policy="dynamic"
 * @scr.reference name="SearchPropertyProvider"
 *                interface="org.sakaiproject.kernel.api.search.SearchPropertyProvider"
 *                bind="bindSearchPropertyProvider"
 *                unbind="unbindSearchPropertyProvider" cardinality="0..n"
 *                policy="dynamic"
 */
public class SearchServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 4130126304725079596L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(SearchServlet.class);
  private SearchResultProcessor defaultSearchProcessor = new SearchResultProcessor() {
    public void writeNode(JSONWriter write, Node resultNode)
        throws JSONException, RepositoryException {
      write.value(resultNode);
    }
  };
  
  private SearchBatchResultProcessor defaultSearchBatchProcessor = new SearchBatchResultProcessor() {

    public void writeNodeIterator(JSONWriter write, NodeIterator nodeIterator)
        throws JSONException, RepositoryException {
      while (nodeIterator.hasNext()) {
        write.value(nodeIterator.nextNode());
      }
    }
    
  };
  
  private Map<String, SearchBatchResultProcessor> batchProcessors = new ConcurrentHashMap<String, SearchBatchResultProcessor>();
  private Map<Long, SearchBatchResultProcessor> batchProcessorsById = new ConcurrentHashMap<Long, SearchBatchResultProcessor>();

  
  private Map<String, SearchResultProcessor> processors = new ConcurrentHashMap<String, SearchResultProcessor>();
  private Map<Long, SearchResultProcessor> processorsById = new ConcurrentHashMap<Long, SearchResultProcessor>();

  private Map<String, SearchPropertyProvider> propertyProvider = new ConcurrentHashMap<String, SearchPropertyProvider>();
  private Map<Long, SearchPropertyProvider> propertyProviderById = new ConcurrentHashMap<Long, SearchPropertyProvider>();

  private ComponentContext osgiComponentContext;
  private List<ServiceReference> delayedReferences = new ArrayList<ServiceReference>();
  private List<ServiceReference> delayedPropertyReferences = new ArrayList<ServiceReference>();
  private List<ServiceReference> delayedBatchReferences = new ArrayList<ServiceReference>();

  protected void output(JSONWriter write, NodeIterator resultNodes, long start,
      long end) throws RepositoryException, JSONException {
  }

  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    try {
      Resource resource = request.getResource();
      Node node = resource.adaptTo(Node.class);
      if (node != null && node.hasProperty(SAKAI_QUERY_TEMPLATE)) {
        String queryTemplate = node.getProperty(SAKAI_QUERY_TEMPLATE)
            .getString();
        String queryLanguage = Query.SQL;
        if (node.hasProperty(SAKAI_QUERY_LANGUAGE)) {
          queryLanguage = node.getProperty(SAKAI_QUERY_LANGUAGE).getString();
        }
        String propertyProviderName = null;
        if (node.hasProperty(SAKAI_PROPERTY_PROVIDER)) {
          propertyProviderName = node.getProperty(SAKAI_PROPERTY_PROVIDER)
              .getString();
        }
        
        
        int nitems = intRequestParameter(request, PARAMS_ITEMS_PER_PAGE, 25);
        int offset = intRequestParameter(request, PARAMS_PAGE, 0) * nitems;

        String queryString = processQueryTemplate(request, queryTemplate,
            queryLanguage, propertyProviderName);

        LOGGER.info("Posting Query {} ", queryString);
        QueryManager queryManager = node.getSession().getWorkspace()
            .getQueryManager();
        Query query = queryManager.createQuery(queryString, queryLanguage);
        QueryResult result = query.execute();

        JSONWriter write = new JSONWriter(response.getWriter());
        write.object();
        write.key(JSON_QUERY);
        write.value(queryString);
        write.key(PARAMS_ITEMS_PER_PAGE);
        write.value(nitems);
        NodeIterator resultNodes = result.getNodes();
        write.key(TOTAL);
        long total = resultNodes.getSize();
        write.value(total);
        write.key(JSON_RESULTS);
        write.array();
        
        SearchBatchResultProcessor searchBatchProcessor = defaultSearchBatchProcessor;
        if (node.hasProperty(SearchConstants.SAKAI_BATCHRESULTPROCESSOR)) {
          searchBatchProcessor = batchProcessors.get(node.getProperty(SearchConstants.SAKAI_BATCHRESULTPROCESSOR).getString());
          if (searchBatchProcessor == null) {
            searchBatchProcessor = defaultSearchBatchProcessor;
          }
        }
        
        SearchResultProcessor searchProcessor = defaultSearchProcessor;
        if (node.hasProperty(SAKAI_RESULTPROCESSOR)) {
          searchProcessor = processors.get(node.getProperty(
              SAKAI_RESULTPROCESSOR).getString());
          if (searchProcessor == null) {
            searchProcessor = defaultSearchProcessor;
          }
        }

        // if we didnt get a total,
        if (total == -1) {
          total = Integer.MAX_VALUE;
        }
        long start = Math.min(offset, total);
        long end = Math.min(offset + nitems, total + 1);
        resultNodes.skip(start);
        if (searchBatchProcessor != defaultSearchBatchProcessor) {
          searchBatchProcessor.writeNodeIterator(write, resultNodes);
          LOGGER.info("Using batch processor for results");
        }else {
          for (long i = start; i < end && resultNodes.hasNext(); i++) {
            Node resultNode = resultNodes.nextNode();
            searchProcessor.writeNode(write, resultNode);
            LOGGER.info("Using regular processor for results");
          }
        }
        write.endArray();
        write.endObject();
      }
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getMessage());
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getMessage());
      LOGGER.info("Caught JSONException {}", e.getMessage());
      e.printStackTrace();
    }
  }

  private int intRequestParameter(SlingHttpServletRequest request,
      String paramName, int defaultVal) {
    RequestParameter param = request.getRequestParameter(paramName);
    if (param != null) {
      try {
        return Integer.parseInt(param.getString());
      } catch (NumberFormatException e) {
        LOGGER.warn(paramName + "parameter (" + param.getString()
            + ") is invalid defaulting to " + defaultVal + " items ", e);
      }
    }
    return defaultVal;
  }

  /**
   * Processes a template of the form select * from y where x = {q} so that
   * strings enclosed in { and } are replaced by the same property in the
   * request.
   * 
   * @param request
   *          the request.
   * @param queryTemplate
   *          the query template.
   * @param propertyProviderName
   * @return A processed query template.
   * @throws RepositoryException
   */
  protected String processQueryTemplate(SlingHttpServletRequest request,
      String queryTemplate, String queryLanguage, String propertyProviderName) {
    Map<String, String> propertiesMap = loadUserProperties(request,
        propertyProviderName);

    StringBuilder sb = new StringBuilder();
    boolean escape = false;
    int vstart = -1;
    char[] ca = queryTemplate.toCharArray();
    String defaultValue = null;
    for (int i = 0; i < ca.length; i++) {
      char c = ca[i];
      if (escape) {
        sb.append(c);
        escape = false;
      } else if (vstart >= 0) {
        if (c == '}') {
          String v = new String(ca, vstart + 1, i - vstart - 1);
          defaultValue = null;
          // Take care of default values
          if (v.contains("|")) {
            String[] val = v.split("\\|");
            v = val[0];
            defaultValue = val[1];
          }
          if (v.startsWith("_")) {
            String value = propertiesMap.get(v);
            if (value != null) {
              sb.append(escapeString(value, queryLanguage));
            } else if (value == null && defaultValue != null) {
              sb.append(escapeString(defaultValue, queryLanguage));
            }
          } else {

            RequestParameter rp = request.getRequestParameter(v);
            if (rp != null) {
              sb.append(escapeString(rp.getString(), queryLanguage));
            } else if (rp == null && defaultValue != null) {
              sb.append(escapeString(defaultValue, queryLanguage));
            }
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

  /**
   * @param request
   * @param propertyProviderName
   * @return
   * @throws RepositoryException
   */
  private Map<String, String> loadUserProperties(
      SlingHttpServletRequest request, String propertyProviderName) {
    Map<String, String> propertiesMap = new HashMap<String, String>();
    String userId = request.getRemoteUser();
    String userPrivatePath = "/jcr:root"
        + PersonalUtils.getPrivatePath(userId, "");
    propertiesMap.put("_userPrivatePath", ISO9075.encodePath(userPrivatePath));
    propertiesMap.put("_userId", userId);
    if (propertyProviderName != null) {
      LOGGER.info("Trying Provider Name {} ", propertyProviderName);
      SearchPropertyProvider provider = propertyProvider
          .get(propertyProviderName);
      if (provider != null) {
        LOGGER.info("Trying Provider {} ", provider);
        provider.loadUserProperties(request, propertiesMap);
      } else {
        LOGGER.warn("No properties provider found for {} ",
            propertyProviderName);
      }
    } else {
      LOGGER.info("No Provider ");
    }
    return propertiesMap;
  }

  private String escapeString(String value, String queryLanguage) {
    String escaped = null;
    if (value != null) {
      if (queryLanguage.equals(Query.XPATH) || queryLanguage.equals(Query.SQL)) {
        // See JSR-170 spec v1.0, Sec. 6.6.4.9 and 6.6.5.2
        escaped = value.replaceAll("\\\\(?![-\"])", "\\\\\\\\").replaceAll("'",
            "\\\\'").replaceAll("'", "''");
      } else {
        LOGGER.error("Unknown query language: " + queryLanguage);
      }
    }
    return escaped;
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
  
  protected void bindSearchBatchResultProcessor(ServiceReference serviceReference) {
    synchronized (delayedBatchReferences) {
      if (osgiComponentContext == null) {
        delayedBatchReferences.add(serviceReference);
      } else {
        addBatchProcessor(serviceReference);
      }
    }

  }

  protected void unbindSearchBatchResultProcessor(ServiceReference serviceReference) {
    synchronized (delayedBatchReferences) {
      if (osgiComponentContext == null) {
        delayedBatchReferences.remove(serviceReference);
      } else {
        removeBatchProcessor(serviceReference);
      }
    }

  }

  protected void bindSearchPropertyProvider(ServiceReference serviceReference) {
    synchronized (delayedReferences) {
      if (osgiComponentContext == null) {
        delayedPropertyReferences.add(serviceReference);
      } else {
        addProvider(serviceReference);
      }
    }

  }

  protected void unbindSearchPropertyProvider(ServiceReference serviceReference) {
    synchronized (delayedReferences) {
      if (osgiComponentContext == null) {
        delayedPropertyReferences.remove(serviceReference);
      } else {
        removeProvider(serviceReference);
      }
    }

  }

  /**
   * @param serviceReference
   */
  private void removeProcessor(ServiceReference serviceReference) {
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
    SearchResultProcessor processor = processorsById.remove(serviceId);
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
  
  
  /**
   * @param serviceReference
   */
  private void removeBatchProcessor(ServiceReference serviceReference) {
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
    SearchResultProcessor processor = processorsById.remove(serviceId);
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
  private void addBatchProcessor(ServiceReference serviceReference) {
    SearchBatchResultProcessor processor = (SearchBatchResultProcessor) osgiComponentContext
        .locateService(SEARCH_BATCH_RESULT_PROCESSOR, serviceReference);
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);

    batchProcessorsById.put(serviceId, processor);
    String[] processorNames = OsgiUtil.toStringArray(serviceReference
        .getProperty(REG_BATCH_PROCESSOR_NAMES));

    if(processorNames != null) {
      for (String processorName : processorNames) {
        batchProcessors.put(processorName, processor);
      }
    }
  }
  

  /**
   * @param serviceReference
   */
  private void removeProvider(ServiceReference serviceReference) {
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
    SearchPropertyProvider provider = propertyProviderById.remove(serviceId);
    if (provider != null) {
      List<String> toRemove = new ArrayList<String>();
      for (Entry<String, SearchPropertyProvider> e : propertyProvider
          .entrySet()) {
        if (provider.equals(e.getValue())) {
          toRemove.add(e.getKey());
        }
      }
      for (String r : toRemove) {
        propertyProvider.remove(r);
      }
    }
  }

  /**
   * @param serviceReference
   */
  private void addProvider(ServiceReference serviceReference) {
    SearchPropertyProvider provider = (SearchPropertyProvider) osgiComponentContext
        .locateService(SEARCH_PROPERTY_PROVIDER, serviceReference);
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);

    propertyProviderById.put(serviceId, provider);
    String[] processorNames = OsgiUtil.toStringArray(serviceReference
        .getProperty(REG_PROVIDER_NAMES));

    for (String processorName : processorNames) {
      propertyProvider.put(processorName, provider);
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
    synchronized (delayedBatchReferences) {
      osgiComponentContext = componentContext;
      for (ServiceReference ref : delayedBatchReferences) {
        addBatchProcessor(ref);
      }
      delayedBatchReferences.clear();
    }
    synchronized (delayedPropertyReferences) {
      osgiComponentContext = componentContext;
      for (ServiceReference ref : delayedPropertyReferences) {
        addProvider(ref);
      }
      delayedPropertyReferences.clear();
    }
  }

}
