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
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceResponse;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.sakaiproject.kernel.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Stream the file.
 * 
 */
@SlingServlet(resourceTypes = "sakai/file", methods = { "GET" })
@Properties(value = {
    @Property(name = "service.description", value = "Stream the file."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
@ServiceDocumentation(
   name = "FileServlet",
   shortDescription = "Streams a file too the browser",
   description = "Streams a file too the browser.",
   bindings = @ServiceBinding(
       type = BindingType.TYPE,
       bindings = "sakai/file"
   )
)
public class FileServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -6591047521699263996L;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    Resource resource = request.getResource();
    Node node = (Node) resource.adaptTo(Node.class);

    String filename = null;
    try {
      if (node.hasNode(JcrConstants.JCR_CONTENT)) {
        Node content = node.getNode(JcrConstants.JCR_CONTENT);
        response.setHeader("Content-Type", content.getProperty(JcrConstants.JCR_MIMETYPE)
            .getString());
        response.setHeader("Content-Length", ""
            + content.getProperty(JcrConstants.JCR_DATA).getLength());
      }
      if (node.hasProperty(FilesConstants.SAKAI_FILENAME)) {
        filename = node.getProperty(FilesConstants.SAKAI_FILENAME).getString();
      }
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // If we provided a filename and we haven't changed the name in a previous request.
    if (filename != null && !response.containsHeader("Content-Disposition")) {
      response.setHeader("Content-Disposition", "filename=\"" + filename + "\"");
    }

    response.setStatus(HttpServletResponse.SC_OK);
    InputStream in = (InputStream) request.getResource().adaptTo(InputStream.class);
    OutputStream out = response.getOutputStream();

    IOUtils.stream(in, out);
  }
}
