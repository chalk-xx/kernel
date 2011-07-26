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
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.framework.Constants;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Search result processor to write out profile information when search returns home nodes
 * (sakai/user-home). This result processor should live in the user bundle but at the time
 * of this writing, moving to that bundle creates a cyclical dependency of:<br/>
 * search -&gt; personal -&gt; user -&gt; search
 *
 * TODO: Sort the above out and move this code to the correct bundle.
 */
@Component(inherit = true)
@Service(SolrSearchBatchResultProcessor.class)
@Properties({
  @Property(name = Constants.SERVICE_VENDOR, value = "The Sakai Foundation"),
  @Property(name = SearchConstants.REG_BATCH_PROCESSOR_NAMES, value = "ProfileContacts")
})
public class ProfileContactsSearchResultProcessor extends ProfileNodeSearchResultProcessor implements SolrSearchBatchResultProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileContactsSearchResultProcessor.class);

  @Reference
  private ConnectionManager connMgr;

  public ProfileContactsSearchResultProcessor() {
    super();
  }

  ProfileContactsSearchResultProcessor(SolrSearchServiceFactory searchServiceFactory,
      ProfileService profileService, PresenceService presenceService) {
    super(searchServiceFactory, profileService, presenceService);
  }

  public void writeResults(SlingHttpServletRequest request, JSONWriter write,
      Iterator<Result> results) throws JSONException {
    ExtendedJSONWriter exWriter = (ExtendedJSONWriter) write;
    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver()
        .adaptTo(javax.jcr.Session.class));

    String currUser = request.getRemoteUser();
    try {
      // write out the profile information for each result
      while (results.hasNext()) {
        Result result = results.next();
        // start the object here so we can decorate with contact details
        write.object();
        super.writeResult(request, write, result, true);

        // add contact information if appropriate
        String otherUser = String.valueOf(result.getFirstValue("path"));
        connMgr.writeConnectionInfo(exWriter, session, currUser, otherUser);

        write.endObject();
      }
    } catch (StorageClientException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }
}
