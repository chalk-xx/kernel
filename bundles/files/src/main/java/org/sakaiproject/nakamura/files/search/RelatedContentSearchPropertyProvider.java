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

import com.google.common.base.Join;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
@Component(label = "RelatedContentSearchPropertyProvider", description = "Property provider for related content searches")
@Service
@Properties({
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value = "RelatedContentSearchPropertyProvider") })
public class RelatedContentSearchPropertyProvider extends
    MeManagerViewerSearchPropertyProvider implements SolrSearchPropertyProvider {

  private static final Logger LOG = LoggerFactory
      .getLogger(RelatedContentSearchPropertyProvider.class);

  private static final String DEFAULT_SEARCH_PROC_TARGET = "(&("
      + SolrSearchResultProcessor.DEFAULT_PROCESSOR_PROP + "=true))";
  @Reference(target = DEFAULT_SEARCH_PROC_TARGET)
  private transient SolrSearchResultProcessor defaultSearchProcessor;

  private static final int MAX_SOURCE_LIMIT = 100;

  /**
   * The solr query options that will be used in phase one where we find source content to
   * match against.
   */
  public static final Map<String, String> SOURCE_QUERY_OPTIONS;
  static {
    final Map<String, String> sqo = new HashMap<String, String>(3);
    // sort by most recent content
    sqo.put("sort", "_lastModified desc");
    // limit source content for matching to something reasonable
    sqo.put("items", String.valueOf(MAX_SOURCE_LIMIT));
    sqo.put("page", "0");
    SOURCE_QUERY_OPTIONS = Collections.unmodifiableMap(sqo);
  }

  /**
   * Pattern to match any character that *might* be used to separate words. We only
   * compile the pattern *once* for performance.
   */
  private static final Pattern REGEX_PATTERN = Pattern
      .compile("(\\s|[\\Q`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?\\E])+");

  /**
   * A goofy string that we will use in edges cases where the search template requires we
   * supply a non-null, non-empty set of values. Very unlikely anything will ever match
   * this string.
   */
  private static final String AVOID_FALSE_POSITIVE_MATCHES = ClientUtils
      .escapeQueryChars(REGEX_PATTERN.pattern());

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.files.search.MeManagerViewerSearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(final SlingHttpServletRequest request,
      final Map<String, String> propertiesMap) {

    /* phase one - find source content to match against */

    final String user = super.getUser(request);
    if (User.ANON_USER.equals(user)) {
      // stop here, anonymous is not a manager or a viewer of anything
      return;
    }

    final Session session = StorageClientUtils.adaptToSession(request
        .getResourceResolver().adaptTo(javax.jcr.Session.class));
    final Set<String> managers = super.getPrincipals(session, user);
    final Set<String> viewers = new HashSet<String>(managers);

    final StringBuilder sourceQuery = new StringBuilder(
        "resourceType:sakai/pooled-content AND (manager:(");
    sourceQuery.append(Join.join(" OR ", managers));
    sourceQuery.append(") OR viewer:(");
    sourceQuery.append(Join.join(" OR ", viewers));
    sourceQuery.append("))");
    final Query query = new Query(Query.SOLR, sourceQuery.toString(),
        SOURCE_QUERY_OPTIONS);

    SolrSearchResultSet rs = null;
    try {
      rs = defaultSearchProcessor.getSearchResultSet(request, query);
    } catch (SolrSearchException e) {
      LOG.error(e.getLocalizedMessage(), e);
      throw new IllegalStateException(e);
    }
    if (rs != null) {
      try {
        final ContentManager contentManager = session.getContentManager();
        final Iterator<Result> i = rs.getResultSetIterator();
        Set<String> allFileNames = new HashSet<String>();
        Set<String> allTagUuids = new HashSet<String>();
        int count = 0;
        while (i.hasNext() && count < MAX_SOURCE_LIMIT) {
          final Result result = i.next();
          final String path = (String) result.getFirstValue("path");
          final Content content = contentManager.get(path);
          if (content != null) {
            // grab the unique file name tokens
            String fileName = (String) content
                .getProperty(FilesConstants.POOLED_CONTENT_FILENAME);
            if (fileName != null) {
              final String fileExtension = (String) content
                  .getProperty("sakai:fileextension");
              if (fileExtension != null) {
                final int extensionIndex = fileName.lastIndexOf(fileExtension);
                if (extensionIndex > 0) {
                  fileName = fileName.substring(0, extensionIndex);
                }
              }
              final String[] foundFileNames = REGEX_PATTERN.split(fileName);
              allFileNames.addAll(Arrays.asList(foundFileNames));
            }

            // grab all the unique tag uuids
            final String[] tagUuids = (String[]) content
                .getProperty(FilesConstants.SAKAI_TAG_UUIDS);
            if (tagUuids != null) {
              allTagUuids.addAll(Arrays.asList(tagUuids));
            }
          } else {
            // fail quietly in this edge case
            LOG.debug("Content not found: {}", path);
          }
          count++;
        }

        /* phase two - provide properties for final search */

        final List<String> connections = connectionManager.getConnectedUsers(request,
            user, ConnectionState.ACCEPTED);
        if (connections != null) {
          for (final String connection : connections) {
            managers.add(ClientUtils.escapeQueryChars(connection));
          }
        }
        managers.remove(user); // do not display my own content
        viewers.remove(user); // do not display my own content
        if (managers.isEmpty()) { // to prevent solr parse errors
          managers.add(AVOID_FALSE_POSITIVE_MATCHES);
        }
        propertiesMap.put("managers", Join.join(" OR ", managers));
        if (viewers.isEmpty()) { // to prevent solr parse errors
          viewers.add(AVOID_FALSE_POSITIVE_MATCHES);
        }
        propertiesMap.put("viewers", Join.join(" OR ", viewers));

        if (allFileNames.isEmpty()) { // to prevent solr parse errors
          allFileNames.add(AVOID_FALSE_POSITIVE_MATCHES);
        }
        if (allFileNames.size() > 1024) {
          /*
           * solr allows a maximum of 1024. Performance will likely be an issue by this
           * point.
           */
          LOG.warn(
              "Exceeded maximum number of solr binary operations: {}. Reduced size to 1024.",
              allFileNames.size());
          final String[] tooLarge = (String[]) allFileNames.toArray();
          final String[] justRight = Arrays.copyOf(tooLarge, 1024);
          allFileNames = new HashSet<String>(Arrays.asList(justRight));
        }
        propertiesMap.put("fileNames", Join.join(" OR ", allFileNames));

        if (allTagUuids.isEmpty()) { // to prevent solr parse errors
          allTagUuids.add(AVOID_FALSE_POSITIVE_MATCHES);
        }
        if (allTagUuids.size() > 1024) {
          /*
           * solr allows a maximum of 1024. Performance will likely be an issue by this
           * point.
           */
          LOG.warn(
              "Exceeded maximum number of solr binary operations: {}. Reduced size to 1024.",
              allTagUuids.size());
          final String[] tooLarge = (String[]) allTagUuids.toArray();
          final String[] justRight = Arrays.copyOf(tooLarge, 1024);
          allTagUuids = new HashSet<String>(Arrays.asList(justRight));
        }
        propertiesMap.put("tagUuids", Join.join(" OR ", allTagUuids));

      } catch (AccessDeniedException e) {
        LOG.error(e.getLocalizedMessage(), e);
      } catch (StorageClientException e) {
        LOG.error(e.getLocalizedMessage(), e);
      }
    }
  }

}
