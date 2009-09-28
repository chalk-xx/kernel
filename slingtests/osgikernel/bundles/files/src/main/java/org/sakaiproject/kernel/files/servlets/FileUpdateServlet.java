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

import com.google.common.collect.Lists;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.api.files.FilesConstants;

import java.io.IOException;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Update a file
 * 
 * @scr.component metatype="no" immediate="true" label="FileUpdateServlet" description
 *                ="Servlet to allow to update a file."
 * @scr.property name="service.description" value="Updates files in the store."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/file"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="update"
 */
public class FileUpdateServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = -625686874623971605L;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    try {
      RequestParameter file = request.getRequestParameter("Filedata");
      if (file == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Missing Filedata parameter.");
      }

      Session session = request.getResourceResolver().adaptTo(Session.class);
      Node node = request.getResource().adaptTo(Node.class);
      String path;
      path = node.getPath();
      String id = node.getName();
      // Try to determine the real content type.
      // get content type
      String contentType = file.getContentType();
      if (contentType != null) {
        int idx = contentType.indexOf(';');
        if (idx > 0) {
          contentType = contentType.substring(0, idx);
        }
      }
      if (contentType == null || contentType.equals("application/octet-stream")) {
        ServletContext context = this.getServletConfig().getServletContext();
        contentType = context.getMimeType(file.getFileName());
        if (contentType == null || contentType.equals("application/octet-stream")) {
          contentType = "application/octet-stream";
        }
      }

      Node fileNode = FileUtils.saveFile(session, path, id, file, contentType);
      String fileName = fileNode.getProperty(FilesConstants.SAKAI_FILENAME).getString();

      RequestParameter[] links = request.getRequestParameters("link");

      List<String> createdLinks = Lists.newArrayList();
      for (RequestParameter link : links) {
        String linkPath = link.getString();
        if (!linkPath.startsWith("/")) {
          response.sendError(400, "A link should be an absolute path.");
          return;
        }
        if (!linkPath.endsWith("/"))
          linkPath += "/";
        linkPath += fileName;
        FileUtils.createLink(session, node, linkPath);
        createdLinks.add(linkPath);
      }

      session.save();

      // Print a JSON response back
      JSONWriter writer = new JSONWriter(response.getWriter());
      writer.object();
      writer.key("id");
      writer.value(id);
      writer.key("filename");
      writer.value(fileName);
      writer.key("path");
      writer.value(FileUtils.getDownloadPath(node));
      writer.key("links");
      writer.array();
      for (String link : createdLinks) {
        writer.value(link);
      }
      writer.endArray();
      writer.endObject();

    } catch (RepositoryException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } catch (JSONException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

  }

}
