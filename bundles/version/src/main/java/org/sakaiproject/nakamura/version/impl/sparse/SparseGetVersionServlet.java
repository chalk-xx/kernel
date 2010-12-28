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

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.version.impl.VersionRequestPathInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Outputs a version
 */
@SlingServlet(resourceTypes = "sling/servlet/default", methods = "GET", selectors = "version")
@ServiceDocumentation(name = "Get Version Servlet", description = "Gets a previous version of a resource", shortDescription = "Get a version of a resource", bindings = @ServiceBinding(type = BindingType.TYPE, bindings = { "sling/servlet/default" }, selectors = @ServiceSelector(name = "version", description = "Retrieves a named version of a resource, the version specified in the URL"), extensions = @ServiceExtension(name = "*", description = "All selectors availalble in SLing (jcon, html, xml)")), methods = @ServiceMethod(name = "GET", description = {
    "Gets a previous version of a resource. The url is of the form "
        + "http://host/resource.version.,versionnumber,.json "
        + " where versionnumber is the version number of version to be retrieved. Note that the , "
        + "at the start and end of versionnumber"
        + " delimit the version number. Once the version of the node requested has been extracted the request "
        + " is processed as for other Sling requests ",
    "Example<br>"
        + "<pre>curl http://localhost:8080/sresource/resource.version.,1.1,.json</pre>" }, response = {
    @ServiceResponse(code = 200, description = "Success a body is returned"),
    @ServiceResponse(code = 400, description = "If the version name is not known."),
    @ServiceResponse(code = 404, description = "Resource was not found."),
    @ServiceResponse(code = 500, description = "Failure with HTML explanation.") }

))
public class SparseGetVersionServlet extends SlingSafeMethodsServlet implements
    OptingServlet {

  public static final Logger LOG = LoggerFactory.getLogger(SparseGetVersionServlet.class);
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

    // the version might be encapsulated in , at each end.
    final String versionName = VersionRequestPathInfo.getVersionName(
        requestPathInfo.getSelectorString(), requestPathInfo.getExtension());
    if (versionName == null) {
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
      versionContentTemp = contentManager.getVersion(content.getPath(), versionName);
    } catch (StorageClientException e1) {
      LOG.warn(e1.getMessage(),e1);
      throw new ServletException(e1.getMessage(),e1);
    } catch (AccessDeniedException e1) {
      LOG.warn(e1.getMessage(),e1);
      throw new ServletException(e1.getMessage(),e1);
    }
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
          // TODO: convert this to a version map correctly.
          return (AdapterType) versionContent.getProperties();
        }
        if (type.equals(InputStream.class)) {
          getResourceMetadata()
              .setContentLength(
                  StorageClientUtils.toLong(versionContent
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
        return StorageClientUtils.toString(versionContent
            .getProperty("sling:resourceType"));
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getResourceSuperType()
       */
      @Override
      public String getResourceSuperType() {
        return StorageClientUtils.toString(versionContent
            .getProperty("sling:resourceSuperType"));
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
