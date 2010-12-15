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

import com.google.common.collect.ImmutableMap;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_MEMBERS_NODE;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_MANAGER;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_RT;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet endpoint for comments associated to a piece of pooled content.
 */
@SlingServlet(methods = { "GET", "POST", "DELETE" }, extensions = "comments", resourceTypes = { "sakai/pooled-content" })
public class ContentPoolCommentServlet extends SlingAllMethodsServlet implements
    OptingServlet {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ContentPoolCommentServlet.class);
  private static final long serialVersionUID = 1L;
  private static final String COMMENT = "comment";
  private static final String COMMENTS = "comments";
  private static final String COMMENT_ID = "commentId";
  private static final String AUTHOR = "author";
  private static final String CREATED = "created";

  @Reference
  private ProfileService profileService;

  @Reference
  private Repository repository;

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
    Content poolContent = resource.adaptTo(Content.class);

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

    try {
      ContentManager contentManager = resource.adaptTo(ContentManager.class);
      Session session = resource.adaptTo(Session.class);
      AuthorizableManager authorizableManager = resource.adaptTo(Session.class)
          .getAuthorizableManager();
      Content comments = contentManager.get(poolContent.getPath() + "/" + COMMENTS);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      ExtendedJSONWriter w = new ExtendedJSONWriter(response.getWriter());
      w.setTidy(isTidy);
      w.object();
      w.key(COMMENTS);
      w.array();

      if (comments != null) {
        for (Content comment : comments.listChildren()) {
          Map<String, Object> properties = comment.getProperties();
          String authorId = StorageClientUtils.toString(properties.get(AUTHOR));
          Authorizable author = authorizableManager.findAuthorizable(authorId);
          ValueMap profile = profileService.getCompactProfileMap(author, session);
          w.object();

          w.valueMapInternals(profile);

          w.key(COMMENT);
          w.value(StorageClientUtils.toString(properties.get(COMMENT)));

          w.key(COMMENT_ID);
          w.value(comment.getPath());

          w.key(CREATED);
          w.value(StorageClientUtils.toString(properties.get(CREATED)));

          w.endObject();

        }
      }

      w.endArray();
      w.endObject();
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.error(e.getMessage(), e);
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
      adminSession = repository.loginAdministrative();
      ContentManager contentManager = adminSession.getContentManager();
      Content node = resource.adaptTo(Content.class);
      String path = node.getPath() + "/" + COMMENTS;

      Content comments = contentManager.get(path);
      if (comments == null) {
        comments = new Content(path, Collections.EMPTY_MAP);
        contentManager.update(comments);
      }

      // have the node name be the number of the comments there are
      Calendar cal = Calendar.getInstance();
      String newNodeName = Long.toString(cal.getTimeInMillis());
      Content newComment = new Content(path + "/" + newNodeName, ImmutableMap.of(AUTHOR,
          StorageClientUtils.toStore(request.getRemoteUser()), COMMENT,
          StorageClientUtils.toStore(body), CREATED, StorageClientUtils.toStore(cal)));

      contentManager.update(newComment);

      response.setStatus(HttpServletResponse.SC_CREATED);
    } catch (ClientPoolException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
    } finally {
      try {
        adminSession.logout();
      } catch (ClientPoolException e) {
        LOGGER.warn(e.getMessage(), e);
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
      Content poolItem = resource.adaptTo(Content.class);

      if (!isManager(poolItem, user)) {
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

      String path = poolItem.getPath() + "/" + COMMENTS + "/" + commentId;
      ContentManager contentManager = resource.adaptTo(ContentManager.class);
      contentManager.delete(path);
      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
    } finally {
      if (adminSession != null) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOGGER.warn(e.getMessage(), e);
        }
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
  private boolean isManager(Content poolItem, String userId) {
    Map<String, Object> properties = poolItem.getProperties();
    String managers = StorageClientUtils.toString(properties
        .get(POOLED_CONTENT_USER_MANAGER));
    // assuming there is no , in the principals
    return ( managers.indexOf(userId) > 0 );
  }
}
