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

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_MEMBERS_NODE;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_MANAGER;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_RT;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet endpoint for comments associated to a piece of pooled content.
 */
@SlingServlet(methods = {"GET", "POST", "DELETE"}, extensions = "comments", resourceTypes = { "sakai/pooled-content" })
public class ContentPoolCommentServlet extends SlingAllMethodsServlet implements OptingServlet {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ContentPoolCommentServlet.class);
  private static final long serialVersionUID = 1L;
  private static final String COMMENT = "comment";
  private static final String COMMENTS = "comments";
  private static final String COMMENT_ID = "commentId";
  private static final String AUTHOR = "author";
  private static final String CREATED = "created";

  @Reference
  private SlingRepository repository;

  @Reference
  private ProfileService profileService;

  /**
   * Determine if we will accept this request. Had to add this because something is
   * triggering this servlet to take GET requests even though the extension != comments.
   *
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.OptingServlet#accepts(org.apache.sling.api.SlingHttpServletRequest)
   */
  public boolean accepts(SlingHttpServletRequest request) {
    RequestPathInfo rpi = request.getRequestPathInfo();
    boolean willAccept = "comments".equals(rpi.getExtension());
    return willAccept;
  }

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

      ExtendedJSONWriter w = new ExtendedJSONWriter(response.getWriter());
      w.setTidy(isTidy);
      w.object();
      w.key(COMMENTS);
      w.array();

      if (node.hasNode(COMMENTS)) {
        UserManager userManager = AccessControlUtil.getUserManager(adminSession);

        Node comments = node.getNode(COMMENTS);
        NodeIterator nodes = comments.getNodes();
        while (nodes.hasNext()) {
          Node n = nodes.nextNode();

          String authorId = n.getProperty(AUTHOR).getString();
          Authorizable author = userManager.getAuthorizable(authorId);
          ValueMap profile = profileService.getCompactProfileMap(author, adminSession);
          w.object();

          w.valueMapInternals(profile);

          w.key(COMMENT);
          w.value(n.getProperty(COMMENT).getString());

          w.key(COMMENT_ID);
          w.value(n.getName());

          w.key(CREATED);
          w.value(n.getProperty(CREATED).getString());

          w.endObject();
        }
      }

      w.endArray();
      w.endObject();
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.error(e.getMessage(), e);
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
    String body = request.getParameter(COMMENT);

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
      String path = node.getPath() + "/" + COMMENTS;
      Node commentsNode = JcrUtils.deepGetOrCreateNode(adminSession, path);

      // have the node name be the number of the comments there are
      Calendar cal = Calendar.getInstance();
      String newNodeName = Long.toString(cal.getTimeInMillis());
      Node newComment = commentsNode.addNode(newNodeName);
      newComment.setProperty(AUTHOR, user);
      newComment.setProperty(COMMENT, body);
      newComment.setProperty(CREATED, cal);

      // save the session and return a 'created' status
      adminSession.save();
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

  @Override
  protected void doDelete(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    String user = request.getRemoteUser();
    String commentId = request.getParameter("commentId");

    Session adminSession = null;
    try {
      // check that user is a manager of the content item
      Resource resource = request.getResource();
      Node node = resource.adaptTo(Node.class);
      if (!isManager(node, user)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
            "Must be a manager of the pooled content item to delete a comment.");
        return;
      }

      // stop now if no comment ID is provided
      if (StringUtils.isBlank(commentId)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "'commentId' must be provided.");
        return;
      }

      String path = resource.getPath() + "/" + COMMENTS + "/" + commentId;

      adminSession = repository.loginAdministrative(null);
      adminSession.removeItem(path);
      adminSession.save();
      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.error(e.getMessage(), e);
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    }
  }

  /**
   * Gets all the "members" for a file.
   *
   * @param node
   *          The node that represents the file.
   * @return A map where each key is a userid, the value is a boolean that states if it is
   *         a manager or not.
   * @throws RepositoryException
   */
  private boolean isManager(Node node, String userId) throws RepositoryException {
    boolean isManager = false;

    Session session = node.getSession();

    // Perform a query that gets all the "member" nodes.
    String path = ISO9075.encodePath(node.getPath());
    StringBuilder sb = new StringBuilder("/jcr:root/");
    sb.append(path).append(POOLED_CONTENT_MEMBERS_NODE).append("//*[@").append(SLING_RESOURCE_TYPE_PROPERTY);
    sb.append("='").append(POOLED_CONTENT_USER_RT).append("']");
    QueryManager qm = session.getWorkspace().getQueryManager();
    Query q = qm.createQuery(sb.toString(), "xpath");
    QueryResult qr = q.execute();
    NodeIterator iterator = qr.getNodes();

    // Loop over the "member" nodes.
    while (iterator.hasNext()) {
      Node memberNode = iterator.nextNode();
      if (userId.equals(memberNode.getName())
          && memberNode.hasProperty(POOLED_CONTENT_USER_MANAGER)) {
        isManager = true;
        break;
      }
    }

    return isManager;
  }
}
