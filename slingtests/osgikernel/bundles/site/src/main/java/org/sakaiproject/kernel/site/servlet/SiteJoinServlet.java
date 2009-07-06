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
package org.sakaiproject.kernel.site.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.kernel.api.site.SiteException;
import org.sakaiproject.kernel.api.site.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceJoinServlet</code> supports Join for joinable
 * sites and initiates authorized Joining workflow.
 * 
 * @scr.component immediate="true" label="SiteJoinServlet"
 *                description="Join servlet for site service"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="Supports Join functionality on the site."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sakai/site"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="join"
 */
public class SiteJoinServlet extends AbstractSiteServlet {

  private static final long serialVersionUID = 7673360724593565303L;
  private static final Logger LOG = LoggerFactory
      .getLogger(SiteJoinServlet.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    LOG.info("Got get to SiteServiceGetServlet");
    Node site = request.getResource().adaptTo(Node.class);
    if (site == null) {
      response.sendError(HttpServletResponse.SC_NO_CONTENT,
          "Couldn't find site node");
      return;
    }
    if (!getSiteService().isSite(site)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Location does not represent site ");
      return;
    }
    RequestParameter requestedGroup = request
        .getRequestParameter(SiteService.PARAM_GROUP);
    if (requestedGroup == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Target Group must be specified in the request parameter "
              + SiteService.PARAM_GROUP);
      return;
    }

    try {
      getSiteService().joinSite(site, requestedGroup.getString());

      response.sendError(HttpServletResponse.SC_OK);
    } catch (SiteException ex) {
      response.sendError(ex.getStatusCode(), ex.getMessage());
    }
    return;

  }

}
