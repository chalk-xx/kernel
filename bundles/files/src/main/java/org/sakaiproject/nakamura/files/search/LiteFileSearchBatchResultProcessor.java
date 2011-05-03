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

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.files.FileUtils;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * Formats the files search results.
 */
@Component(immediate = true, metatype = true)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SolrSearchConstants.REG_BATCH_PROCESSOR_NAMES, value = "LiteFiles") })
@Service(value = SolrSearchBatchResultProcessor.class)
public class LiteFileSearchBatchResultProcessor implements SolrSearchBatchResultProcessor {

  public static final Logger LOGGER = LoggerFactory
      .getLogger(LiteFileSearchBatchResultProcessor.class);

  @Reference
  private SolrSearchServiceFactory searchServiceFactory;

  @Reference
  private ProfileService profileService;

  public LiteFileSearchBatchResultProcessor(SolrSearchServiceFactory searchServiceFactory, ProfileService profileService) {
    this.searchServiceFactory = searchServiceFactory;
    this.profileService = profileService;
  }

  public LiteFileSearchBatchResultProcessor() {
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.sakaiproject.nakamura.api.search.solr.Query)
   */
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {

    return searchServiceFactory.getSearchResultSet(request, query);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#writeResults(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter, java.util.Iterator)
   */
  public void writeResults(SlingHttpServletRequest request, JSONWriter write,
      Iterator<Result> iterator) throws JSONException {

    writeResultsInternal(request, write, iterator);
  }

  /**
   * Same as writeResults logic, but counts number of results iterated over.
   * 
   * @param request
   * @param write
   * @param iterator
   * @return Set containing all unique paths processed.
   * @throws JSONException
   */
  public Set<String> writeResultsInternal(SlingHttpServletRequest request,
      JSONWriter write, Iterator<Result> iterator) throws JSONException {
    final Set<String> uniquePaths = new HashSet<String>();
    final Integer iDepth = (Integer) request.getAttribute("depth");
    int depth = 0;
    if (iDepth != null) {
      depth = iDepth.intValue();
    }
    try {
      javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(javax.jcr.Session.class);
      final Session session = StorageClientUtils.adaptToSession(jcrSession);
      while (iterator.hasNext()) {
        final Result result = iterator.next();
        uniquePaths.add(result.getPath());
        try {
          if ("authorizable".equals(result.getFirstValue("resourceType"))) {
            AuthorizableManager authManager = session.getAuthorizableManager();
            Authorizable auth = authManager.findAuthorizable((String) result.getFirstValue("id"));
            if (auth != null) {
              write.object();
              ValueMap map = profileService.getProfileMap(auth, jcrSession);
              ExtendedJSONWriter.writeValueMapInternals(write, map);
              write.endObject();
            }
          } else {
            String contentPath = result.getPath();
            final Content content = session.getContentManager().get(contentPath);
            handleContent(content, session, write, depth);
          }
        } catch (AccessDeniedException e) {
          // do nothing
        } catch (RepositoryException e) {
          throw new JSONException(e);
        }
      }
    } catch (StorageClientException e) {
      throw new JSONException(e);
    }
    return uniquePaths;
  }

  /**
   * Give a JSON representation of the content.
   * 
   * @param content
   * @param session
   * @param write
   *          The {@link JSONWriter} to output to.
   * @param depth
   * @throws JSONException
   * @throws StorageClientException
   */
  protected void handleContent(final Content content, final Session session,
      final JSONWriter write, final int depth) throws JSONException,
      StorageClientException {

    final String type = (String) content.getProperty(SLING_RESOURCE_TYPE_PROPERTY);
    if (FilesConstants.RT_SAKAI_LINK.equals(type)) {
      FileUtils.writeLinkNode(content, session, write);
    } else {
      FileUtils.writeFileNode(content, session, write, depth);
    }
  }

}
