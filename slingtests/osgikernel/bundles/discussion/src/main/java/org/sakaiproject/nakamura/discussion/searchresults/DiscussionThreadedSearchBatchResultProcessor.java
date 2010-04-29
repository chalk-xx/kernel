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
package org.sakaiproject.nakamura.discussion.searchresults;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.discussion.DiscussionConstants;
import org.sakaiproject.nakamura.api.discussion.Post;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.search.AbstractSearchResultSet;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.util.RowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

/**
 * Formats message node search results
 */
@Component(immediate = true, label = "%discussion.threadedSearchBatch.label", description = "%discussion.threadedSearchBatch.desc")
@Service
public class DiscussionThreadedSearchBatchResultProcessor implements
    SearchBatchResultProcessor {

  public static final Logger LOG = LoggerFactory
      .getLogger(DiscussionThreadedSearchBatchResultProcessor.class);

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "DiscussionThreaded")
  static final String SEARCH_BATCHPROCESSOR = "sakai.search.batchprocessor";

  public void writeNodes(SlingHttpServletRequest request, JSONWriter writer,
      Aggregator aggregator, RowIterator iterator) throws JSONException,
      RepositoryException {

    Session session = request.getResourceResolver().adaptTo(Session.class);
    List<Node> allNodes = new ArrayList<Node>();
    for (; iterator.hasNext();) {
      Node node = RowUtils.getNode(iterator.nextRow(), session);
      if (aggregator != null) {
        aggregator.add(node);
      }
      allNodes.add(node);
    }

    List<Post> basePosts = new ArrayList<Post>();
    for (int i = 0; i < allNodes.size(); i++) {
      Node n = allNodes.get(i);

      if (n.hasProperty(DiscussionConstants.PROP_REPLY_ON)) {
        String replyon = n.getProperty(DiscussionConstants.PROP_REPLY_ON)
            .getString();
        // This post is a reply on another post.
        // Find that post and add it.
        addPost(basePosts, n, replyon);

      } else {
        // This post is not a reply to another post, thus it is a basepost.
        basePosts.add(new Post(n));
      }
    }

    // The posts are sorted, now return them as json.

    for (Post p : basePosts) {
      p.outputPostAsJSON(writer);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SearchException {
    try {
      // Perform the query
      QueryResult qr = query.execute();
      RowIterator iterator = qr.getRows();

      // Get the hits
      long hits = SearchUtil.getHits(qr);

      // Return the result set.
      return new AbstractSearchResultSet(iterator, hits);
    } catch (RepositoryException e) {
      throw new SearchException(500, "Unable to execute query.");
    }
  }

  /**
   * Adds the post to the list at the correct place.
   * 
   * @param basePosts
   * @param n
   * @return
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws RepositoryException
   */
  private void addPost(List<Post> basePosts, Node n, String replyon)
      throws ValueFormatException, PathNotFoundException, RepositoryException {
    String postid = n.getProperty(MessageConstants.PROP_SAKAI_ID).getString();
    for (Post p : basePosts) {
      if (p.getPostId().equals(replyon)) {
        p.getChildren().add(new Post(n));
        break;
      } else {
        p.addPost(n, postid, replyon);
      }
    }
  }

}
