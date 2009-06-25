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
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessagingException;
import org.sakaiproject.kernel.api.message.MessagingService;
import org.sakaiproject.kernel.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * Will create a message under the user's _private folder. If the box is set to
 * outbox en the pending property to pending or none it will be picked up by the
 * MessagePostProcessor who will then send an OSGi event that feeds it to the
 * correct MessageHandler.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sakai/messagestore"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="create"
 * @scr.reference name="MessagingService"
 *                interface="org.sakaiproject.kernel.api.message.MessagingService"
 *                bind="bindMessagingService" unbind="unbindMessagingService"
 */
public class CreateMessageServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 3813877071190736742L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(CreateMessageServlet.class);

  private MessagingService messagingService;

  protected void bindMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  protected void unbindMessagingService(MessagingService messagingService) {
    this.messagingService = null;
  }


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
    LOGGER.info("Creating message.");

    request.setAttribute(MessageConstants.MESSAGE_OPERATION, request
        .getMethod());

    LOGGER.info("ServletPath " + request.getPathInfo());

    // This is the message store resource.
    Resource baseResource = request.getResource();

    String user = request.getRemoteUser();
    if (user == null || UserConstants.ANON_USERID.equals(request.getRemoteUser()) ) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "Anonymous users can't send messages.");
      return;
    }

    // Do some small checks before we actually write anything.
    if (request.getRequestParameter(MessageConstants.PROP_SAKAI_TYPE) == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "No type for this message specified.");
      return;
    }
    if (request.getRequestParameter(MessageConstants.PROP_SAKAI_TO) == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "No recipient specified.");
      return;
    }

    RequestParameterMap mapRequest = request.getRequestParameterMap();
    Map<String, Object> mapProperties = new HashMap<String, Object>();
    Iterator<String> it = mapRequest.keySet().iterator();

    while (it.hasNext()) {
      String k = it.next();
      mapProperties.put(k, mapRequest.get(k).toString());
    }
    mapProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
        MessageConstants.SAKAI_MESSAGE_RT);
    mapProperties.put(MessageConstants.PROP_SAKAI_READ, true);
    mapProperties.put(MessageConstants.PROP_SAKAI_FROM, user);

    // Create the message.
    Node msg = null;
    String path = null;
    try {
      msg = messagingService.create(baseResource, mapProperties);
      if (msg == null) {
        throw new MessagingException("Unable to create the message.");
      }
      path = msg.getPath();
    } catch (MessagingException e) {
      LOGGER.warn("MessagingException: " + e.getMessage());
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getMessage());
      return;
    } catch (RepositoryException e) {
      LOGGER.warn("RepositoryException: " + e.getMessage());
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getMessage());
      return;
    }

    baseResource.getResourceMetadata().setResolutionPath("/");
    baseResource.getResourceMetadata().setResolutionPathInfo(path);

    final String finalPath = path;
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
    LOGGER.info("Sending the request out again with attribute: "
        + request.getAttribute(MessageConstants.MESSAGE_OPERATION));
    request.getRequestDispatcher(wrapper, options).forward(request,
        wrappedResponse);
    response.reset();
    response.sendRedirect(finalPath);
  }
}
