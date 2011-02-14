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
package org.sakaiproject.nakamura.calendar.search;

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
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@Component
@Service
@Properties({ @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SolrSearchConstants.REG_PROCESSOR_NAMES, value = "Calendar") })
public class CalendarSearchResultProcessor implements SolrSearchResultProcessor {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(CalendarSearchResultProcessor.class);

  @Reference
  private SolrSearchServiceFactory searchServiceFactory;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      java.lang.String)
   */
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {
    return searchServiceFactory.getSearchResultSet(request, query);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchResultProcessor#writeResult(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter,
   *      org.sakaiproject.nakamura.api.search.solr.Result)
   */
  public void writeResult(SlingHttpServletRequest request, JSONWriter write, Result result)
      throws JSONException {
    String name = (String) result.getFirstValue(Authorizable.NAME_FIELD);
    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver()
        .adaptTo(javax.jcr.Session.class));

    try {
      AuthorizableManager authMgr = session.getAuthorizableManager();
      Authorizable auth = authMgr.findAuthorizable(name);
      ExtendedJSONWriter.writeValueMap(write, auth.getSafeProperties());
    } catch (StorageClientException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

}
