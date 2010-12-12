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

import static org.sakaiproject.nakamura.api.files.FilesConstants.ACCESS_SCHEME_PROPERTY;
import static org.sakaiproject.nakamura.api.files.FilesConstants.LOGGED_IN_ACCESS_SCHEME;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_PUBLIC_RELATED_SELECTOR;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_RELATED_SELECTOR;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_RT;
import static org.sakaiproject.nakamura.api.files.FilesConstants.PUBLIC_ACCESS_SCHEME;
import static org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_UUIDS;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Returns up to 10 items related to the current content node. There are two
 * versions of the feed: one which contains items available to to any logged-in
 * user (with a "sakai:permissions" property of "everyone"), and one which contains
 * only publicly accessible items (with a "sakai:permissions" property of "public").
 */
@ServiceDocumentation(name = "GetRelatedContentServlet", shortDescription = "Get up to ten related nodes",
    description = {
      "This servlet returns an array of content related to the targeted node.",
      "Currently, relatedness is determined by the number of shared tags."
    },
    bindings = {
      @ServiceBinding(type = BindingType.TYPE, bindings = { POOLED_CONTENT_RT },
          extensions = @ServiceExtension(name = "json", description = "This servlet outputs JSON data."),
          selectors = {
            @ServiceSelector(name = POOLED_CONTENT_RELATED_SELECTOR, description = "Will retrieve related content with an access scheme of 'everyone'."),
            @ServiceSelector(name = POOLED_CONTENT_PUBLIC_RELATED_SELECTOR, description = "Will retrieve related content with an access scheme of 'public'."),
            @ServiceSelector(name = "tidy", description = "Optional sub-selector. Will send back 'tidy' output.")
          }
      )
    },
    methods = {
      @ServiceMethod(name = "GET",  parameters = {},
          description = { "This servlet only responds to GET requests." },
          response = {
            @ServiceResponse(code = 200, description = "Succesful request, json can be found in the body"),
            @ServiceResponse(code = 500, description = "Failure to retrieve tags or files, an explanation can be found in the HTMl.")
          }
      )
    }
)
@SlingServlet(methods = { "GET" },
    extensions = { "json" },
    resourceTypes = { POOLED_CONTENT_RT },
    selectors = { POOLED_CONTENT_RELATED_SELECTOR, POOLED_CONTENT_PUBLIC_RELATED_SELECTOR }
)
public class GetRelatedContentServlet extends SlingSafeMethodsServlet {
  private static final long serialVersionUID = -1262781431579462713L;
  private static final Logger LOGGER = LoggerFactory.getLogger(GetRelatedContentServlet.class);

  public static final int MAX_RESULTS = 10;

  // Set up the access-scheme-based queries.
  private static final String LOGGED_IN_QUERY =
    "(@" + ACCESS_SCHEME_PROPERTY + "='" + LOGGED_IN_ACCESS_SCHEME + "' or @" +
    ACCESS_SCHEME_PROPERTY + "='" + PUBLIC_ACCESS_SCHEME + "')";
  private static final String PUBLIC_QUERY = "@" + ACCESS_SCHEME_PROPERTY + "='" + PUBLIC_ACCESS_SCHEME + "'";

  @Reference
  protected SearchServiceFactory searchServiceFactory;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // Query should look like:
    //  //element(*, sakai:pooled-content)[(@sakai:tag-uuid='6c589c99-4a08-4a51-8f09-2960b36bec6f'
    //    or @sakai:tag-uuid='506edc80-ad50-4bb3-abe8-aa5c72e65888') and (@sakai:permissions='public'
    //    or @sakai:permissions='everyone')] order by @jcr:score descending
    StringBuilder sb = new StringBuilder("//element(*, sakai:pooled-content)[");
    List<String> selectors = Arrays.asList(request.getRequestPathInfo().getSelectors());
    if (selectors.contains(POOLED_CONTENT_PUBLIC_RELATED_SELECTOR)) {
      sb.append(PUBLIC_QUERY);
    } else if (selectors.contains(POOLED_CONTENT_RELATED_SELECTOR)) {
      sb.append(LOGGED_IN_QUERY);
    } else {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No valid selector specified");
      return;
    }

    // Collect tags to search against.
    Node node = request.getResource().adaptTo(Node.class);
    try {
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.setTidy(selectors.contains("tidy"));
      writer.array();
      if (node.hasProperty(SAKAI_TAG_UUIDS)) {
        String nodePath = node.getPath();
        // Each node that has been tagged has one or more tag UUIDs riding with it
        Value[] uuidValues = JcrUtils.getValues(node, SAKAI_TAG_UUIDS);
        if (uuidValues.length > 0) {
          Set<String> tagUuids = new HashSet<String>(uuidValues.length);
          for (Value uuidValue : uuidValues) {
            tagUuids.add(uuidValue.getString());
          }
          sb.append("and (@sakai:tag-uuid='").
              append(StringUtils.join(tagUuids, "' or @sakai:tag-uuid='")).
              append("')");
        }
        sb.append("] order by @jcr:score descending");
        String queryString = sb.toString();
        Session session = node.getSession();
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.XPATH);
        QueryResult queryResult = query.execute();
        SearchResultSet resultSet = searchServiceFactory.getSearchResultSet(queryResult.getRows(), MAX_RESULTS);
        RowIterator iterator = resultSet.getRowIterator();
        int count = 0;
        while ((count < MAX_RESULTS) && iterator.hasNext()) {
          Row row = iterator.nextRow();
          Node relatedNode = row.getNode();
          if (!nodePath.equals(relatedNode.getPath())) {
            ExtendedJSONWriter.writeNodeToWriter(writer, relatedNode);
            count++;
          }
        }
      }
      writer.endArray();
    } catch (RepositoryException e) {
      LOGGER.error("Error getting related content for " + request.getPathTranslated(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (JSONException e) {
      LOGGER.error("Error writing related content for " + request.getPathTranslated(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

}
