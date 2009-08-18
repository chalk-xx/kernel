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
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.api.comments.CommentsConstants;
import org.sakaiproject.kernel.api.comments.CommentsManager;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.sakaiproject.kernel.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Gets all the comments under a store.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/commentsstore"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.selectors" values.0="get"
 * @scr.property name="sling.servlet.extensions" value="json"
 * @scr.reference name="CommentsManager"
 *                interface="org.sakaiproject.kernel.api.comments.CommentsManager"
 *                bind="bindCommentsManager" unbind="unbindCommentsManager"
 */
public class GetCommentsServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -9136252302512119535L;

  public static final Logger LOG = LoggerFactory
      .getLogger(GetCommentsServlet.class);

  private CommentsManager commentsManager;

  protected void bindCommentsManager(CommentsManager commentsManager) {
    this.commentsManager = commentsManager;
  }

  protected void unbindCommentsManager(CommentsManager commentsManager) {
    this.commentsManager = null;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {

    RequestParameter startParam = request
        .getRequestParameter(CommentsConstants.PARAM_START);
    RequestParameter itemsParam = request
        .getRequestParameter(CommentsConstants.PARAM_ITEMS);
    RequestParameter[] sortParam = request
        .getRequestParameters(CommentsConstants.PARAM_SORT);

    // Default values.
    int start = 0;
    int items = 10;
    List<String[]> sorts = new ArrayList<String[]>();

    if (startParam != null) {
      try {
        start = Integer.parseInt(startParam.getString());
      } catch (NumberFormatException nfe) {
        LOG.warn("Could not parse the start parameter. {}", startParam
            .getString());
      }
    }
    if (itemsParam != null) {
      try {
        items = Integer.parseInt(itemsParam.getString());
      } catch (NumberFormatException nfe) {
        LOG.warn("Could not parse the items parameter: {}", itemsParam
            .getString());
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
    try {
      
      String currentUser = request.getRemoteUser();
      
      Node storeNode = (Node) request.getResource().adaptTo(Node.class);
      
      // Get the comments
      NodeIterator it = commentsManager.getComments(request.getResource(),
          sorts);

      // Write the comments.
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.object();
      writer.key("total");
      writer.value(it.getSize());
      writer.key("comments");
      commentsManager.outputCommentsAsJSON(it, writer, request
          .getResourceResolver(), start, items, currentUser, storeNode);
      writer.endObject();

    } catch (Exception e) {
      LOG.warn("Failed to create a comment: {}", e.getMessage());
      e.printStackTrace();
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getMessage());
    }

  }

}
