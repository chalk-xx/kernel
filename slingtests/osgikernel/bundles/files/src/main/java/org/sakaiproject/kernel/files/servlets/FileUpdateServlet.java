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

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceParameter;
import org.sakaiproject.kernel.api.doc.ServiceResponse;
import org.sakaiproject.kernel.api.doc.ServiceSelector;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.api.files.FilesConstants;
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
 * Update a file
 * 
 */
@SlingServlet(resourceTypes={"sakai/file"}, methods={"POST"}, selectors={"update"})
@Properties(value = {
    @Property(name = "service.description", value = "Servlet to allow to update a file."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
@ServiceDocumentation(
    name = "FilesUpdateServlet", 
    shortDescription = "Update an already existing file in the repository.",
    bindings = @ServiceBinding(
        type = BindingType.TYPE,
        selectors = @ServiceSelector(name="update", description = "Update a file in the repository."),
        bindings = "sakai/files"
    ),
    methods = {@ServiceMethod(
        name = "POST", 
        description = "Update a file in the repository.",
        parameters = {
            @ServiceParameter(
                name="Filedata", 
                description="Required: the parameter that holds the actual data for the file that should be uploaded."),
            @ServiceParameter(
                name="link", 
                description="Optional: absolute path where you want to create a link for the file. This can be multivalued. It has to be the same size as the site parameter though."),
            @ServiceParameter(
                name="site", 
                description="Optional: absolute path to a site that should be associated with this file. This can be multivalued. It has to be the same size as the link parameter though.")
        },
        response = {
            @ServiceResponse(
                code = 200, 
                description = "Everything went OK and the file was updated. All links and properties were created/set."),
            @ServiceResponse(
                code = 400,
                description = "Filedata parameter was not provided."
            ),
            @ServiceResponse(
                code = 500,
                description = "Failure with HTML explanation."
            )
        }
    )}
)
public class FileUpdateServlet extends SlingAllMethodsServlet {

  public static final Logger LOGGER = LoggerFactory.getLogger(FileUpdateServlet.class);
  private static final long serialVersionUID = -625686874623971605L;

  @Reference
  private transient SlingRepository slingRepository;


  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    try {
      RequestParameter file = request.getRequestParameter("Filedata");
      if (file == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Missing Filedata parameter.");
        return;
      }

      String[] links = request.getParameterValues("link");
      String[] sites = request.getParameterValues("site");
      if (sites.length != links.length) {
        response
            .sendError(HttpServletResponse.SC_BAD_REQUEST,
                "The site parameter's length doesn't match with the link's parameter length.");
        return;
      }

      Session session = request.getResourceResolver().adaptTo(Session.class);
      LOGGER.info("Trying to update file for: " + session.getUserID());
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

      Node fileNode = FileUtils.saveFile(session, path, id, file, contentType,
          slingRepository);
      String fileName = fileNode.getProperty(FilesConstants.SAKAI_FILENAME).getString();

      List<String> createdLinks = Lists.newArrayList();
      for (int i = 0; i < links.length; i++) {
        String linkPath = links[i];
        if (!linkPath.endsWith("/"))
          linkPath += "/";
        linkPath += fileName;
        FileUtils.createLink(session, node, linkPath, sites[i], slingRepository);
        createdLinks.add(linkPath);
      }

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
