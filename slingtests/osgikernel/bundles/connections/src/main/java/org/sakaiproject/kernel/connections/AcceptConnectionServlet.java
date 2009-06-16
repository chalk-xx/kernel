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
package org.sakaiproject.kernel.connections;

import static org.sakaiproject.kernel.api.connections.ConnectionConstants.CONNECTION_OPERATION;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.sakaiproject.kernel.util.PathUtils;
import org.sakaiproject.kernel.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;

/**
 * Accepts a connection request.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sakai/connectionstore"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="accept"
 */
public class AcceptConnectionServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 3813877071190736742L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(AcceptConnectionServlet.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.message.AbstractMessageServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request,
      org.apache.sling.api.SlingHttpServletResponse response)
      throws javax.servlet.ServletException, java.io.IOException {
    request.setAttribute(CONNECTION_OPERATION, request.getMethod());

    LOGGER.info("ServletPath " + request.getPathInfo());

    Resource baseResource = request.getResource();

    final ResourceMetadata rm = baseResource.getResourceMetadata();
    String pathInfo = String.valueOf(Thread.currentThread().getId())
        + String.valueOf(System.currentTimeMillis());
    try {
      pathInfo = StringUtils.sha1Hash(pathInfo);
    } catch (Exception ex) {

    }
    String servletPath = rm.getResolutionPath();
    
    String[] pathParts = PathUtils.getNodePathParts(pathInfo);

    final String finalPath = PathUtils.toInternalHashedPath(servletPath, pathParts[0], pathParts[1]);
    final ResourceMetadata resourceMetadata = new ResourceMetadata();
    resourceMetadata.putAll(rm);
    resourceMetadata.setResolutionPath("/");
    resourceMetadata.setResolutionPathInfo(finalPath);

    LOGGER.info("ServletPath:{}\nPathInfo:{}\nFinalPath:{}", new Object[] {servletPath,
        pathInfo, finalPath});

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
    response.reset();
    response.sendRedirect(rm.getResolutionPath() + "/" + pathInfo);
  }

}
