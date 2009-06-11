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
package org.sakaiproject.kernel.message;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

public class MessagingServiceImpl implements MessagingService {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(MessagingServiceImpl.class);

  /**
   * 
   * {@inheritDoc}
   * @throws IOException 
   * @throws ServletException 
   * @see org.sakaiproject.kernel.api.message.MessagingService#create(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
   */
  public void create(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    request.setAttribute(MessageConstants.MESSAGE_OPERATION, request
        .getMethod());

    LOGGER.info("ServletPath " + request.getPathInfo());

    Resource baseResource = request.getResource();

    final ResourceMetadata rm = baseResource.getResourceMetadata();
    String pathInfo = String.valueOf(Thread.currentThread().getId())
        + String.valueOf(System.currentTimeMillis());
    try {
      pathInfo = org.sakaiproject.kernel.util.StringUtils.sha1Hash(pathInfo);
    } catch (Exception ex) {

    }
    String servletPath = rm.getResolutionPath();

    String[] pathParts = PathUtils.getNodePathParts(pathInfo);

    final String finalPath = PathUtils.toInternalHashedPath(servletPath,
        pathParts[0], pathParts[1]);
    final ResourceMetadata resourceMetadata = new ResourceMetadata();
    resourceMetadata.putAll(rm);
    resourceMetadata.setResolutionPath("/");
    resourceMetadata.setResolutionPathInfo(finalPath);

    LOGGER.info("ServletPath:{}\nPathInfo:{}\nFinalPath:{}", new Object[] {
        servletPath, pathInfo, finalPath });

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
    LOGGER
        .info("Sending the request out again in CreateMessageServlet with attribute: "
            + request.getAttribute(MessageConstants.MESSAGE_OPERATION));
    request.getRequestDispatcher(wrapper, options).forward(request,
        wrappedResponse);
    response.reset();
    response.sendRedirect(rm.getResolutionPath() + "/" + pathInfo);

  }

}
