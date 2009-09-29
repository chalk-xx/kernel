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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.sakaiproject.kernel.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

/**
 * Stream the file.
 * 
 * @scr.component metatype="no" immediate="true" label="FileServlet"
 *                description="Stream the file"
 * @scr.property name="service.description" value="Stream the file."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/file"
 * @scr.property name="sling.servlet.methods" values.0="GET"
 */
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
    InputStream in = (InputStream) request.getResource().adaptTo(InputStream.class);
    OutputStream out = response.getOutputStream();

    IOUtils.stream(in, out);
  }
}
