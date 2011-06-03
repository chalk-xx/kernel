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
package org.sakaiproject.nakamura.presence.search;

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
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.presence.PresenceUtils;
import org.sakaiproject.nakamura.api.profile.ProfileService;
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

import javax.jcr.RepositoryException;

/**
 *
 * @author ad602@caret.cam.ac.uk
 */
@Component(metatype = true)
@Service
@Properties({
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SolrSearchConstants.REG_PROCESSOR_NAMES, value = "GeneralFeed")
})
public class GeneralFeedSearchResultProcessor implements SolrSearchResultProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralFeedSearchResultProcessor.class);
    @Reference
    private SolrSearchServiceFactory searchServiceFactory;
    @Reference
    private ProfileService profileService;
    @Reference
    private PresenceService presenceService;

    public GeneralFeedSearchResultProcessor() {
    }

    public GeneralFeedSearchResultProcessor(SolrSearchServiceFactory searchServiceFactory) {
        if (searchServiceFactory == null) {
            throw new IllegalArgumentException("Search Service Factory must be set when not using as a component");
        }
        this.searchServiceFactory = searchServiceFactory;
    }

    public GeneralFeedSearchResultProcessor(SolrSearchServiceFactory searchServiceFactory, ProfileService profileService, PresenceService presenceService) {
        if (searchServiceFactory == null || profileService == null || presenceService == null) {
            throw new IllegalArgumentException("SearchServiceFactory, ProfileService and PresenceService must be set when not using as a component");
        }
        this.searchServiceFactory = searchServiceFactory;
        this.presenceService = presenceService;
        this.profileService = profileService;
    }

    public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query) throws SolrSearchException {
        return searchServiceFactory.getSearchResultSet(request, query);
    }

    public void writeResult(SlingHttpServletRequest request, JSONWriter write, Result result) throws JSONException {
        javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(javax.jcr.Session.class);
        Session session = StorageClientUtils.adaptToSession(jcrSession);
        try {

            AuthorizableManager authManager;
            authManager = session.getAuthorizableManager();
            String path = (String) result.getFirstValue("path");

            if ("authorizable".equals(result.getFirstValue("resourceType"))) {
                Authorizable auth = authManager.findAuthorizable(path);
                if (auth != null) {
                    write.object();
                    ValueMap map = profileService.getProfileMap(auth, jcrSession);
                    ExtendedJSONWriter.writeValueMapInternals(write, map);

                    // If this is a User Profile, then include Presence data.
                    if (!auth.isGroup()) {
                        PresenceUtils.makePresenceJSON(write, path, presenceService, true);
                    }
                    write.endObject();
                }
            } else {
                // process this as file
                // no need to wrap this with write.object(); and write.endObject(); writeFileNode will do this automatically.
                Content content = session.getContentManager().get(path);
                if (content == null) {
                  LOGGER.warn("Can't output null content [path={}]", path);
                } else {
                  FileUtils.writeFileNode(content, session, write);
                }
            }

        } catch (RepositoryException ex) {
          LOGGER.error(ex.getMessage(), ex);
        } catch (AccessDeniedException ex) {
          LOGGER.error(ex.getMessage(), ex);
        } catch (StorageClientException ex) {
          LOGGER.error(ex.getMessage(), ex);
        }
    }
}