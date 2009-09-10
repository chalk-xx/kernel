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
package org.sakaiproject.kernel.version;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Gets a version
 * 
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sling/servlet/default"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.selectors" value="versions"
 * @scr.property name="sling.servlet.extensions" value="json"
 * 
 * 
 */
public class ListVersionsServlet extends SlingAllMethodsServlet {

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
  private static final Logger LOGGER = LoggerFactory.getLogger(ListVersionsServlet.class);

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Resource resource = request.getResource();
    String path = null;
    try {
      Node node = resource.adaptTo(Node.class);
      if (node == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      path = node.getPath();
      int nitems = intRequestParameter(request, PARAMS_ITEMS_PER_PAGE, 25);
      int offset = intRequestParameter(request, PARAMS_PAGE, 0) * nitems;
      
      VersionHistory versionHistory = node.getVersionHistory();
      VersionIterator versionIterator = versionHistory.getAllVersions();
      
      long total = versionIterator.getSize();
      long[] range = getInvertedRange(total,offset,nitems);
      nitems = (int)(range[1] - range[0]);
      Version[] versions = new Version[nitems];
      versionIterator.skip(range[0]);
      
      int i = 0;
      while (i < nitems && versionIterator.hasNext()) {
        versions[i++] = versionIterator.nextVersion();
      }   

      Writer writer = response.getWriter();
      ExtendedJSONWriter write = new ExtendedJSONWriter(writer);
      write.object();
      write.key(JSON_PATH);
      write.value(node.getPath());
      write.key(JSON_ITEMS);
      write.value(nitems);
      write.key(JSON_TOTAL);
      write.value(total);
      write.key(JSON_VERSIONS);
      write.object();
      versionIterator.skip(offset);
      for ( int j = versions.length-1;  j >=0; j-- ) {
        write.key(versions[j].getName());
        write.object();
        Node vnode = versions[j].getNode(JcrConstants.JCR_FROZENNODE);
        if (!writeEditorDetails(vnode, SaveVersionServlet.SAVED_BY, SaveVersionServlet.SAVED_BY, write)) {
          writeEditorDetails(vnode, "jcr:createdBy", SaveVersionServlet.SAVED_BY, write);
        }
        ExtendedJSONWriter.writeNodeContentsToWriter(write, versions[j]);
        write.endObject();
      }
      write.endObject();
      write.endObject();
    } catch (UnsupportedRepositoryOperationException e) {
      Writer writer = response.getWriter();
      try {
        JSONWriter write = new JSONWriter(writer);
        write.object();
        write.key(JSON_PATH);
        write.value(path);
        write.key(JSON_ITEMS);
        write.value(0);
        write.key(JSON_TOTAL);
        write.value(0);
        write.key("warning");
        write.value("The resource requested is not versionable, "
            + "either becuase the repository does not support it "
            + "or becuase the Resource is not versionable");
        write.key(JSON_VERSIONS);
        write.object();
        write.endObject();
        write.endObject();
      } catch (JSONException e1) {
        response.reset();
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
        return;
      }

    } catch (RepositoryException e) {
      response.reset();
      LOGGER.info("Failed to get version History ", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    } catch (JSONException e) {
      LOGGER.info("Failed to get version History ", e);
      response.reset();
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
  }

  private boolean writeEditorDetails(Node node, String propertySource, String outputName, ExtendedJSONWriter write) {
    try {
      PersonalUtils.writeUserInfo(node, write, propertySource, outputName);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * @param total
   * @param offset
   * @param nitems
   * @return
   */
  protected long[] getInvertedRange(long total, int offset, int nitems) {
    long[] range = new long[2];
    if ( total < 0 || nitems < 0 ) {
      range[0] = 0;
      range[1] = 0;
      return range;
    }
    if ( offset < 0 ) {
      offset = 0;
    }
    range[1] = total-offset;
    range[0] = 0;
    if ( range[1] < 0 ) {
      range[1] = 0;
      range[0] = 0;
    } else {
      range[0] = range[1] - nitems;
      if ( range[0] < 0 ) {
        range[0] = 0;
      }
    }
    return range;
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

}
