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
package org.sakaiproject.kernel.files.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.api.site.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 * Dumps the info for a link.
 * 
 * @scr.component metatype="no" immediate="true" label="FileLinkInfoServlet"
 *                description="Gives info about the actual file"
 * @scr.property name="service.description" value="Links nodes to files"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/link"
 * @scr.property name="sling.servlet.methods" values.0="GET"
 * @scr.property name="sling.servlet.selectors" value="info"
 * @scr.reference name="SiteService"
 *                interface="org.sakaiproject.kernel.api.site.SiteService"
 */
public class FileLinkInfoServlet extends SlingAllMethodsServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileLinkInfoServlet.class);
  private static final long serialVersionUID = -527034533334782419L;
  private SiteService siteService;

  protected void bindSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  protected void unbindSiteService(SiteService siteService) {
    this.siteService = null;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    try {
      Node node = (Node) request.getResource().adaptTo(Node.class);
      Session session = request.getResourceResolver().adaptTo(Session.class);
      JSONWriter write = new JSONWriter(response.getWriter());
      FileUtils.writeLinkNode(node, session, write, siteService);
    } catch (RepositoryException e) {
      LOGGER.warn("Unable to get file info for link.");
      e.printStackTrace();
      response.sendError(500, "Unable get file info.");

      e.printStackTrace();
    } catch (JSONException e) {
      response.sendError(500, "Unable to parse JSON.");
    }
  }
}
