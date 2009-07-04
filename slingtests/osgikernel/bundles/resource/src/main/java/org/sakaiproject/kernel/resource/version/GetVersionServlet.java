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
package org.sakaiproject.kernel.resource.version;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.sakaiproject.kernel.util.StringUtils;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Outputs a version
 * 
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sling/servlet/default"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.selectors" value="version"
 * 
 * 
 */
public class GetVersionServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = -4838347347796204151L;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    RequestPathInfo requestPathInfo = request.getRequestPathInfo();
    String suffix = requestPathInfo.getSuffix();

    String[] suffixParts = StringUtils.split(suffix, '.');

    Resource resource = request.getResource();
    Node node = resource.adaptTo(Node.class);
    Version versionNode = null;
    try {
      versionNode = node.getVersionHistory().getVersion(suffixParts[0]);
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
    Node vnode = null;
    try {
      vnode = versionNode.getNode(JcrConstants.JCR_FROZENNODE);
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
    final Node finalNode = vnode;
    final VersionRequestPathInfo versionRequestPathInfo = new VersionRequestPathInfo(
        requestPathInfo);
    ResourceWrapper resourceWrapper = new ResourceWrapper(resource) {
      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#adaptTo(java.lang.Class)
       */
      @SuppressWarnings("unchecked")
      @Override
      public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type.equals(Node.class)) {
          return (AdapterType) finalNode;
        }
        return super.adaptTo(type);
      }

    };

    SlingHttpServletRequestWrapper requestWrapper = new SlingHttpServletRequestWrapper(
        request) {
      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper#getRequestPathInfo()
       */
      @Override
      public RequestPathInfo getRequestPathInfo() {
        return versionRequestPathInfo;
      }

    };
    request.getRequestDispatcher(resourceWrapper).forward(requestWrapper, response);
    
  }

}
