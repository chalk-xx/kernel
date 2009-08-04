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
package org.sakaiproject.kernel.activity;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * In addition to the required parameters, properties should be included that will be used
 * to fill the bundle (i.e. macro expansions).
 * <p>
 * Required parameters:<br/>
 * actor: (i.e. the person taking the action)<br/>
 * appId: (i.e. used to locate the bundles)<br/>
 * bundleId: (i.e. you know - the bundleId. Language codes will be prepended to the
 * bundleId for resolution.)
 * 
 * @scr.component immediate="true" label="ActivityServlet"
 *                description="Records the activity related to a particular node"
 * @scr.property name="service.description"
 *               value="Records the activity related to a particular node"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.selectors" value="activity"
 * @scr.property name="sling.servlet.methods" values.0="POST"
 */
public class ActivityServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = 1375206766455341437L;
  private static final Logger LOG = LoggerFactory.getLogger(ActivityServlet.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("doPost(SlingHttpServletRequest " + request
          + ", SlingHttpServletResponse " + response + ")");
    }
    RequestParameter actor = request.getRequestParameter("actor");
    if (actor == null || "".equals(actor)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The actor parameter must not be null");
      return;
    }
    RequestParameter appId = request.getRequestParameter("appId");
    if (appId == null || "".equals(appId)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The appId parameter must not be null");
      return;
    }
    RequestParameter bundleId = request.getRequestParameter("bundleId");
    if (bundleId == null || "".equals(bundleId)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The bundleId parameter must not be null");
      return;
    }
    super.doPost(request, response);
    // TODO fire OSGi event to trigger JMS listener for delivery of activity
  }

}
