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
package org.sakaiproject.nakamura.version.impl.sparse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.AbstractSafeMethodsServletResourceHandler;
import org.sakaiproject.nakamura.api.resource.SafeServletResourceHandler;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.version.impl.jcr.VersionRequestPathInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;


@Component(metatype=true, immediate=true)
@Service(value=SafeServletResourceHandler.class)
@Property(name="handling.servlet",value="GetVersionServlet")
public class SparseGetVersionServletHandler extends AbstractSafeMethodsServletResourceHandler {

  public static final Logger LOG = LoggerFactory.getLogger(SparseGetVersionServletHandler.class);
  /**
*
*/
  private static final long serialVersionUID = -4838347347796204151L;

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    RequestPathInfo requestPathInfo = request.getRequestPathInfo();

    // the version might be encapsulated in , at each end.
    String requestVersionName = VersionRequestPathInfo.getVersionName(
        requestPathInfo.getSelectorString(), requestPathInfo.getExtension());
    if (requestVersionName == null) {
      response
          .sendError(HttpServletResponse.SC_BAD_REQUEST,
              "No version specified, url should of the form nodepath.version.,versionnumber,.json");
      return;
    }    
    Resource resource = request.getResource();
    Content content = resource.adaptTo(Content.class);
    final ContentManager contentManager = resource.adaptTo(ContentManager.class);
    Content versionContentTemp;
    try {
      if ( requestVersionName.startsWith("1.")) {
        int versionNumber = Integer.parseInt(requestVersionName.substring(2));
        List<String> versionIds = contentManager.getVersionHistory(content.getPath());
        int i = versionIds.size()-1-versionNumber;
        if ( i < 0 || i >= versionIds.size()) {
          response
          .sendError(HttpServletResponse.SC_BAD_REQUEST,
              "No version specified, url should of the form nodepath.version.,versionnumber,.json");
          return;          
        }
        requestVersionName = versionIds.get(versionIds.size()-1-versionNumber);
      }
      versionContentTemp = contentManager.getVersion(content.getPath(), requestVersionName);
    } catch (StorageClientException e1) {
      LOG.warn(e1.getMessage(),e1);
      throw new ServletException(e1.getMessage(),e1);
    } catch (AccessDeniedException e1) {
      LOG.warn(e1.getMessage(),e1);
      throw new ServletException(e1.getMessage(),e1);
    }
    final String versionName = requestVersionName;
    final Content versionContent = versionContentTemp;
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
        if (type.equals(Content.class)) {
          return (AdapterType) versionContent;
        }
        if (type.equals(ValueMap.class) || type.equals(Map.class)) {
          ValueMap vm = new ValueMapDecorator(versionContent.getProperties());
          return (AdapterType) vm;
        }
        if (type.equals(InputStream.class)) {
          getResourceMetadata()
              .setContentLength(toLong(versionContent
                      .getProperty(Content.LENGTH_FIELD)));
          try {
            return (AdapterType) contentManager.getVersionInputStream(versionContent.getPath(), versionName);
          } catch (AccessDeniedException e) {
            LOG.warn(e.getMessage());
          } catch (StorageClientException e) {
            LOG.warn(e.getMessage());
          } catch (IOException e) {
            LOG.warn(e.getMessage());
          }
        }
        return super.adaptTo(type);
      }

      private long toLong(Object property) {
        if ( property instanceof Long) {
          return ((Long) property).longValue();
        }
        return 0;
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getPath()
       */
      @Override
      public String getPath() {
        return versionContent.getPath();
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getResourceType()
       */
      @Override
      public String getResourceType() {
        return (String) versionContent
            .getProperty("sling:resourceType");
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getResourceSuperType()
       */
      @Override
      public String getResourceSuperType() {
        return (String) versionContent
            .getProperty("sling:resourceSuperType");
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
    return (request.getResource() instanceof SparseContentResource);
  }

}
