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

import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.DEFAULT_PAGED_ITEMS;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.JSON_RESULTS;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.PARAMS_ITEMS_PER_PAGE;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.PARAMS_PAGE;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.REG_BATCH_PROCESSOR_NAMES;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.REG_PROCESSOR_NAMES;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.REG_PROVIDER_NAMES;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.SAKAI_BATCHRESULTPROCESSOR;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.SAKAI_LIMIT_RESULTS;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.SAKAI_PROPERTY_PROVIDER;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.SAKAI_QUERY_TEMPLATE;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.SAKAI_QUERY_TEMPLATE_OPTIONS;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.SAKAI_RESULTPROCESSOR;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.SEARCH_BATCH_RESULT_PROCESSOR;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.SEARCH_PATH_PREFIX;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.SEARCH_PROPERTY_PROVIDER;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.SEARCH_RESULT_PROCESSOR;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.TIDY;
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.TOTAL;

import com.google.common.collect.Maps;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.search.solr.MissingParameterException;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchUtil;
import org.sakaiproject.nakamura.api.templates.TemplateService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SearchServlet</code> uses nodes from the
 *
 */
@SlingServlet(extensions = { "json" }, methods = { "GET" }, resourceTypes = { "sakai/solr-search", "sakai/sparse-search" })
@Properties(value = {
    @Property(name = "service.description", value = { "Perfoms searchs based on the associated node." }),
    @Property(name = "service.vendor", value = { "The Sakai Foundation" }),
    @Property(name = "maximumResults", longValue = 2500L) })
@References(value = {
    @Reference(name = "SearchResultProcessor", referenceInterface = SolrSearchResultProcessor.class, bind = "bindSearchResultProcessor", unbind = "unbindSearchResultProcessor", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "SearchBatchResultProcessor", referenceInterface = SolrSearchBatchResultProcessor.class, bind = "bindSearchBatchResultProcessor", unbind = "unbindSearchBatchResultProcessor", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "SearchPropertyProvider", referenceInterface = SolrSearchPropertyProvider.class, bind = "bindSearchPropertyProvider", unbind = "unbindSearchPropertyProvider", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC) })
public class SolrSearchServlet extends SlingSafeMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 4130126304725079596L;
  private static final Logger LOGGER = LoggerFactory.getLogger(SolrSearchServlet.class);

  private Map<String, SolrSearchBatchResultProcessor> batchProcessors = new ConcurrentHashMap<String, SolrSearchBatchResultProcessor>();
  private Map<Long, SolrSearchBatchResultProcessor> batchProcessorsById = new ConcurrentHashMap<Long, SolrSearchBatchResultProcessor>();

  private Map<String, SolrSearchResultProcessor> processors = new ConcurrentHashMap<String, SolrSearchResultProcessor>();
  private Map<Long, SolrSearchResultProcessor> processorsById = new ConcurrentHashMap<Long, SolrSearchResultProcessor>();

  private Map<String, SolrSearchPropertyProvider> propertyProvider = new ConcurrentHashMap<String, SolrSearchPropertyProvider>();
  private Map<Long, SolrSearchPropertyProvider> propertyProviderById = new ConcurrentHashMap<Long, SolrSearchPropertyProvider>();

  private transient ComponentContext osgiComponentContext;
  private List<ServiceReference> delayedReferences = new ArrayList<ServiceReference>();
  private List<ServiceReference> delayedPropertyReferences = new ArrayList<ServiceReference>();
  private List<ServiceReference> delayedBatchReferences = new ArrayList<ServiceReference>();

  protected long maximumResults = 100;

  // Default processors
  /**
   * Reference uses property set on NodeSearchResultProcessor. Other processors can become
   * the default by setting {@link SearchResultProcessor.DEFAULT_PROCESOR_PROP} to true.
   */
  private static final String DEFAULT_BATCH_SEARCH_PROC_TARGET = "(&("
      + SolrSearchBatchResultProcessor.DEFAULT_BATCH_PROCESSOR_PROP + "=true))";
  @Reference(target = DEFAULT_BATCH_SEARCH_PROC_TARGET)
  protected transient SolrSearchBatchResultProcessor defaultSearchBatchProcessor;

  /**
   * Reference uses property set on NodeSearchResultProcessor. Other processors can become
   * the default by setting {@link SearchResultProcessor.DEFAULT_PROCESOR_PROP} to true.
   */
  private static final String DEFAULT_SEARCH_PROC_TARGET = "(&("
      + SolrSearchResultProcessor.DEFAULT_PROCESSOR_PROP + "=true))";
  @Reference(target = DEFAULT_SEARCH_PROC_TARGET)
  protected transient SolrSearchResultProcessor defaultSearchProcessor;

  @Reference
  private transient TemplateService templateService;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Resource resource = request.getResource();
      if (!resource.getPath().startsWith(SEARCH_PATH_PREFIX)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
            "Search templates can only be executed if they are located under "
                + SEARCH_PATH_PREFIX);
        return;
      }

      Node node = resource.adaptTo(Node.class);
      if (node != null && node.hasProperty(SAKAI_QUERY_TEMPLATE)) {
        // TODO: we might want to use this ?
        @SuppressWarnings("unused")
        boolean limitResults = true;
        if (node.hasProperty(SAKAI_LIMIT_RESULTS)) {
          limitResults = node.getProperty(SAKAI_LIMIT_RESULTS).getBoolean();
        }

        // KERN-1147 Respond better when all parameters haven't been provided for a query
        Query query = null;
        try {
          query = processQuery(request, node);
        } catch (MissingParameterException e) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
          return;
        }

        long nitems = SolrSearchUtil.longRequestParameter(request, PARAMS_ITEMS_PER_PAGE,
            DEFAULT_PAGED_ITEMS);
        long page = SolrSearchUtil.longRequestParameter(request, PARAMS_PAGE, 0);

        // allow number of items to be specified in sakai:query-template-options
        if (query.getOptions().containsKey(PARAMS_ITEMS_PER_PAGE)) {
          nitems = Long.valueOf(query.getOptions().get(PARAMS_ITEMS_PER_PAGE));
        } else {
          // add this to the options so that all queries are constrained to a limited
          // number of returns per page.
          query.getOptions().put(PARAMS_ITEMS_PER_PAGE, Long.toString(nitems));
        }

        if (query.getOptions().containsKey(PARAMS_PAGE)) {
          page = Long.valueOf(query.getOptions().get(PARAMS_PAGE));
        } else {
          // add this to the options so that all queries are constrained to a limited
          // number of returns per page.
          query.getOptions().put(PARAMS_PAGE, Long.toString(page));
        }

        boolean useBatch = false;
        // Get the
        SolrSearchBatchResultProcessor searchBatchProcessor = defaultSearchBatchProcessor;
        if (node.hasProperty(SAKAI_BATCHRESULTPROCESSOR)) {
          searchBatchProcessor = batchProcessors.get(node.getProperty(
              SAKAI_BATCHRESULTPROCESSOR).getString());
          useBatch = true;
          if (searchBatchProcessor == null) {
            searchBatchProcessor = defaultSearchBatchProcessor;
          }
        }

        SolrSearchResultProcessor searchProcessor = defaultSearchProcessor;
        if (node.hasProperty(SAKAI_RESULTPROCESSOR)) {
          searchProcessor = processors.get(node.getProperty(SAKAI_RESULTPROCESSOR)
              .getString());
          if (searchProcessor == null) {
            searchProcessor = defaultSearchProcessor;
          }
        }

        SolrSearchResultSet rs = null;
        try {
          // Prepare the result set.
          // This allows a processor to do other queries and manipulate the results.
          if (useBatch) {
            rs = searchBatchProcessor.getSearchResultSet(request, query);
          } else {
            rs = searchProcessor.getSearchResultSet(request, query);
          }
        } catch (SolrSearchException e) {
          response.sendError(e.getCode(), e.getMessage());
          return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ExtendedJSONWriter write = new ExtendedJSONWriter(response.getWriter());
        write.setTidy(isTidy(request));

        write.object();
        write.key(PARAMS_ITEMS_PER_PAGE);
        write.value(nitems);
        write.key(JSON_RESULTS);

        write.array();

        Iterator<Result> iterator = rs.getResultSetIterator();
        if (useBatch) {
          LOGGER.info("Using batch processor for results");
          searchBatchProcessor.writeResults(request, write, iterator);
        } else {
          LOGGER.info("Using regular processor for results");
          // We don't skip any rows ourselves here.
          // We expect a rowIterator coming from a resultset to be at the right place.
          for (long i = 0; i < nitems && iterator.hasNext(); i++) {
            // Get the next row.
            Result result = iterator.next();

            // Write the result for this row.
            searchProcessor.writeResult(request, write, result);
          }
        }
        write.endArray();
        
        if (page > 0 || rs.getSize() == nitems) {
          // the result set may have been truncated by paging, so lets get a fuller count
          query.getOptions().put(PARAMS_ITEMS_PER_PAGE, Long.toString(maximumResults));
          query.getOptions().put(PARAMS_PAGE, Long.toString(0));
          try {
            if (useBatch) {
              rs = searchBatchProcessor.getSearchResultSet(request, query);
            } else {
              rs = searchProcessor.getSearchResultSet(request, query);
            }
          } catch (SolrSearchException e) {
            response.sendError(e.getCode(), e.getMessage());
            return;
          }
        }

        // write the total out after processing the list to give the underlying iterator
        // a chance to walk the results then report how many there were.
        write.key(TOTAL);
        write.value(rs.getSize());

        write.endObject();
      }
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (JSONException e) {
      LOGGER.error(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Processes a velocity template so that variable references are replaced by the same
   * properties in the property provider and request.
   *
   * @param request
   *          the request.
   * @param queryTemplate
   *          the query template.
   * @param propertyProviderName
   * @return A processed query template
   * @throws MissingParameterException
   */
  protected Query processQuery(SlingHttpServletRequest request, Node queryNode)
      throws RepositoryException, MissingParameterException, JSONException {
    // check the resource type and set the query type appropriately
    // default to using solr for queries
    javax.jcr.Property resourceType = queryNode.getProperty("sling:resourceType");
    String queryType = null;
    if ("sakai/sparse-search".equals(resourceType.getString())) {
      queryType = Query.SPARSE;
    } else {
      queryType = Query.SOLR;
    }

    String propertyProviderName = null;
    if (queryNode.hasProperty(SAKAI_PROPERTY_PROVIDER)) {
      propertyProviderName = queryNode.getProperty(SAKAI_PROPERTY_PROVIDER).getString();
    }
    Map<String, String> propertiesMap = loadProperties(request, propertyProviderName,
        queryNode.getProperties(), queryType);

    String queryTemplate = queryNode.getProperty(SAKAI_QUERY_TEMPLATE).getString();

    // process the query string before checking for missing terms to a) give processors a
    // chance to set things and b) catch any missing terms added by the processors.
    String queryString = templateService.evaluateTemplate(propertiesMap, queryTemplate);

    // expand home directory references to full path; eg. ~user => a:user
    queryString = SearchUtil.expandHomeDirectory(queryString);

    // check for any missing terms & process the query template
    Collection<String> missingTerms = templateService.missingTerms(queryString);
    if (!missingTerms.isEmpty()) {
      throw new MissingParameterException(
          "Your request is missing parameters for the template: "
              + StringUtils.join(missingTerms, ", "));
    }

    // collect query options
    JSONObject queryOptions = accumulateQueryOptions(queryNode);

    // process the options as templates and check for missing params
    Map<String, String> options = processOptions(propertiesMap, queryOptions, queryType);

    Query query = new Query(queryType, queryString, options);
    return query;
  }

  /**
   * @param propertiesMap
   * @param queryOptions
   * @return
   * @throws JSONException
   * @throws MissingParameterException
   */
  private Map<String, String> processOptions(Map<String, String> propertiesMap,
      JSONObject queryOptions, String queryType) throws JSONException, MissingParameterException {
    Collection<String> missingTerms;
    Map<String, String> options = Maps.newHashMap();
    if (queryOptions != null) {
      Iterator<String> keys = queryOptions.keys();
      while(keys.hasNext()) {
        String key = keys.next();
        String val = queryOptions.getString(key);
        missingTerms = templateService.missingTerms(propertiesMap, val);
        if (!missingTerms.isEmpty()) {
          throw new MissingParameterException(
              "Your request is missing parameters for the template: "
                  + StringUtils.join(missingTerms, ", "));
        }

        String processedVal = templateService.evaluateTemplate(propertiesMap, val);
        processedVal = SearchUtil.escapeString(processedVal, queryType);
        options.put(key, processedVal);
      }
    }
    return options;
  }

  /**
   * @param queryNode
   * @param queryOptions
   * @return
   * @throws RepositoryException
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws JSONException
   */
  private JSONObject accumulateQueryOptions(Node queryNode)
      throws RepositoryException, ValueFormatException, PathNotFoundException,
      JSONException {
    JSONObject queryOptions = null;
    if (queryNode.hasProperty(SAKAI_QUERY_TEMPLATE_OPTIONS)) {
      // process the options as JSON string
      String optionsProp = queryNode.getProperty(SAKAI_QUERY_TEMPLATE_OPTIONS).getString();
      queryOptions = new JSONObject(optionsProp);
    } else if (queryNode.hasNode(SAKAI_QUERY_TEMPLATE_OPTIONS)) {
      // process the options as a sub-node
      Node optionsNode = queryNode.getNode(SAKAI_QUERY_TEMPLATE_OPTIONS);
      if (optionsNode.hasProperties()) {
        queryOptions = new JSONObject();
        PropertyIterator props = optionsNode.getProperties();
        while (props.hasNext()) {
          javax.jcr.Property prop = props.nextProperty();
          String key = prop.getName();
          String val = prop.getString();
          if (!key.startsWith("jcr:")) {
            queryOptions.put(key, val);
          }
        }
      }
    }
    return queryOptions;
  }

  /**
   * Load properties from the query node, request and property provider.<br/>
   *
   * Overwrite order: query node &lt; request &lt; property provider<br/>
   *
   * This ordering allows the query node to set defaults, the request to override those
   * defaults but the property provider to have the final say in what value is set.
   * 
   * @param request
   * @param propertyProviderName
   * @return
   * @throws RepositoryException
   */
  private Map<String, String> loadProperties(SlingHttpServletRequest request,
      String propertyProviderName, PropertyIterator defaultProps, String queryType) throws RepositoryException {
    Map<String, String> propertiesMap = new HashMap<String, String>();

    // 0. load authorizable (user) information
    String userId = request.getRemoteUser();
    String userPrivatePath = ClientUtils.escapeQueryChars(LitePersonalUtils
        .getPrivatePath(userId));
    propertiesMap.put("_userPrivatePath", userPrivatePath);
    propertiesMap.put("_userId", ClientUtils.escapeQueryChars(userId));

    // 1. load in properties from the query template node so defaults can be set
    if (defaultProps != null) {
      while (defaultProps.hasNext()) {
        javax.jcr.Property prop = defaultProps.nextProperty();
        String key = prop.getName();
        if (!propertiesMap.containsKey(key) && !prop.isMultiple()) {
          String val = prop.getString();
          propertiesMap.put(key, val);
        }
      }
    }

    // 2. load in properties from the request
    RequestParameterMap params = request.getRequestParameterMap();
    for (Entry<String, RequestParameter[]> entry : params.entrySet()) {
      RequestParameter[] vals = entry.getValue();
      String requestValue = vals[0].getString();

      // blank values aren't cool
      if (StringUtils.isBlank(requestValue)) {
        continue;
      }

      // we're selective with what we escape to make sure we don't hinder
      // search functionality
      String key = entry.getKey();
      String val = SearchUtil.escapeString(requestValue, queryType);
      propertiesMap.put(key, val);
    }

    // 3. load properties from a property provider
    if (propertyProviderName != null) {
      LOGGER.debug("Trying Provider Name {} ", propertyProviderName);
      SolrSearchPropertyProvider provider = propertyProvider.get(propertyProviderName);
      if (provider != null) {
        LOGGER.debug("Trying Provider {} ", provider);
        provider.loadUserProperties(request, propertiesMap);
      } else {
        LOGGER.warn("No properties provider found for {} ", propertyProviderName);
      }
    } else {
      LOGGER.debug("No Provider ");
    }

    return propertiesMap;
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
    SolrSearchResultProcessor processor = processorsById.remove(serviceId);
    if (processor != null) {
      List<String> toRemove = new ArrayList<String>();
      for (Entry<String, SolrSearchResultProcessor> e : processors.entrySet()) {
        if (processor.equals(e.getValue())) {
          toRemove.add(e.getKey());
        }
      }
      for (String r : toRemove) {
        processors.remove(r);
      }

      // bit of a kludge until I can figure out why felix doesn't wire up the default
      // processor even though it finds a matching service.
      boolean defaultProcessor = getSetting(
          serviceReference.getProperty(SolrSearchResultProcessor.DEFAULT_PROCESSOR_PROP),
          false);
      if (defaultProcessor) {
        defaultSearchProcessor = null;
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T getSetting(Object o, T defaultValue) {
    if (o == null) {
      return defaultValue;
    }
    return (T) o;
  }

  private String[] getSetting(Object o, String[] defaultValue) {
    if (o == null) {
      return defaultValue;
    }
    if (o.getClass().isArray()) {
      return (String[]) o;
    }
    return new String[]{(String) o};
  }

  /**
   * @param serviceReference
   */
  private void addProcessor(ServiceReference serviceReference) {
    SolrSearchResultProcessor processor = (SolrSearchResultProcessor) osgiComponentContext
        .locateService(SEARCH_RESULT_PROCESSOR, serviceReference);
    if (processor == null) {
      LOGGER.warn("Retrieved null processor [{}]", serviceReference);
      return;
    }

    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);

    processorsById.put(serviceId, processor);
    String[] processorNames = getSetting(
        serviceReference.getProperty(REG_PROCESSOR_NAMES), new String[0]);

    for (String processorName : processorNames) {
      processors.put(processorName, processor);
    }

    // bit of a kludge until I can figure out why felix doesn't wire up the default
    // processor even though it finds a matching service.
    boolean defaultProcessor = getSetting(
        serviceReference.getProperty(SolrSearchResultProcessor.DEFAULT_PROCESSOR_PROP),
        false);
    if (defaultProcessor) {
      defaultSearchProcessor = processor;
    }
  }

  /**
   * @param serviceReference
   */
  private void removeBatchProcessor(ServiceReference serviceReference) {
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
    SolrSearchResultProcessor processor = processorsById.remove(serviceId);
    if (processor != null) {
      List<String> toRemove = new ArrayList<String>();
      for (Entry<String, SolrSearchResultProcessor> e : processors.entrySet()) {
        if (processor.equals(e.getValue())) {
          toRemove.add(e.getKey());
        }
      }
      for (String r : toRemove) {
        processors.remove(r);
      }

      // bit of a kludge until I can figure out why felix doesn't wire up the default
      // processor even though it finds a matching service.
      boolean defaultBatchProcessor = getSetting(serviceReference
          .getProperty(SolrSearchBatchResultProcessor.DEFAULT_BATCH_PROCESSOR_PROP),
          false);
      if (defaultBatchProcessor) {
        defaultSearchBatchProcessor = null;
      }
    }
  }

  /**
   * @param serviceReference
   */
  private void addBatchProcessor(ServiceReference serviceReference) {
    SolrSearchBatchResultProcessor processor = (SolrSearchBatchResultProcessor) osgiComponentContext
        .locateService(SEARCH_BATCH_RESULT_PROCESSOR, serviceReference);
    if (processor == null) {
      LOGGER.warn("Retrieved null processor [{}]", serviceReference);
      return;
    }
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);

    batchProcessorsById.put(serviceId, processor);
    String[] processorNames = getSetting(serviceReference
        .getProperty(REG_BATCH_PROCESSOR_NAMES), new String[0]);

    if (processorNames != null) {
      for (String processorName : processorNames) {
        batchProcessors.put(processorName, processor);
      }
    }

    // bit of a kludge until I can figure out why felix doesn't wire up the default
    // processor even though it finds a matching service.
    boolean defaultBatchProcessor = getSetting(serviceReference
        .getProperty(SolrSearchBatchResultProcessor.DEFAULT_BATCH_PROCESSOR_PROP), false);
    if (defaultBatchProcessor) {
      defaultSearchBatchProcessor = processor;
    }
  }

  /**
   * @param serviceReference
   */
  private void removeProvider(ServiceReference serviceReference) {
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
    SolrSearchPropertyProvider provider = propertyProviderById.remove(serviceId);
    if (provider != null) {
      List<String> toRemove = new ArrayList<String>();
      for (Entry<String, SolrSearchPropertyProvider> e : propertyProvider.entrySet()) {
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
    SolrSearchPropertyProvider provider = (SolrSearchPropertyProvider) osgiComponentContext
        .locateService(SEARCH_PROPERTY_PROVIDER, serviceReference);
    if (provider == null) {
      LOGGER.warn("Retrieved null provider [{}]", serviceReference);
      return;
    }
    Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);

    propertyProviderById.put(serviceId, provider);
    String[] processorNames = getSetting(serviceReference
        .getProperty(REG_PROVIDER_NAMES), new String[0]);

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

    maximumResults = (Long) componentContext.getProperties().get("maximumResults");
  }

  /**
   * True if our request wants the "tidy" pretty-printed format Copied from
   * org.apache.sling.servlets.get.impl.helpers.JsonRendererServlet
   */
  protected boolean isTidy(SlingHttpServletRequest req) {
    for (String selector : req.getRequestPathInfo().getSelectors()) {
      if (TIDY.equals(selector)) {
        return true;
      }
    }
    return false;
  }
}
