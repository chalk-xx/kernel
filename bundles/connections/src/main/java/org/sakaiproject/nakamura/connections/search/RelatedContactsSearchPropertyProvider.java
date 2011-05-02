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
package org.sakaiproject.nakamura.connections.search;

import static org.sakaiproject.nakamura.api.connections.ConnectionConstants.SEARCH_PROP_CONNECTIONSTORE;

import com.google.common.base.Join;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.connections.ConnectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * KERN-1798
 * Create a feed that lists people related to My Contacts. The criteria that 
 * should be used for this are: 
 * 
 * - Contacts from my contacts 
 * - People with similar tags, directory locations or descriptions 
 * - People that have commented on content I have commented on 
 * - People that are a member of groups I'm a member of 
 * 
 * The feed should not include people that are already contacts of mine. 
 * 
 * When less than 11 items are found for these criteria, the feed should be 
 * filled up with random people. However, preference should be given to people 
 * that have a profile picture, and a high number of contacts, memberships and 
 * content items.
 * </pre>
 */
@Component(label = "ConnectionSearchPropertyProvider", description = "Provides properties to handle connection searches.")
@Service
@Properties({
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value = "RelatedContactsSearchPropertyProvider") })
public class RelatedContactsSearchPropertyProvider implements SolrSearchPropertyProvider {

  private static final Logger LOG = LoggerFactory
      .getLogger(RelatedContactsSearchPropertyProvider.class);

  private static final String DEFAULT_SEARCH_PROC_TARGET = "(&("
      + SolrSearchResultProcessor.DEFAULT_PROCESSOR_PROP + "=true))";
  @Reference(target = DEFAULT_SEARCH_PROC_TARGET)
  private transient SolrSearchResultProcessor defaultSearchProcessor;

  @Reference
  protected ConnectionManager connectionManager;

  /**
   * The solr query options that will be used in phase one where we find source content to
   * match against.
   */
  public static final Map<String, String> SOURCE_QUERY_OPTIONS;
  static {
    SOURCE_QUERY_OPTIONS = new HashMap<String, String>(3);
    // sort by most recent content
    SOURCE_QUERY_OPTIONS.put("sort", "_lastModified desc");
    // limit source content for matching to something reasonable
    SOURCE_QUERY_OPTIONS.put("items", "25");
    SOURCE_QUERY_OPTIONS.put("page", "0");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {

    /* find source contacts to match against */
    try {
      final String me = request.getRemoteUser();
      final Set<String> relatedConnectionPaths = new HashSet<String>();

      final Session session = StorageClientUtils.adaptToSession(request
          .getResourceResolver().adaptTo(javax.jcr.Session.class));
      final AuthorizableManager authorizableManager = session.getAuthorizableManager();
      final Set<String> allTagUuids = new HashSet<String>();
      final String[] myTagUuids = (String[]) authorizableManager.findAuthorizable(me)
          .getProperty("sakai:tag-uuid");
      if (myTagUuids != null) {
        allTagUuids.addAll(Arrays.asList(myTagUuids));
      }
      final List<String> myConnections = connectionManager.getConnectedUsers(request, me,
          ConnectionState.ACCEPTED);
      if (myConnections != null) {
        for (final String myConnection : myConnections) {
          String connectionPath = ClientUtils.escapeQueryChars(ConnectionUtils
              .getConnectionPathBase(myConnection));
          if (connectionPath.startsWith("/")) {
            connectionPath = connectionPath.substring(1);
          }
          relatedConnectionPaths.add(connectionPath);
        }
      }

      final String connectionPath = Join.join(" OR ", relatedConnectionPaths);
      propertiesMap.put(SEARCH_PROP_CONNECTIONSTORE, connectionPath);

      if (allTagUuids.isEmpty()) { // to prevent solr parse errors
        allTagUuids.add(String.valueOf(false));
      }
      propertiesMap.put("tagUuids", Join.join(" OR ", allTagUuids));
    } catch (AccessDeniedException e) {
      LOG.error(e.getLocalizedMessage(), e);
    } catch (StorageClientException e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

}
