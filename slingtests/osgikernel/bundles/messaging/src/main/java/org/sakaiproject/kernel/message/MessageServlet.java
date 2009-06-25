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
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.message.chat.ChatMessageCleaner;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sakai/messagestore"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.reference interface="org.apache.sling.jcr.api.SlingRepository"
 *                name="SlingRepository" bind="bindSlingRepository"
 *                unbind="unbindSlingRepository" policy="dynamic"
 */
public class MessageServlet extends AbstractMessageServlet {


  private static final Logger LOGGER = LoggerFactory.getLogger(MessageServlet.class);

  /**
   *
   */
  private static final long serialVersionUID = -2663916166760531044L;

  private SlingRepository slingRepository;  
  
  private Timer chatCleanUpTimer = null;
  
  
  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }
  
  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }
  
  /**
   * @param componentContext
   */
  protected void activate(ComponentContext componentContext) {
    // Start the timer that will delete this message.
    chatCleanUpTimer = new Timer();
    chatCleanUpTimer.schedule(new ChatMessageCleaner(slingRepository), 15 * 1000, MessageConstants.CLEAUNUP_EVERY_X_MINUTES * 1000 * 60);
    
    LOGGER.info("Started the chats cleanup timer.");
  }
  
  protected void deactivate(ComponentContext componentContext) {
    if (chatCleanUpTimer != null) {
      chatCleanUpTimer.cancel();
    }
  }
  
  
  
  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.message.AbstractMessageServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  protected void hashRequest(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    try {
      /*
       * The type will be sakai/messagestore, other wise the request would not be here,
       * however the resource has not be processed. We need to reprocess the path to find
       * the first existing node, and then calculate the path from that point on, to
       * generate the virtual path. Once we have done that we need to re-resolve the path
       * and re-dispatch.
       */
      Resource baseResource = request.getResource();
      Session session = request.getResourceResolver().adaptTo(Session.class);
      String uriPath = baseResource.getPath();
      Node firstNode = getFirstNode(session, baseResource.getPath());
      if (firstNode == null) {
        response
            .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unable to find parent node of resource, even the repository base is missing");
        return;
      }
      String path = firstNode.getPath();
      String pathInfo = uriPath.substring(path.length());

      System.out.println(pathInfo);
 
      if (pathInfo.length() == 0 || "/".equals(pathInfo)) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource does not exist");
        return;
      }

      String relativePath = pathInfo.substring(1);
      String[] parts = PathUtils.getNodePathParts(relativePath);
      String resourcePath = PathUtils.toInternalHashedPath(path, parts[0], parts[1]);
      
      System.out.println("ResourcePath = " + resourcePath);
      
      Resource resource = request.getResourceResolver().resolve(path);
      if ( resource instanceof NonExistingResource ) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND,"Resource does not exist (non existant)");
        return;
      }
      if ( resource == null ) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND,"Resource does not exist (null)");
        return;     
      }
      System.out.println("Dispatch request from MessageServlet");
      request.getRequestDispatcher(resource).forward(request, response);
      
      
    } catch (RepositoryException ex) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
    }

  }

  /**
   * @throws RepositoryException
   * 
   */
  private Node getFirstNode(Session session, String absRealPath)
      throws RepositoryException {
    Item item = null;
    try {
      item = session.getItem(absRealPath);
    } catch (PathNotFoundException ex) {
    }
    String parentPath = absRealPath;
    while (item == null && !"/".equals(parentPath)) {
      parentPath = PathUtils.getParentReference(parentPath);
      try {
        item = session.getItem(parentPath);
      } catch (PathNotFoundException ex) {
      }
    }
    if (item == null) {
      return null;
    }
    // convert first item to a node.
    if (!item.isNode()) {
      item = item.getParent();
    }

    return (Node) item;
  }
}
