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
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 * Create a jcrinternal link to a file.
 * 
 * @scr.component metatype="no" immediate="true" label="FileCreateLinkServlet"
 * @scr.property name="service.description" value="Creates an internal link to a file."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/file"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="link"
 * @scr.reference name="SlingRepository"
 *                interface="org.apache.sling.jcr.api.SlingRepository"
 */
public class FileCreateLinkServlet extends SlingAllMethodsServlet {

  public static final Logger log = LoggerFactory.getLogger(FileCreateLinkServlet.class);
  private static final long serialVersionUID = -6206802633585722105L;
  private static final String LINK_PARAM = "link";

  private SlingRepository slingRepository;
  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }
  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      String[] links = request.getParameterValues(LINK_PARAM);
      Session session = request.getResourceResolver().adaptTo(Session.class);
      Node fileNode = request.getResource().adaptTo(Node.class);
      JSONWriter write = new JSONWriter(response.getWriter());
      write.array();
      for (String link : links) {
        write.object();
        write.key("link");
        write.value(link);
        try {
          String linkPath = FileUtils.createLink(session, fileNode, link, slingRepository);
          write.key("path");
          write.value(linkPath);
          write.key("succes");
          write.value(true);
        } catch (RepositoryException e) {
          write.key("succes");
          write.value(false);
        }
        write.endObject();
      }
      write.endArray();
    } catch (JSONException ex) {
      response.sendError(500, "Could not write JSON response.");
    }
  }
}
