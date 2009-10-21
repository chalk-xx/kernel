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

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Create a jcrinternal link to a file.
 */
@SlingServlet(methods = { "POST" }, selectors = { "link" }, resourceTypes = { "sakai/file" })
@Properties(value = {
    @Property(name = "service.description", value = "Creates an internal link to a file."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class FileCreateLinkServlet extends SlingAllMethodsServlet {

  public static final Logger log = LoggerFactory.getLogger(FileCreateLinkServlet.class);
  private static final long serialVersionUID = -6206802633585722105L;
  private static final String LINK_PARAM = "link";
  private static final String SITE_PARAM = "site";

  @Reference
  private SlingRepository slingRepository;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    String[] sites = request.getParameterValues(SITE_PARAM);
    String[] links = request.getParameterValues(LINK_PARAM);
    if (sites == null || links == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "A site and link parameter have to be provided.");
      return;
    }
    if (sites.length != links.length) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The site parameter's length doesn't match with the link's parameter length.");
      return;
    }

    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      Node fileNode = request.getResource().adaptTo(Node.class);
      String fileName = fileNode.getProperty(FilesConstants.SAKAI_FILENAME).getString();
      JSONWriter write = new JSONWriter(response.getWriter());
      write.array();
      for (int i = 0; i < links.length; i++) {
        String link = links[i];
        if (!link.endsWith("/")) {
          link += "/";
        }
        link += fileName;

        String site = sites[i];
        write.object();
        write.key("link");
        write.value(link);
        try {
          String linkPath = FileUtils.createLink(session, fileNode, link, site,
              slingRepository);
          write.key("path");
          write.value(linkPath);
          write.key("succes");
          write.value(true);
        } catch (RepositoryException e) {
          e.printStackTrace();
          write.key("succes");
          write.value(false);
        }
        write.endObject();
      }
      write.endArray();
    } catch (JSONException ex) {
      response.sendError(500, "Could not write JSON response.");
    } catch (RepositoryException e) {
      response.sendError(500, "Unable to get filename");
    }
  }
}
