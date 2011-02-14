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
package org.sakaiproject.nakamura.search.processors;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import javax.jcr.RepositoryException;


@Component(label = "GroupJoinRequestSearchResultProcessor", description = "Formatter for group join request search results.")
@Properties({
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.processor", value = "GroupJoinRequest") })
@Service
public class GroupJoinRequestSearchResultProcessor implements SolrSearchResultProcessor {

  @Reference
  private ProfileService profileService;


  @Reference
  private SolrSearchServiceFactory searchServiceFactory;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {
    // return the result set
    return searchServiceFactory.getSearchResultSet(request, query);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#writeNode(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter,
   *      org.sakaiproject.nakamura.api.search.Aggregator, javax.jcr.query.Row)
   */
  public void writeResult(SlingHttpServletRequest request, JSONWriter write, Result result)
      throws JSONException {
    javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(javax.jcr.Session.class);
    Session session = StorageClientUtils.adaptToSession(jcrSession);
    String path = result.getPath();
    String userId = (String) result.getFirstValue(User.NAME_FIELD);
    if (userId != null) {
      try {
        AuthorizableManager authMgr = session.getAuthorizableManager();
        Authorizable auth = authMgr.findAuthorizable(path);

        write.object();
        ValueMap map = profileService.getCompactProfileMap(auth, jcrSession);
        ((ExtendedJSONWriter)write).valueMapInternals(map);
        write.endObject();
      } catch (StorageClientException e) {
        throw new RuntimeException(e.getMessage(), e);
      } catch (AccessDeniedException e) {
      } catch (RepositoryException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }

}
