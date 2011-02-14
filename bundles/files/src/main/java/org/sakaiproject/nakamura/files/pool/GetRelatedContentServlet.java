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
package org.sakaiproject.nakamura.files.pool;

import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_PUBLIC_RELATED_SELECTOR;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_RELATED_SELECTOR;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_RT;
import static org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_UUIDS;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Returns up to 10 items related to the current content node. There are two versions of
 * the feed: one which contains items available to to any logged-in user (with a
 * "sakai:permissions" property of "everyone"), and one which contains only publicly
 * accessible items (with a "sakai:permissions" property of "public").
 */
@ServiceDocumentation(name = "GetRelatedContentServlet", shortDescription = "Get up to ten related nodes", description = {
    "This servlet returns an array of content related to the targeted node.",
    "Currently, relatedness is determined by the number of shared tags." }, bindings = { @ServiceBinding(type = BindingType.TYPE, bindings = { POOLED_CONTENT_RT }, extensions = @ServiceExtension(name = "json", description = "This servlet outputs JSON data."), selectors = {
    @ServiceSelector(name = POOLED_CONTENT_RELATED_SELECTOR, description = "Will retrieve related content with an access scheme of 'everyone'."),
    @ServiceSelector(name = POOLED_CONTENT_PUBLIC_RELATED_SELECTOR, description = "Will retrieve related content with an access scheme of 'public'."),
    @ServiceSelector(name = "tidy", description = "Optional sub-selector. Will send back 'tidy' output.") }) }, methods = { @ServiceMethod(name = "GET", parameters = {}, description = { "This servlet only responds to GET requests." }, response = {
    @ServiceResponse(code = 200, description = "Succesful request, json can be found in the body"),
    @ServiceResponse(code = 500, description = "Failure to retrieve tags or files, an explanation can be found in the HTMl.") }) })
@SlingServlet(methods = { "GET" }, extensions = { "json" }, resourceTypes = { POOLED_CONTENT_RT }, selectors = {
    POOLED_CONTENT_RELATED_SELECTOR, POOLED_CONTENT_PUBLIC_RELATED_SELECTOR })
public class GetRelatedContentServlet extends SlingSafeMethodsServlet {
  private static final long serialVersionUID = -1262781431579462713L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(GetRelatedContentServlet.class);

  public static final int MAX_RESULTS = 10;

  @Reference
  protected SolrSearchServiceFactory solrSearchServiceFactory;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    LOGGER.info("GETTING RELATED CONTENT ================================================ ");
    // Query should look like:
    // //element(*,
    // sakai:pooled-content)[(@sakai:tag-uuid='6c589c99-4a08-4a51-8f09-2960b36bec6f'
    // or @sakai:tag-uuid='506edc80-ad50-4bb3-abe8-aa5c72e65888') and
    // (@sakai:permissions='public'
    // or @sakai:permissions='everyone')] order by @jcr:score descending
    StringBuilder sb = new StringBuilder("resourceType:sakai/pooled-content ");
    Set<String> selectors = ImmutableSet.of(request.getRequestPathInfo().getSelectors());

    boolean publicSearch = selectors.contains(POOLED_CONTENT_PUBLIC_RELATED_SELECTOR);

    // Collect tags to search against.
    Resource resource = request.getResource();
    Content content = resource.adaptTo(Content.class);
    ContentManager contentManager = resource.adaptTo(ContentManager.class);
    try {
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.setTidy(selectors.contains("tidy"));
      writer.array();

      if (content.hasProperty(SAKAI_TAG_UUIDS)) {
        String nodePath = content.getPath();
        Map<String, Object> properties = content.getProperties();
        Set<String> tagUuids = Sets.newHashSet();
        if (properties.containsKey(SAKAI_TAG_UUIDS)) {
          String[] uuids = (String[]) properties.get(SAKAI_TAG_UUIDS);
          for (String uuid : uuids) {
            tagUuids.add(ClientUtils.escapeQueryChars(uuid));
          }
        }

        if (tagUuids.size() > 0) {
          sb.append("(taguuid:").append(StringUtils.join(tagUuids, " OR "))
              .append(")");
        }
        Query query = new Query(sb.toString(), null);
        LOGGER.info("Submitting Query {} ", query);
        SolrSearchResultSet resultSet = solrSearchServiceFactory.getSearchResultSet(
            request, query, publicSearch);
        Iterator<Result> iterator = resultSet.getResultSetIterator();
        int count = 0;
        while ((count < MAX_RESULTS) && iterator.hasNext()) {
          Result result = iterator.next();
          String path = result.getPath();
          if (!nodePath.equals(path)) {
            try {
              Content contentResult = contentManager.get(path);
              writer.object();
              ExtendedJSONWriter.writeNodeContentsToWriter(writer, contentResult);
              writer.endObject();
              count++;
            } catch (StorageClientException e) {
              LOGGER.error(
                  "Error getting related content for " + request.getPathTranslated(), e);
            } catch (AccessDeniedException e) {
              LOGGER.info(
                  "Denied access for related content to  " + request.getPathTranslated(),
                  e);
            }
          }
        }
      } else {
        LOGGER.info("No UUID Tags in {} ",content.getProperties());
      }
      writer.endArray();
    } catch (JSONException e) {
      LOGGER.error("Error writing related content for " + request.getPathTranslated(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (SolrSearchException e) {
      LOGGER.error("Error writing related content for " + request.getPathTranslated(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

}
