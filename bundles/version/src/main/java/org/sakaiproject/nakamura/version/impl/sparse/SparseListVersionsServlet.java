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
package org.sakaiproject.nakamura.version.impl.sparse;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Gets a version
 */

@ServiceDocumentation(name = "List Versions Servlet", description = "Lists versions of a resource in json format", shortDescription = "List versions of a resource", bindings = @ServiceBinding(type = BindingType.TYPE, bindings = { "sling/servlet/default" }, selectors = @ServiceSelector(name = "versions", description = "Retrieves a paged list of versions for the resource"), extensions = @ServiceExtension(name = "json", description = "A list over versions in json format")), methods = @ServiceMethod(name = "GET", description = {
    "Lists previous versions of a resource. The url is of the form "
        + "http://host/resource.versions.json ",
    "Example<br>"
        + "<pre>curl http://localhost:8080/sresource/resource.versions.json</pre>" }, parameters = {
    @ServiceParameter(name = "items", description = "The number of items per page"),
    @ServiceParameter(name = "page", description = "The page to of items to return") }, response = {
    @ServiceResponse(code = 200, description = "Success a body is returned containing a json tree"),
    @ServiceResponse(code = 404, description = "Resource was not found."),
    @ServiceResponse(code = 500, description = "Failure with HTML explanation.") }))
@SlingServlet(resourceTypes = "sling/servlet/default", methods = "GET", selectors = "versions", extensions = "json")
public class SparseListVersionsServlet extends SlingSafeMethodsServlet implements OptingServlet {


  /**
   *
   */
  private static final String JSON_PATH = "path";
  /**
   *
   */
  private static final String JSON_ITEMS = "items";
  /**
   *
   */
  private static final String JSON_TOTAL = "total";
  /**
   *
   */
  private static final String JSON_VERSIONS = "versions";
  public static final String PARAMS_ITEMS_PER_PAGE = JSON_ITEMS;
  /**
  *
  */
  public static final String PARAMS_PAGE = "page";

  /**
   *
   */
  private static final long serialVersionUID = 764192946800357626L;
  private static final Logger LOGGER = LoggerFactory.getLogger(SparseListVersionsServlet.class);

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Resource resource = request.getResource();
    String path = null;
    
    try {
      Content content = resource.adaptTo(Content.class);
      ContentManager contentManager = resource.adaptTo(ContentManager.class);
      Session session = resource.adaptTo(Session.class);
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      if (content == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      path = content.getPath();
      int nitems = intRequestParameter(request, PARAMS_ITEMS_PER_PAGE, 25);
      int offset = intRequestParameter(request, PARAMS_PAGE, 0) * nitems;

      List<String> versionList = contentManager.getVersionHistory(path);
      int total = versionList.size();
      int start = Math.min(offset, total);
      int end = Math.min(start+nitems, total);
      nitems = end - start;

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      boolean tidy = false;
      String[] selectors = request.getRequestPathInfo().getSelectors();
      for (String selector : selectors) {
        if ("tidy".equals(selector)) {
          tidy = true;
          break;
        }
      }

      Writer writer = response.getWriter();
      ExtendedJSONWriter write = new ExtendedJSONWriter(writer);
      write.setTidy(tidy);
      write.object();
      write.key(JSON_PATH);
      write.value(path);
      write.key(JSON_ITEMS);
      write.value(nitems);
      write.key(JSON_TOTAL);
      write.value(total);
      write.key(JSON_VERSIONS);
      write.object();
      
      for (int j = start; j < end ; j++) {
        String versionId = versionList.get(j);
        write.key(versionId);
        write.object();
        Content vContent = contentManager.getVersion(path, versionId);
        writeEditorDetails(vContent, write, authorizableManager);
        ExtendedJSONWriter.writeNodeContentsToWriter(write, vContent);
        write.endObject();
      }
      write.endObject();
      write.endObject();
    } catch (JSONException e) {
      LOGGER.info("Failed to get version History ", e);
      response.reset();
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    } catch (StorageClientException e) {
      LOGGER.info("Failed to get version History ", e);
      response.reset();
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    } catch (AccessDeniedException e) {
      LOGGER.info("Failed to get version History ", e);
      response.reset();
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
  }

  private void writeEditorDetails(Content content, ExtendedJSONWriter write, AuthorizableManager authorizableManager)
      throws JSONException, AccessDeniedException, StorageClientException {
    String user = null;
    if (content.hasProperty(Content.VERSION_SAVEDBY)) {
      user = StorageClientUtils.toString(content.getProperty(Content.VERSION_SAVEDBY));
    }
    

    if (user != null) {
      org.sakaiproject.nakamura.api.lite.authorizable.Authorizable authorizable = authorizableManager.findAuthorizable(user);
      write.key(Content.VERSION_SAVEDBY);
      write.valueMap(authorizable.getSafeProperties());
    }
  }



  private int intRequestParameter(SlingHttpServletRequest request, String paramName,
      int defaultVal) throws ServletException {
    RequestParameter param = request.getRequestParameter(paramName);
    if (param != null) {
      try {
        return Integer.parseInt(param.getString());
      } catch (NumberFormatException e) {
        throw new ServletException("Invalid request, the value of param " + paramName
            + " is not a number " + e.getMessage());
      }
    }
    return defaultVal;
  }

  
  public boolean accepts(SlingHttpServletRequest request) {
    return (request.getResource() instanceof SparseContentResource);
  }

}
