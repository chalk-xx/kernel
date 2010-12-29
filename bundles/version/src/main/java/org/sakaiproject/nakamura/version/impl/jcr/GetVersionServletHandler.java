/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.version.impl.jcr;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.jcr.resource.JcrModifiablePropertyMap;
import org.apache.sling.jcr.resource.JcrPropertyMap;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.nakamura.api.resource.AbstractSafeMethodsServletResourceHandler;
import org.sakaiproject.nakamura.api.resource.SafeServletResourceHandler;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.NodeInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Outputs a version
 */
@Component(metatype=true, immediate=true)
@Service(value=SafeServletResourceHandler.class)
@Property(name="handling.servlet",value="GetVersionServlet")
public class GetVersionServletHandler extends AbstractSafeMethodsServletResourceHandler {

  public static final Logger LOG = LoggerFactory.getLogger(GetVersionServletHandler.class);
  /**
*
*/
  private static final long serialVersionUID = -4838347347796204151L;

  /**
   * {@inheritDoc}
   * 
   */
  public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    RequestPathInfo requestPathInfo = request.getRequestPathInfo();

    // the version might be encapsulated in , at each end.
    String versionName = VersionRequestPathInfo.getVersionName(requestPathInfo.getSelectorString(), requestPathInfo.getExtension());
    if (versionName == null) {
      response
          .sendError(HttpServletResponse.SC_BAD_REQUEST,
              "No version specified, url should of the form nodepath.version.,versionnumber,.json");
      return;
    }
    Resource resource = request.getResource();
    Node node = resource.adaptTo(Node.class);
    Version versionNode = null;
    try {
      VersionManager versionManager = node.getSession().getWorkspace()
          .getVersionManager();
      versionNode = versionManager.getVersionHistory(node.getPath()).getVersion(
          versionName);
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
    Node vnode = null;
    String vpath = null;
    String vresourceType = null;
    String vresourceSuperType = null;
    try {
      vnode = versionNode.getNode(JcrConstants.JCR_FROZENNODE);
      vpath = vnode.getPath();
      if (vnode.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
        vresourceType = vnode.getProperty(
            JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString();
      } else {
        vresourceType = vnode.getPrimaryNodeType().getName();
      }
      if (vnode.hasProperty(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY)) {
        vresourceSuperType = vnode.getProperty(
            JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY).getString();
      }
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }

    final Node finalNode = vnode;
    final String path = vpath;
    final String resourceType = vresourceType;
    final String resourceSuperType = vresourceSuperType;
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
        LOG.debug("Adapting to:{} ", type);
        if (type.equals(Node.class)) {
          return (AdapterType) finalNode;
        }
        if (type.equals(ValueMap.class) || type.equals(Map.class)) {
          try {
            if (finalNode.hasProperty(JcrConstants.JCR_FROZENPRIMARYTYPE)
                && finalNode.getProperty(JcrConstants.JCR_FROZENPRIMARYTYPE).getString()
                    .equals(JcrConstants.NT_FILE)) {
              NodeInputStream stream = JcrUtils.getInputStreamForNode(finalNode.getNode("jcr:content"));
              InputStream is = stream.getInputStream();
              // Convert stream to string
              StringBuilder sb = new StringBuilder();
              InputStreamReader inr = new InputStreamReader(is);
              BufferedReader reader = new BufferedReader(inr);
              try {
                String line = reader.readLine();
                while (line != null) {
                  sb.append(line +"\n");
                  line = reader.readLine();
                }
              } catch (IOException e) {
                e.printStackTrace();
              }
              finally {
                try {
                  reader.close();
                } catch (IOException e) {
                  LOG.debug(e.getMessage(),e);
                }
                try {
                  inr.close();
                } catch (IOException e) {
                  LOG.debug(e.getMessage(),e);
                }
                try {
                  is.close();
                } catch (IOException e) {
                  LOG.debug(e.getMessage(),e);
                }
              }
              JcrModifiablePropertyMap map = new JcrModifiablePropertyMap(finalNode);
              map.put("data", sb.toString());
              return (AdapterType) map;
            } else {
              return (AdapterType) new JcrPropertyMap(finalNode);
            }
          } catch (RepositoryException e) {
            return (AdapterType) new JcrPropertyMap(finalNode);
          }
        }
        if (type.equals(InputStream.class)) {
          Node node = finalNode;
          try {
            if ( finalNode.hasNode(JcrConstants.JCR_CONTENT)) {
              node = finalNode.getNode(JcrConstants.JCR_CONTENT);
            }
          } catch (RepositoryException e) {
            LOG.warn("Failed to convert to a content node: {}",e.getMessage());
          }


          NodeInputStream stream = JcrUtils.getInputStreamForNode(node);
          if ( stream != null ) {
            ResourceMetadata resourceMetadata = getResourceMetadata();
            long length = stream.getLength();
            resourceMetadata.setContentLength(length);
            return (AdapterType) stream.getInputStream();
          }
        }
        return super.adaptTo(type);
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getPath()
       */
      @Override
      public String getPath() {
        return path;
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getResourceType()
       */
      @Override
      public String getResourceType() {
        return resourceType;
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getResourceSuperType()
       */
      @Override
      public String getResourceSuperType() {
        return resourceSuperType;
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


  public boolean accepts(SlingHttpServletRequest request) {
    return !(request.getResource() instanceof SparseContentResource);
  }

}
