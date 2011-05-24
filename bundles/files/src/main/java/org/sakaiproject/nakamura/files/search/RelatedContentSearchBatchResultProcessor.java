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
package org.sakaiproject.nakamura.files.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Create a feed that lists content related to the content in My Library. The criteria
 * that should be used for this are:
 * <p>
 * - Other content with similar words in the title</br> - Other content from my contact's
 * library</br> - Other content with similar tags</br> - Other content with similar
 * directory locations
 * </p>
 * 
 * When less than 11 items are found for these criteria, the feed should be filled up with
 * random content. However, preference should be given to items that have a thumbnail
 * (page1-small.jpg), a description, tags and comments.
 */
@Component(immediate = true, metatype = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SolrSearchConstants.REG_BATCH_PROCESSOR_NAMES, value = "RelatedContentSearchBatchResultProcessor") })
@Service(value = SolrSearchBatchResultProcessor.class)
public class RelatedContentSearchBatchResultProcessor extends
    LiteFileSearchBatchResultProcessor {

  private static final Logger LOG = LoggerFactory
      .getLogger(RelatedContentSearchBatchResultProcessor.class);

  private static final String DEFAULT_SEARCH_PROC_TARGET = "(&("
      + SolrSearchResultProcessor.DEFAULT_PROCESSOR_PROP + "=true))";
  @Reference(target = DEFAULT_SEARCH_PROC_TARGET)
  private transient SolrSearchResultProcessor defaultSearchProcessor;

  /**
   * "These go to eleven"
   */
  public static final int VOLUME = 11;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.files.search.LiteFileSearchBatchResultProcessor#writeResults(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter, java.util.Iterator)
   */
  @Override
  public void writeResults(SlingHttpServletRequest request, JSONWriter write,
      Iterator<Result> iterator) throws JSONException {

    final Set<String> uniquePathsProcessed = super.writeResultsInternal(request, write,
        iterator);
    final int resultsCount = uniquePathsProcessed.size();
    if (resultsCount < VOLUME) {
      /* we need to grab some random content to reach minimum of 11 results. */
      final Session session = StorageClientUtils.adaptToSession(request
          .getResourceResolver().adaptTo(javax.jcr.Session.class));
      final String user = MeManagerViewerSearchPropertyProvider.getUser(request);

      // query to find ALL content that is not mine, boost some fields that have values
      final StringBuilder sourceQuery = new StringBuilder(
          "(resourceType:sakai/pooled-content AND (manager:((everyone OR anonymous) AND NOT ");
      sourceQuery.append(user);
      sourceQuery.append(") OR viewer:((everyone OR anonymous) AND NOT ");
      sourceQuery.append(user);
      sourceQuery
          .append("))) OR (resourceType:sakai/pooled-content AND (manager:((everyone OR anonymous) AND NOT ");
      sourceQuery.append(user);
      sourceQuery.append(") OR viewer:((everyone OR anonymous) AND NOT ");
      sourceQuery.append(user);
      // FYI: ^4 == 4 times boost; default boost value is one
      sourceQuery.append(")) AND (description:[* TO *] OR taguuid:[* TO *]))^4");


      try {
        final Iterator<Result> i = SolrSearchUtil.getRandomResults(request,
                                                                   defaultSearchProcessor,
                                                                   sourceQuery.toString(),
                                                                   "items", String.valueOf(VOLUME),
                                                                   "page", "0",
                                                                   "sort", "score desc");


        if (i != null) {

          final ContentManager contentManager = session.getContentManager();
          while (i.hasNext() && uniquePathsProcessed.size() <= VOLUME) {
            final Result result = i.next();
            final String path = (String) result.getFirstValue("path");
            if (uniquePathsProcessed.contains(path)) {
              // we have already painted this result
              continue;
            }
            final Content content = contentManager.get(path);
            if (content != null) {
              super.handleContent(content, session, write, 0);
              uniquePathsProcessed.add(path);
            } else {
              // fail quietly in this edge case
              LOG.debug("Content not found: {}", path);
            }
          }
          if (uniquePathsProcessed.size() < VOLUME) {
            LOG.debug("Did not meet functional specification. There should be at least {} results; actual size was: {}",
                      VOLUME, uniquePathsProcessed.size());
          }
        }

      } catch (AccessDeniedException e) {
        // quietly swallow access denied
        LOG.debug(e.getLocalizedMessage(), e);

      } catch (SolrSearchException e) {
        LOGGER.error(e.getMessage(), e);
        throw new IllegalStateException(e);

      } catch (StorageClientException e) {
        LOG.error(e.getLocalizedMessage(), e);
        throw new IllegalStateException(e);
      }
    }
  }
}
