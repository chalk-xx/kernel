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
package org.sakaiproject.kernel.comments.servlets;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.api.comments.CommentsConstants;
import org.sakaiproject.kernel.api.comments.CommentsException;
import org.sakaiproject.kernel.api.comments.CommentsManager;
import org.sakaiproject.kernel.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sakai/commentsstore"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" values.0="create" values.1="get"
 * @scr.reference name="CommentsManager"
 *                interface="org.sakaiproject.kernel.api.comments.CommentsManager"
 *                bind="bindCommentsManager" unbind="unbindCommentsManager"
 */
public class CreateCommentsServlet extends SlingAllMethodsServlet {

  public static final Logger LOG = LoggerFactory.getLogger(CreateCommentsServlet.class);

  private CommentsManager commentsManager;

  protected void bindCommentsManager(CommentsManager commentsManager) {
    this.commentsManager = commentsManager;
  }

  protected void unbindCommentsManager(CommentsManager commentsManager) {
    this.commentsManager = null;
  }

  private static final long serialVersionUID = -773401134098855271L;

  @SuppressWarnings("unchecked")
  @Override
  protected void doPost(SlingHttpServletRequest request,
      org.apache.sling.api.SlingHttpServletResponse response) {

    LOG.info("Creating a comment.");
    Resource resource = request.getResource();
    try {

      Node storeNode = (Node) resource.adaptTo(Node.class);
      // Check if the user is logged in.
      String user = request.getRemoteUser();
      if (user == null || UserConstants.ANON_USERID.equals(user)) {
        // Default behavior is to allow anonymous comments.
        if (storeNode.hasProperty(CommentsConstants.PROP_ALLOWANONYMOUSCOMMENTS)) {
          boolean allowAnonymous = storeNode.getProperty(
              CommentsConstants.PROP_ALLOWANONYMOUSCOMMENTS).getBoolean();
          if (!allowAnonymous) {
            // The user is not allowed here..
            try {
              response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                  "User must be logged in to place a comment.");
              return;
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        } else {
          if (storeNode.hasProperty(CommentsConstants.PROP_ANON_FORCE_NAME)) {
            RequestParameter name = request.getRequestParameter(CommentsConstants.PROP_ANON_NAME);
            if (name == null) {
              try {
                response.sendError(400, "Anonymous comments must provide a name.");
                return;
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          }
          if (storeNode.hasProperty(CommentsConstants.PROP_ANON_FORCE_EMAIL)) {
            RequestParameter email = request.getRequestParameter(CommentsConstants.PROP_ANON_EMAIL);
            if (email == null) {
              try {
                response.sendError(400, "Anonymous comments must provide an email address.");
                return;
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
            // TODO Add email validation?
          }
        }
      }
      commentsManager.createComment(request.getParameterMap(), resource);

      if (storeNode.hasProperty(CommentsConstants.PROP_NOTIFICATION)) {
        // TODO Send an email to ?
      }
    } catch (CommentsException e) {
      e.printStackTrace();
      LOG.warn("Unable to create comment. {}", e.getMessage());
      try {
        response.sendError(e.getCode(), e.getMessage());
      } catch (IOException e1) {
        throw new RuntimeException(e);
      }
    } catch (RepositoryException e) {
      e.printStackTrace();
      LOG.warn("Unable to create comment. {}", e.getMessage());
    }

  }
}
