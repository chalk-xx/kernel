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
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.kernel.api.cluster.ClusterTrackingService;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Create a file
 * 
 * @scr.component metatype="no" immediate="true" label="FilesUploadServlet" description
 *                ="Servlet to allow uploading of files to the store."
 * @scr.property name="service.description" value="Uploads files to the store."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/files"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="upload"
 * @scr.reference name="ClusterTrackingService"
 *                interface="org.sakaiproject.kernel.api.cluster.ClusterTrackingService"
 * @scr.reference name="SlingRepository"
 *                interface="org.apache.sling.jcr.api.SlingRepository"
 */
public class FilesUploadServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = -2582970789079249113L;

  private ClusterTrackingService clusterTrackingService;

  protected void bindClusterTrackingService(ClusterTrackingService clusterTrackingService) {
    this.clusterTrackingService = clusterTrackingService;
  }

  protected void unbindClusterTrackingService(
      ClusterTrackingService clusterTrackingService) {
    this.clusterTrackingService = null;
  }

  private SlingRepository slingRepository;

  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

  public static final Logger LOG = LoggerFactory.getLogger(FilesUploadServlet.class);

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    String store = request.getResource().getPath();
    LOG.info("Attempted upload for " + session.getUserID());

    // If there is a link parameter provided than we will create a
    // link for each file under this path.
    RequestParameter linkParam = request.getRequestParameter("link");
    if (linkParam != null) {
      String link = linkParam.getString();
      if (!link.startsWith("/")) {
        response
            .sendError(500,
                "If a link location is specified, it should be absolute and point to a folder.");
        return;
      }
    }

    List<Node> fileNodes = Lists.newArrayList();
    List<String> links = Lists.newArrayList();

    // Create the files and links.
    try {
      // Handle multi files
      RequestParameter[] files = request.getRequestParameters("Filedata");
      if (files == null) {
        response.sendError(400, "Missing Filedata parameter.");
        return;
      }

      // Loop over each file parameter request and create a file.
      for (RequestParameter file : files) {
        Node fileNode = createFile(session, store, file);
        fileNodes.add(fileNode);
      }

      // Create a link for each file if there is a need for it.
      if (linkParam != null) {
        Node linkFolder = (Node) session.getItem(linkParam.getString());
        // For each file .. create a link
        for (Node fileNode : fileNodes) {
          String fileName = fileNode.getProperty(FilesConstants.SAKAI_FILENAME)
              .getString();

          String linkPath = linkFolder.getPath() + "/" + fileName;
          FileUtils.createLink(session, fileNode, linkPath);
          links.add(linkPath);
        }
      }
      session.save();

      // Send a response back to the user.

      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.object();
      writer.key("files");
      writer.array();
      for (Node fileNode : fileNodes) {
        writer.object();
        writer.key("filename");
        writer.value(fileNode.getName());
        writer.key("path");
        writer.value(FileUtils.getDownloadPath(fileNode));
        writer.key("id");
        writer.value(fileNode.getProperty(FilesConstants.SAKAI_ID).getString());
        writer.endObject();
      }
      writer.endArray();
      if (links.size() > 0) {
        writer.key("links");
        writer.array();
        for (String link : links) {
          writer.value(link);
        }
        writer.endArray();
      }
      writer.endObject();

      // We send a 200 because SWFUpload has some problems dealing with other status
      // codes.
      response.setStatus(HttpServletResponse.SC_OK);

    } catch (RepositoryException e) {
      LOG.warn("Failed to create file.");
      e.printStackTrace();
      response.sendError(500, "Failed to save file.");
    } catch (JSONException e) {
      LOG.warn("Failed to write JSON format.");
      response.sendError(500, "Failed to write JSON format.");
      e.printStackTrace();
    }

  }

  /**
   * Creates a file under the store. Ex: store/aa/bb/cc/dd/myID
   * 
   * @param session
   * @param file
   * @param writer
   * @throws RepositoryException
   * @throws IOException
   * @throws JSONException
   */
  private Node createFile(Session session, String store, RequestParameter file)
      throws RepositoryException, IOException, JSONException {
    String contentType = file.getContentType();
    // Try to determine the real content type.
    // get content type
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
    String id = clusterTrackingService.getClusterUniqueId();
    if (id.endsWith("=="))
      id = id.substring(0, id.length() - 2);

    id = id.replace('/', '_');

    String path = FileUtils.getHashedPath(store, id);

    Node fileNode = FileUtils.saveFile(session, path, id, file, contentType,
        slingRepository);
    return fileNode;
  }

}
