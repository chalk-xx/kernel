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
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Gets or sets settings on the BigStore node. This should be temp.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/commentsstore"
 * @scr.property name="sling.servlet.methods" values.0="POST" values.1="GET"
 * @scr.property name="sling.servlet.selectors" values.0="settings"
 */
public class SettingsCommentsServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -2695017481393638511L;

  public static final Logger LOG = LoggerFactory
      .getLogger(SettingsCommentsServlet.class);

  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {

    try {
      Node n = (Node) request.getResource().adaptTo(Node.class);
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.node(n);
    } catch (Exception ex) {
      LOG.warn("Unable to get settings. {}", ex.getMessage());
      ex.printStackTrace();
      response.sendError(500, "Unable to get settings.");
    }

  }

  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {

    try {
      Node n = (Node) request.getResource().adaptTo(Node.class);
      /*
       * RequestParameter allowAnonymousCommentsParam = request
       * .getRequestParameter(CommentsConstants.PROP_ALLOWANONYMOUSCOMMENTS);
       * RequestParameter notifyParam = request
       * .getRequestParameter(CommentsConstants.PROP_NOTIFICATION);
       * 
       * if (allowAnonymousCommentsParam != null) {
       * n.setProperty(CommentsConstants.PROP_ALLOWANONYMOUSCOMMENTS,
       * allowAnonymousCommentsParam.getString()); }
       * 
       * if (notifyParam != null) {
       * n.setProperty(CommentsConstants.PROP_NOTIFICATION, notifyParam
       * .getString()); }
       */

      Map<String, String[]> map = request.getParameterMap();
      for (Entry<String, String[]> param : map.entrySet()) {
        String[] values = param.getValue();
        if (values.length == 1) {
          n.setProperty(param.getKey(), values[0]);
        } else {
          n.setProperty(param.getKey(), values);
        }
      }
      
      n.save();

    } catch (Exception ex) {
      LOG.warn("Unable to save settings. {}", ex.getMessage());
      ex.printStackTrace();
      response.sendError(500, "Unable to save settings.");
    }

  }
}
