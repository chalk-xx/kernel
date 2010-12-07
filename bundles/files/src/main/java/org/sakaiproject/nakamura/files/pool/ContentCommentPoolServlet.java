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

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet endpoint for comments associated to a piece of pooled content.
 */
@SlingServlet(methods = { "GET", "POST" }, extensions = { "comment" }, resourceTypes = { "sakai/pooled-content" })
public class ContentCommentPoolServlet extends SlingAllMethodsServlet {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ContentCommentPoolServlet.class);
  private static final long serialVersionUID = 1L;
  private static final String COMMENTS_NODE = "comments";

  @Reference
  SlingRepository repository;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Resource resource = request.getResource();
    Node node = resource.adaptTo(Node.class);

    boolean isTidy = false;
    String[] selectors = request.getRequestPathInfo().getSelectors();
    if (selectors != null) {
      for (int i = 0; i < selectors.length; i++) {
        String selector = selectors[i];
        if ("tidy".equals(selector)) {
          isTidy = true;
        }
      }
    }

    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative(null);

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      JSONWriter w = new JSONWriter(response.getWriter());
      w.setTidy(isTidy);
      w.object();
      w.key("comments");
      w.array();

      if (node.hasNode(COMMENTS_NODE)) {
        Node comments = node.getNode(COMMENTS_NODE);
        NodeIterator nodes = comments.getNodes();
        while (nodes.hasNext()) {
          Node n = nodes.nextNode();
          w.object();
          w.key("sakai:author");
          w.value(n.getProperty("sakai:author").getString());
          w.key("sakai:comment");
          w.value(n.getProperty("sakai:comment").getString());
          w.key("sakai:created");
          w.value(n.getProperty("sakai:created").getString());
          w.endObject();
        }
      }

      w.endArray();
      w.endObject();
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.info("Caught JSONException {}", e.getMessage());
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.error(e.getMessage(), e);
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    }
  }

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // collect the items we'll store
    String user = request.getRemoteUser();
    String body = request.getParameter("sakai:body");

    // stop now if user is not logged in
    if ("anonymous".equals(user)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Must be logged in to post a comment.");
      return;
    }

    // stop now if no comment provided
    if (StringUtils.isBlank(body)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "'comment' must be provided.");
      return;
    }

    Resource resource = request.getResource();
    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative(null);
      Node node = resource.adaptTo(Node.class);
      String path = node.getPath() + "/" + COMMENTS_NODE;
      Node commentsNode = JcrUtils.deepGetOrCreateNode(adminSession, path);

      // have the node name be the number of the comments there are
      String newNodeName = Long.toString(commentsNode.getNodes().getSize());
      Node newComment = commentsNode.addNode(newNodeName);
      newComment.setProperty("sakai:author", user);
      newComment.setProperty("sakai:comment", body);
      newComment.setProperty("sakai:created", Calendar.getInstance());

      // save the session and return a 'created' status
      if (adminSession.hasPendingChanges()) {
        adminSession.save();
      }
      response.setStatus(HttpServletResponse.SC_CREATED);
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.error(e.getMessage(), e);
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    }
  }
}
