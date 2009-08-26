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
package org.sakaiproject.kernel.discussion.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.kernel.api.discussion.DiscussionConstants;
import org.sakaiproject.kernel.api.discussion.DiscussionManager;
import org.sakaiproject.kernel.api.discussion.DiscussionUtils;
import org.sakaiproject.kernel.api.discussion.Post;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.sakaiproject.kernel.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.servlet.ServletException;

/**
 * 
 * Gets all the discussion posts under a store and makes them threaded.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/discussionstore"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.selectors" values.0="get"
 * @scr.property name="sling.servlet.extensions" value="json"
 * @scr.reference name="DiscussionManager"
 *                interface="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 *                bind="bindDiscussionManager" unbind="unbindDiscussionManager"
 * @scr.reference name="SiteService"
 *                interface="org.sakaiproject.kernel.api.site.SiteService"
 *                bind="bindSiteService" unbind="unbindSiteService"
 */
public class GetDiscussionServlet extends SlingAllMethodsServlet {

  public static final Logger LOG = LoggerFactory.getLogger(GetDiscussionServlet.class);

  /**
   * 
   */
  private static final long serialVersionUID = 6847866175504156603L;
  private DiscussionManager discussionManager;
  private SiteService siteService;

  protected void bindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }

  protected void unbindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = null;
  }

  protected void bindSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  protected void unbindSiteService(SiteService siteService) {
    this.siteService = null;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Resource store = request.getResource();

    // Paging and sorting is only really available to a flat representation.
    RequestParameter startParam = request
        .getRequestParameter(DiscussionConstants.PARAM_START);
    RequestParameter itemsParam = request
        .getRequestParameter(DiscussionConstants.PARAM_ITEMS);
    RequestParameter[] sortParam = request
        .getRequestParameters(DiscussionConstants.PARAM_SORT);
    RequestParameter viewModeParam = request
        .getRequestParameter(DiscussionConstants.PARAM_VIEWMODE);

    // Default values.
    int start = 0;
    int items = 10;
    List<String[]> sorts = new ArrayList<String[]>();
    String viewMode = "flat";

    if (startParam != null) {
      try {
        start = Integer.parseInt(startParam.getString());
      } catch (NumberFormatException nfe) {
        LOG.warn("Could not parse the start parameter. {}", startParam.getString());
      }
    }
    if (itemsParam != null) {
      try {
        items = Integer.parseInt(itemsParam.getString());
      } catch (NumberFormatException nfe) {
        LOG.warn("Could not parse the items parameter: {}", itemsParam.getString());
      }
    }
    if (sortParam != null) {
      for (RequestParameter p : sortParam) {
        try {
          String[] s = StringUtils.split(p.getString(), ',');
          if (s.length == 2) {
            sorts.add(new String[] { s[0], s[1] });
          } else {
            throw new IllegalArgumentException();
          }

        } catch (IllegalArgumentException ie) {
          LOG.warn("Invalid sort parameter: " + p.getString());
        }
      }
    }

    if (viewModeParam != null && viewModeParam.getString().equals("threaded")) {
      viewMode = "threaded";
      sorts.clear();
      // Threaded mode requires the nodes to be sorted on date ascending..
      String[] s = { "jcr:created", "ascending" };
      sorts.add(s);
    }


    String currentUser = request.getRemoteUser();
    boolean siteCollaborator = DiscussionUtils.isSiteCollaborator(currentUser, store, siteService);
    
    // Get all the nodes.
    NodeIterator it = discussionManager.getDiscussionPosts(store, sorts);
    List<Node> allNodes = new ArrayList<Node>();
    for (; it.hasNext();) {
      allNodes.add(it.nextNode());
    }

    try {
      List<Post> basePosts = new ArrayList<Post>();
      for (int i = 0; i < allNodes.size(); i++) {
        Node n = allNodes.get(i);        
        
        // If the request viewmode is threaded, Posts can contain posts.
        if (viewMode.equals("threaded")) {
          if (n.hasProperty(DiscussionConstants.PROP_REPLY_ON)) {
            String replyon = n.getProperty(DiscussionConstants.PROP_REPLY_ON).getString();
            // This post is a reply on another post.
            // Find that post and add it.
            addPost(basePosts, n, replyon);

          } else {
            // This post is not a reply to another post, thus it is a basepost.
            basePosts.add(new Post(n));
          }
        } else {
          // Flat representation.
          // flat representation can be paged.
          if (i >= start && i < start + items) {
            basePosts.add(new Post(n));
          }
        }
      }


      // The posts are sorted, now return them as json.
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.object();
      writer.key("total");
      writer.value(it.getSize());
      writer.key("items");
      writer.array();

      for (Post p : basePosts) {
        p.outputPostAsJSON(writer, currentUser, siteCollaborator);
      }

      writer.endArray();
      writer.endObject();

    } catch (RepositoryException e) {
      LOG.warn("Unable to fetch the posts: {}", e.getMessage());
      e.printStackTrace();
      response.sendError(500, e.getMessage());
    } catch (JSONException e) {
      LOG.warn("Unable to parse the posts into JSON: {}", e.getMessage());
      e.printStackTrace();
      response.sendError(500, e.getMessage());
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
    String postid = n.getProperty(DiscussionConstants.PROP_POST_ID).getString();
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
