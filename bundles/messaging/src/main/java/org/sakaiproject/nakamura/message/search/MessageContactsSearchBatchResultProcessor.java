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
package org.sakaiproject.nakamura.message.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.framework.Constants;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchResponseDecorator;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.util.Iterator;

/**
 * Formats message node search results
 */
@Component(inherit = true)
@Service
@Properties(value = {
    @Property(name = Constants.SERVICE_VENDOR, value = "The Sakai Foundation"),
    @Property(name = SearchConstants.REG_BATCH_PROCESSOR_NAMES, value = "MessageContacts")
})
public class MessageContactsSearchBatchResultProcessor extends MessageSearchResultProcessor
    implements SolrSearchBatchResultProcessor, SearchResponseDecorator {

  @Reference
  protected ConnectionManager connMgr;

  /**
   * Parses the message to a usable JSON format for the UI.
   *
   * @param write
   * @param result
   * @throws JSONException
   */
  public void writeResults(SlingHttpServletRequest request, JSONWriter write,
      Iterator<Result> results) throws JSONException {
    // get a list of connections for the current user

    final ResourceResolver resolver = request.getResourceResolver();
    final Session session = StorageClientUtils.adaptToSession(resolver.adaptTo(javax.jcr.Session.class));

    try {
      final ContentManager cm = session.getContentManager();

      while (results.hasNext()) {
        Result result = results.next();
        // KERN-1573 no chat messages delivered
        // Content content = resolver.getResource(result.getPath()).adaptTo(Content.class);
        final Content content = cm.get(result.getPath());
        if (content != null) {
          write.object();
          writeContent(request, write, content);
          write.endObject();
        }
      }
    } catch (StorageClientException e) {
      throw new JSONException(e);
    } catch (AccessDeniedException e) {
      throw new JSONException(e);
    }
  }

  @Override
  protected void decorateProfile(ProfileType profileType, Session session,
      String otherUser, JSONWriter write) throws AccessDeniedException,
      StorageClientException, JSONException {
    ExtendedJSONWriter exWriter = (ExtendedJSONWriter) write;
    // add connection information
    connMgr.writeConnectionInfo(exWriter, session, session.getUserId(), otherUser);
  }
}
