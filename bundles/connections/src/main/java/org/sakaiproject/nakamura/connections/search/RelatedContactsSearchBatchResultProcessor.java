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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.connections.ConnectionConstants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.api.user.BasicUserInfo;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
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
@Component(immediate = true, metatype = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SolrSearchConstants.REG_BATCH_PROCESSOR_NAMES, value = "RelatedContactsSearchBatchResultProcessor") })
@Service(value = SolrSearchBatchResultProcessor.class)
public class RelatedContactsSearchBatchResultProcessor implements
    SolrSearchBatchResultProcessor {

  private static final Logger LOG = LoggerFactory
      .getLogger(RelatedContactsSearchBatchResultProcessor.class);

  @Reference
  private SolrSearchServiceFactory searchServiceFactory;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#writeResults(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter, java.util.Iterator)
   */
  public void writeResults(final SlingHttpServletRequest request,
      final JSONWriter writer, final Iterator<Result> iterator) throws JSONException {

    final Set<String> processedUsers = new HashSet<String>();
    try {
      while (iterator.hasNext()) {
        final Result result = iterator.next();
        final String resourceType = (String) result.getFirstValue("resourceType");
        if (ConnectionConstants.SAKAI_CONTACT_RT.equals(resourceType)) {
          renderConnection(request, writer, result, processedUsers);
        } else if ("authorizable".equals(resourceType)) {
          renderAuthorizable(request, writer, result, processedUsers);
        }
      }
    } catch (AccessDeniedException e) {
      // quietly swallow access denied
      LOG.debug(e.getLocalizedMessage(), e);
    } catch (StorageClientException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * @param request
   * @param writer
   * @param result
   * @param processedUsers
   * @throws AccessDeniedException
   * @throws JSONException
   * @throws StorageClientException
   */
  protected void renderConnection(SlingHttpServletRequest request, JSONWriter writer,
      Result result, final Set<String> processedUsers) throws AccessDeniedException,
      JSONException, StorageClientException {

    final String contactUser = result.getPath().substring(
        result.getPath().lastIndexOf("/") + 1);
    if (contactUser == null) {
      throw new IllegalArgumentException("Missing " + User.NAME_FIELD);
    }
    renderContact(contactUser, request, writer, processedUsers);
  }

  /**
   * @param request
   * @param writer
   * @param result
   * @param processedUsers
   * @throws AccessDeniedException
   * @throws JSONException
   * @throws StorageClientException
   */
  protected void renderAuthorizable(final SlingHttpServletRequest request,
      final JSONWriter writer, final Result result, final Set<String> processedUsers)
      throws AccessDeniedException, JSONException, StorageClientException {

    renderContact(result.getPath(), request, writer, processedUsers);
  }

  /**
   * @param user
   * @param request
   * @param writer
   * @throws AccessDeniedException
   * @throws JSONException
   * @throws StorageClientException
   */
  protected void renderContact(final String user, final SlingHttpServletRequest request,
      final JSONWriter writer, final Set<String> processedUsers)
      throws AccessDeniedException, JSONException, StorageClientException {

    if (user == null) {
      throw new IllegalArgumentException("Missing " + User.NAME_FIELD);
    }

    final Session session = StorageClientUtils.adaptToSession(request
        .getResourceResolver().adaptTo(javax.jcr.Session.class));
    if (!processedUsers.contains(user) && !session.getUserId().equals(user)) {
      final AuthorizableManager authMgr = session.getAuthorizableManager();
      final Authorizable auth = authMgr.findAuthorizable(user);

      if (auth != null) {
        writer.object();
        writer.key("target");
        writer.value(user);
        writer.key("profile");
        final BasicUserInfo basicUserInfo = new BasicUserInfo();
        ExtendedJSONWriter.writeValueMap(writer,
            new ValueMapDecorator(basicUserInfo.getProperties(auth)));
        writer.endObject();
        processedUsers.add(user);
      }
    }

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

}
