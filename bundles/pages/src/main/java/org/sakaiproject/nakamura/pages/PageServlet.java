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
package org.sakaiproject.nakamura.pages;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "PageServlet", shortDescription = "Lists the messages of the current users mailboxes.", description = "Presents mailbox messages of current user in JSON format.", bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/system/messages"), methods = @ServiceMethod(name = "GET", description = "List current user's mailbox messages.", response = {
    @ServiceResponse(code = 200, description = "Request for information was successful. <br />"),
    @ServiceResponse(code = 401, description = "Unauthorized: credentials provided were not acceptable to return information for."),
    @ServiceResponse(code = 500, description = "Unable to return information about current user.") }))
@SlingServlet(paths = { "/var/search/page" }, generateComponent = true, generateService = true, methods = { "GET" })
public class PageServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -3786472219389695181L;
  private static final Logger LOG = LoggerFactory.getLogger(PageServlet.class);

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      List<Content> contentList = null;
      RequestParameter rp = request.getRequestParameter("path");
      ResourceResolver resourceResolver = request.getResourceResolver();
      if (rp != null) {
        String contentPath = rp.getString("UTF-8");
        if (contentPath.startsWith("/_groupa:")) {
          contentPath = contentPath.replaceFirst("/_groupa:", "/~");
        }
        if (contentPath.endsWith("/")) {
          contentPath = contentPath.substring(0, contentPath.length() - 1);
        }

        Resource pagesResource = resourceResolver.getResource(contentPath);
        if (pagesResource != null) {
          contentList = getPageTree(pagesResource.adaptTo(Content.class));
          ;
        }

      }

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      PrintWriter w = response.getWriter();
      ExtendedJSONWriter writer = new ExtendedJSONWriter(w);
      writer.object();
      // pages info
      int messageCount = 0;
      writer.key("items");
      writer.value(255);

      writer.key("results");
      writer.array();
      if (contentList != null) {
        for (int i = 0; i < contentList.size(); i++) {
          Content page = contentList.get(i);

          writer.object();
          writer.key("jcr:path");
          writer.value(page.getPath().replaceFirst(LitePersonalUtils.PATH_AUTHORIZABLE,
              LitePersonalUtils.PATH_RESOURCE_AUTHORIZABLE));
          for (String messagePropKey : page.getProperties().keySet()) {
            writer.key(messagePropKey);
            writer.value(massageValue(messagePropKey, page.getProperty(messagePropKey)));
          }
          writer.endObject();
          messageCount++;
        }
      }
      writer.endArray();
      writer.key("total");
      writer.value(messageCount);

      writer.endObject();
    } catch (JSONException e) {
      LOG.error("Failed to create proper JSON response in /var/search/page", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to create proper JSON response.");
    }

  }

  private Object massageValue(String messagePropKey, Object property) {
    Object rv = property;
    if (property instanceof java.util.Calendar) {
      rv = ((java.util.Calendar)property).getTimeInMillis();
    }
    if ("pagePosition".equals(messagePropKey) && property instanceof String) {
      rv = Long.valueOf((String)property);
    }
    return rv;
  }

  /*
   * getPageTree: returns List of all pages under the passed path
   */
  private List<Content> getPageTree(Content pageContent) {
    List<Content> contentList = new ArrayList<Content>();

    // Add to list only if content is a page
    String resourceType = (String) pageContent.getProperty("sling:resourceType");
    if (resourceType != null && resourceType.equals("sakai/page")) {
      contentList.add(pageContent);
    }

    if (pageContent != null) {
      for (Content page : pageContent.listChildren()) {
        contentList.addAll(getPageTree(page));
      }
    }

    return contentList;
  }


}