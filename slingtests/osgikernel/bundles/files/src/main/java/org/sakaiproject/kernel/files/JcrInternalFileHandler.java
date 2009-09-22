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
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.sakaiproject.kernel.api.files.FileHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

/**
 * Handles files that are linked to a jcrinternal resource.
 * 
 * @scr.component immediate="true" label="JcrInternalFileHandler"
 *                description="Handles files that are linked to a jcrinternal resource."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.files.handler" value="jcrinternal"
 * @scr.service interface="org.sakaiproject.kernel.api.files.FileHandler"
 */
public class JcrInternalFileHandler implements FileHandler {

  public static final Logger LOGGER = LoggerFactory
      .getLogger(JcrInternalFileHandler.class);

  public void handleFile(SlingHttpServletRequest request,
      SlingHttpServletResponse response, String to) throws ServletException, IOException {
    // Get the filenode.
    Resource baseResource = request.getResource();
    String filename = null;
    Node node = (Node) baseResource.adaptTo(Node.class);
    try {
      filename = node.getName();
    } catch (RepositoryException e) {
      response.sendError(500, "Unable to download file.");
      return;
    }

    if (filename != null && !response.containsHeader("Content-Disposition")) {
      response.setHeader("Content-Disposition", "attachment; filename=\"" + filename
          + "\"");
    }

    LOGGER.info("Pointing request {} to the real file {}", baseResource.getPath(), to);

    // Send request to download the file.
    baseResource.getResourceMetadata().setResolutionPath("");
    baseResource.getResourceMetadata().setResolutionPathInfo(to);

    final String finalPath = to;
    final ResourceMetadata rm = baseResource.getResourceMetadata();

    // Wrap the request so it points to the message we just created.
    ResourceWrapper wrapper = new ResourceWrapper(request.getResource()) {
      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getPath()
       */
      @Override
      public String getPath() {
        return finalPath;
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getResourceType()
       */
      @Override
      public String getResourceType() {
        return "sling/servlet/default";
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getResourceMetadata()
       */
      @Override
      public ResourceMetadata getResourceMetadata() {
        return rm;
      }

    };

    RequestDispatcherOptions options = new RequestDispatcherOptions();
    options.setForceResourceType("sling:File");
    SlingHttpServletResponseWrapper wrappedResponse = new SlingHttpServletResponseWrapper(
        response) {
      ServletOutputStream servletOutputStream = new ServletOutputStream() {

        @Override
        public void write(int b) throws IOException {
        }
      };
      PrintWriter pw = new PrintWriter(servletOutputStream);

      /**
       * {@inheritDoc}
       * 
       * @see javax.servlet.ServletResponseWrapper#flushBuffer()
       */
      @Override
      public void flushBuffer() throws IOException {
      }

      /**
       * {@inheritDoc}
       * 
       * @see javax.servlet.ServletResponseWrapper#getOutputStream()
       */
      @Override
      public ServletOutputStream getOutputStream() throws IOException {
        return servletOutputStream;
      }

      /**
       * {@inheritDoc}
       * 
       * @see javax.servlet.ServletResponseWrapper#getWriter()
       */
      @Override
      public PrintWriter getWriter() throws IOException {
        return pw;
      }
    };
    options.setReplaceSelectors("");
    request.getRequestDispatcher(wrapper, options).forward(request, wrappedResponse);
    if (!response.isCommitted()) {
      response.reset();
    }
  }
}