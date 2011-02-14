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
import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.PARAMS_ITEMS_PER_PAGE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchUtil;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

@Component(immediate = true, metatype=true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SolrSearchConstants.REG_BATCH_PROCESSOR_NAMES, value = "FullResource")
})
@Service(value = SolrSearchBatchResultProcessor.class)
public class DefaultResourceSearchBatchResultProcessor implements
    SolrSearchBatchResultProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultResourceSearchBatchResultProcessor.class);
  @Reference
  protected SolrSearchServiceFactory searchServiceFactory;

  /**
   * The non component constructor
   * @param searchServiceFactory
   */
  DefaultResourceSearchBatchResultProcessor(SolrSearchServiceFactory searchServiceFactory) {
    if ( searchServiceFactory == null ) {
      throw new IllegalArgumentException("Search Service Factory must be set when not using as a component");
    }
    this.searchServiceFactory = searchServiceFactory;
  }


  /**
   * Component Constructor.
   */
  public DefaultResourceSearchBatchResultProcessor() {
  }



  public void writeResults(SlingHttpServletRequest request, JSONWriter write,
      Iterator<Result> iterator) throws JSONException {
    ResourceResolver resolver = request.getResourceResolver();
    int maxTraversalDepth = SolrSearchUtil.getTraversalDepth(request);


    long nitems = SolrSearchUtil.longRequestParameter(request,
        PARAMS_ITEMS_PER_PAGE, DEFAULT_PAGED_ITEMS);



    int maxDepth = SolrSearchUtil.getTraversalDepth(request);
    for (long i = 0; i < nitems && iterator.hasNext(); i++) {
      Result result = iterator.next();
      write.object();
      write.key("searchdoc");
      ExtendedJSONWriter.writeValueMap(write,result.getProperties());
      String path = result.getPath();
      Resource resource = resolver.getResource(path);
      if (resource != null) {
        Content content = resource.adaptTo(Content.class);
        Node node = resource.adaptTo(Node.class);
        if (content != null) {
          write.key("content");
          write.object();
          ExtendedJSONWriter.writeNodeContentsToWriter(write, content);
          write.endObject();
        } else if (node != null) {
          try {
            write.key("node");
            write.object();
            ExtendedJSONWriter.writeNodeContentsToWriter(write, node);
            write.endObject();
          } catch (RepositoryException e) {
            LOGGER.warn(e.getMessage(), e);
          }
        }
      } else {
        try {
          Session session = StorageClientUtils.adaptToSession(resolver.adaptTo(javax.jcr.Session.class));
          ContentManager contentManager= session.getContentManager();
          Content content = contentManager.get(path);
          if (content != null) {
            write.key("content");
            write.object();
            ExtendedJSONWriter.writeNodeContentsToWriter(write, content);
            write.endObject();
          }
        } catch ( Exception e ) {
          LOGGER.warn(e.getMessage(), e);
        }
      }
      write.endObject();
    }
  }


  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query) throws SolrSearchException {
    return searchServiceFactory.getSearchResultSet(request, query);  }

}
