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
package org.sakaiproject.kernel.discussion.searchresults;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.discussion.DiscussionConstants;
import org.sakaiproject.kernel.api.discussion.Post;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.search.SearchBatchResultProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * Formats message node search results
 * 
 * @scr.component immediate="true" label="DiscussionSearchBatchResultProcessor"
 *                description="Formatter for discussion search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.batchprocessor" value="DiscussionThreaded"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchBatchResultProcessor"
 */
public class DiscussionThreadedSearchBatchResultProcessor implements SearchBatchResultProcessor {

  public static final Logger LOG = LoggerFactory
      .getLogger(DiscussionThreadedSearchBatchResultProcessor.class);

  public void writeNodeIterator(JSONWriter writer, NodeIterator nodeIterator) throws JSONException,
      RepositoryException {

    LOG.info("Making a threaded view of discussions");
    List<Node> allNodes = new ArrayList<Node>();
    for (; nodeIterator.hasNext();) {
      allNodes.add(nodeIterator.nextNode());
    }

    List<Post> basePosts = new ArrayList<Post>();
    for (int i = 0; i < allNodes.size(); i++) {
      Node n = allNodes.get(i);

      if (n.hasProperty(DiscussionConstants.PROP_SAKAI_REPLY_ON)) {
        String replyon = n.getProperty(DiscussionConstants.PROP_SAKAI_REPLY_ON).getString();
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
   * Adds the post to the list at the correct place.
   * 
   * @param basePosts
   * @param n
   * @return
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws RepositoryException
   */
  private void addPost(List<Post> basePosts, Node n, String replyon) throws ValueFormatException,
      PathNotFoundException, RepositoryException {
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
