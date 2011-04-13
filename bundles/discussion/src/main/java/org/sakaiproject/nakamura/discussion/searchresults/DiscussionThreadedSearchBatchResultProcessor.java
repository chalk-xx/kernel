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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.discussion.DiscussionConstants;
import org.sakaiproject.nakamura.api.discussion.Post;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Formats message node search results
 */
@Component(label = "%discussion.threadedSearchBatch.label", description = "%discussion.threadedSearchBatch.desc")
@Service
public class DiscussionThreadedSearchBatchResultProcessor implements
    SolrSearchBatchResultProcessor {

  public static final Logger LOG = LoggerFactory
      .getLogger(DiscussionThreadedSearchBatchResultProcessor.class);

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "DiscussionThreaded")
  static final String SEARCH_BATCHPROCESSOR = "sakai.search.batchprocessor";

  @Reference
  PresenceService presenceService;


  @Reference
  SolrSearchServiceFactory searchServiceFactory;

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#writeResults(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.commons.json.io.JSONWriter, java.util.Iterator)
   */
  public void writeResults(SlingHttpServletRequest request, JSONWriter writer,
      Iterator<Result> iterator) throws JSONException {

    try {
      Session session = StorageClientUtils.adaptToSession(request.getResourceResolver()
          .adaptTo(javax.jcr.Session.class));
      ContentManager cm = session.getContentManager();
      List<String> basePosts = new ArrayList<String>();
      Map<String,List<Post>> postChildren = new HashMap<String, List<Post>>();
      Map<String,Post> allPosts = new HashMap<String, Post>();
      while (iterator.hasNext()) {
        Result result = iterator.next();
        Content content = cm.get(result.getPath());
        if (content == null) {
          continue;
        }
        Post p = new Post(content, session);
        allPosts.put((String) content
            .getProperty(MessageConstants.PROP_SAKAI_ID), p);

        if (content.hasProperty(DiscussionConstants.PROP_REPLY_ON)) {
          // This post is a reply on another post.
          String replyon = (String) content
              .getProperty(DiscussionConstants.PROP_REPLY_ON);
          if (!postChildren.containsKey(replyon)) {
            postChildren.put(replyon, new ArrayList<Post>());
          }

          postChildren.get(replyon).add(p);

        } else {
          // This post is not a reply to another post, thus it is a basepost.
          basePosts.add(p.getPostId());
        }
      }

      // Now that we have all the base posts, we can sort the replies properly
      for (String parentId : postChildren.keySet()) {
        Post parentPost = allPosts.get(parentId);
        if (parentPost != null) {
          List<Post> childrenList = parentPost.getChildren();
          List<Post> childrenActual = postChildren.get(parentId);
          childrenList.addAll(childrenActual);
        }
      }

      // The posts are sorted, now return them as json.
      for (String basePostId : basePosts) {
        allPosts.get(basePostId).outputPostAsJSON((ExtendedJSONWriter) writer,
            presenceService, /*profileService,*/ session);
      }
    } catch (StorageClientException e) {
      throw new RuntimeException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new RuntimeException(e.getMessage(), e);
    } catch (RepositoryException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      java.lang.String)
   */
  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query)
      throws SolrSearchException {
      // Return the result set.
      return searchServiceFactory.getSearchResultSet(request, query);
  }
}
