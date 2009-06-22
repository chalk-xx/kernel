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
package org.sakaiproject.kernel.resource;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 */
public abstract class AbstractVirtualPathServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = -7892996718559864951L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractVirtualPathServlet.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.message.AbstractMessageServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    LOGGER.info("Processing {}", request.getRequestURI());
    hashRequest(request, response);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doDelete(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doDelete(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    LOGGER.info("Processing {}", request.getRequestURI());
    hashRequest(request, response);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    LOGGER.info("Personal Servlet Processing {}", request.getRequestURI());
    hashRequest(request, response);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPut(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPut(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    LOGGER.info("Processing {}", request.getRequestURI());
    hashRequest(request, response);
  }

  public void hashRequest(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws IOException, ServletException {
    /*
     * Process the path to expand , then dispatch to the resource at that
     * location.
     */
    Resource baseResource = request.getResource();
    LOGGER.debug("Went into virtual servlet with {}", baseResource);

    Session session = request.getResourceResolver().adaptTo(Session.class);
    String uriPath = baseResource.getPath();

    // find the first real node in the jcr
    Node firstNode = null;
    try {
      firstNode = getFirstNode(session, baseResource.getPath());
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    if (firstNode == null) {
      response
          .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Unable to find parent node of resource, even the repository base is missing");
      return;
    }
    String realPath = null;
    try {
      realPath = firstNode.getPath();
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Unable to get the path the first node " + e.getMessage());
    }
    String pathInfo = uriPath.substring(realPath.length());

    LOGGER.debug(pathInfo);

    if (pathInfo.length() == 0 || "/".equals(pathInfo)) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND,
          "Resource does not exist");
      return;
    }

    String virtualPath = pathInfo.substring(1);

    final String resourcePath = getTargetPath(baseResource, request, response,
        realPath, virtualPath);

    LOGGER.info("Path is {} ", resourcePath);

    Resource resource = request.getResourceResolver().resolve(resourcePath);
    if (resource == null || resource instanceof NonExistingResource) {
      // we need to use dispatch a wrapped resource to the default servlet
      if ("GET|OPTIONS|HEAD".indexOf(request.getMethod()) >= 0) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      } else {
        Resource wrapper = new VirtualResource(baseResource, resourcePath);
        if (preDispatch(request, response, baseResource, wrapper)) {
          request.getRequestDispatcher(wrapper).forward(request, response);
          postDispatch(request, response, baseResource, resource);
        }
      }
    } else {
      if (preDispatch(request, response, baseResource, resource)) {
        // otherwise we can just dispatch the resource we found as is.
        request.getRequestDispatcher(resource).forward(request, response);
        postDispatch(request, response, baseResource, resource);
      }
    }

  }

  /**
   * Override this if you want to perform some actions before the request is
   * dispatched to sling and control whether it should be dispatched to sling or
   * not
   * 
   * @param request
   *          the current sling request
   * @param response
   *          the current sling response
   * @param baseResource
   *          the resource which represents the base of the request
   * @param resource
   *          the current sling resource from the target path
   * @return true if the request should be dispatched to sling OR false if the
   *         response should continue as is
   */
  protected boolean preDispatch(SlingHttpServletRequest request,
      SlingHttpServletResponse response, Resource baseResource,
      Resource resource) {
    return true;
  }

  /**
   * Override this if you want to perform some actions after the request returns
   * from sling, note that this only will be called if the preDispatch returned
   * true
   * 
   * @param request
   *          the current sling request
   * @param response
   *          the current sling response
   * @param baseResource
   *          the resource which represents the base of the request
   * @param resource
   *          the current sling resource from the target path
   */
  protected void postDispatch(SlingHttpServletRequest request,
      SlingHttpServletResponse response, Resource baseResource,
      Resource resource) {
  }

  /**
   * Get the final path,
   * 
   * @param baseResource
   *          the resource which represents the base of the request
   * @param request
   *          the request
   * @param response
   *          the current sling response
   * @param realPath
   *          the part of the path that does exist within the jcr.
   * @param virtualPath
   *          the part of the path that does not exist and is virtual.
   * @return A JCR path that where the resoruce is stored.
   */
  protected abstract String getTargetPath(Resource baseResource,
      SlingHttpServletRequest request, SlingHttpServletResponse response,
      String realPath, String virtualPath);

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
