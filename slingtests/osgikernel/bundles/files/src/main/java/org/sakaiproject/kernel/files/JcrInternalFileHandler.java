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
package org.sakaiproject.kernel.files;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.api.files.FilesConstants;
import org.sakaiproject.kernel.api.files.LinkHandler;
import org.sakaiproject.kernel.util.IOUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

/**
 * Handles files that are linked to a jcrinternal resource.
 * 
 * @scr.component immediate="true" label="JcrInternalFileHandler"
 *                description="Handles files that are linked to a jcrinternal resource."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.files.handler" value="jcrinternal"
 * @scr.service interface="org.sakaiproject.kernel.api.files.LinkHandler"
 */
public class JcrInternalFileHandler implements LinkHandler {

  public static final Logger LOGGER = LoggerFactory
      .getLogger(JcrInternalFileHandler.class);

  public void handleFile(SlingHttpServletRequest request,
      SlingHttpServletResponse response, String to) throws ServletException, IOException {
    // Get file id.
    String id = to.substring(to.lastIndexOf('/') + 1, to.length());
    String storePath = to.substring(0, to.lastIndexOf('/'));

    // get actual file
    String realPath = storePath + PathUtils.getHashedPath(id, 4);
    Resource fileResource = request.getResourceResolver().getResource(realPath);

    // Check filename.
    String filename = null;
    Node linkNode = (Node) request.getResource().adaptTo(Node.class);
    try {
      if (linkNode.hasProperty(FilesConstants.SAKAI_FILENAME)) {
        filename = linkNode.getProperty(FilesConstants.SAKAI_FILENAME).getString();
      } else {
        filename = linkNode.getName();
      }
    } catch (RepositoryException e) {
      LOGGER.warn("Unable to read filename for {}", to);
      response.sendError(500, "Unable to read filename.");
      return;
    }

    // If we provided a filename and we haven't changed the name in a previous request.
    if (filename != null && !response.containsHeader("Content-Disposition")) {
      response.setHeader("Content-Disposition", "sfilename=\"" + filename
          + "\"");
    }
    
    // Write out the file.
    InputStream in = (InputStream) fileResource.adaptTo(InputStream.class);
    OutputStream out = response.getOutputStream();
    IOUtils.stream(in, out);
  }
}