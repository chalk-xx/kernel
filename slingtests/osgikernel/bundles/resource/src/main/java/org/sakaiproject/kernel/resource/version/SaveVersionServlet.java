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
package org.sakaiproject.kernel.resource.version;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.sakaiproject.kernel.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Saves the current version of the JCR node identified by the Resource and checks out a new 
 * writeable version.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sling/servlet/default"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="save"
 * @scr.property name="sling.servlet.extensions" value="json"
 * 
 */
public class SaveVersionServlet extends SlingAllMethodsServlet {


  /**
   *
   */
  private static final long serialVersionUID = -7513481862698805983L;
  private static final Logger LOGGER = LoggerFactory.getLogger(SaveVersionServlet.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Resource resource = request.getResource();
      Node node = resource.adaptTo(Node.class);
      if (node == null) {
        response.setBufferSize(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      JcrUtils.logItem(LOGGER,node);
      Version version = null;
      try {
        version = node.checkin();
      } catch ( UnsupportedRepositoryOperationException e) {
        node.addMixin(JcrConstants.MIX_VERSIONABLE);
        node.save();
        version = node.checkin();
      }
      JcrUtils.logItem(LOGGER,node);

      node.checkout();
      if ( node.getSession().hasPendingChanges() ) {
        node.getSession().save();
      }
      JcrUtils.logItem(LOGGER,node);
      JsonItemWriter itemWriter = new JsonItemWriter(new HashSet<String>());
      itemWriter.dump(version, response.getWriter(), 2);
    } catch (RepositoryException e) {
      LOGGER.info("Failed to save versio ",e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
  }

}
