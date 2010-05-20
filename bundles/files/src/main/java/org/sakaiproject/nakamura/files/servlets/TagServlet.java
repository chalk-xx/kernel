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
package org.sakaiproject.nakamura.files.servlets;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.files.FileUtils;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.files.search.FileSearchBatchResultProcessor;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.io.IOException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(extensions = { "json" }, generateComponent = true, generateService = true, methods = { "GET" }, resourceTypes = { "sakai/tag" }, selectors = {
    "children", "parents", "tagged", "createtag" })
@Properties(value = {
    @Property(name = "service.description", value = "Provides support for file tagging."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class TagServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -8815248520601921760L;

  @Reference
  protected transient SiteService siteService;

  private FileSearchBatchResultProcessor proc;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    String selector = request.getRequestPathInfo().getSelectorString();
    JSONWriter write = new JSONWriter(response.getWriter());
    Node tag = request.getResource().adaptTo(Node.class);
    try {
      if ("children".equals(selector)) {
        sendChildren(tag, write);
      } else if ("parents".equals(selector)) {
        sendParents(tag, write);
      } else if ("tagged".equals(selector)) {
        sendFiles(tag, request, write);
      }
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (SearchException e) {
      response.sendError(e.getCode(), e.getMessage());
    }

  }

  /**
   * @param tag
   * @param request
   * @throws RepositoryException
   * @throws JSONException
   * @throws SearchException
   */
  protected void sendFiles(Node tag, SlingHttpServletRequest request, JSONWriter write)
      throws RepositoryException, JSONException, SearchException {
    // We expect tags to be referencable, if this tag is not..
    // it will throw an exception.
    String uuid = tag.getIdentifier();

    // Tagging on any item will be performed by adding a weak reference to the content
    // item. Put simply a sakai:tag-uuid property with the UUID of the tag node. We use
    // the UUID to uniquely identify the tag in question, a string of the tag name is not
    // sufficient. This allows the tag to be renamed and moved without breaking the
    // relationship.
    String statement = "//*[@sakai:tag-uuid='" + uuid + "']";
    Session session = tag.getSession();
    QueryManager qm = session.getWorkspace().getQueryManager();
    Query query = qm.createQuery(statement, Query.XPATH);

    // For good measurement
    if (proc == null) {
      proc = new FileSearchBatchResultProcessor(siteService);
    }

    SearchResultSet rs = proc.getSearchResultSet(request, query);
    write.array();
    proc.writeNodes(request, write, null, rs.getRowIterator());
    write.endArray();
  }

  /**
   * Write all the parent tags of the passed in tag.
   * 
   * @param tag
   *          The tag that should be sent and get it's children parsed.
   * @param write
   *          The JSONWriter to write to
   * @throws JSONException
   * @throws RepositoryException
   */
  protected void sendParents(Node tag, JSONWriter write) throws JSONException,
      RepositoryException {
    write.object();
    write.key("jcr:name");
    write.value(tag.getName());
    write.key("jcr:path");
    write.value(tag.getPath());
    ExtendedJSONWriter.writeNodeContentsToWriter(write, tag);
    write.key("parent");
    try {
      Node parent = tag.getParent();
      if (FileUtils.isTag(parent)) {
        sendParents(parent, write);
      } else {
        write.value(false);
      }
    } catch (ItemNotFoundException e) {
      write.value(false);
    }

    write.endObject();
  }

  /**
   * Write all the child tags of the passed in tag.
   * 
   * @param tag
   *          The tag that should be sent and get it's children parsed.
   * @param write
   *          The JSONWriter to write to
   * @throws JSONException
   * @throws RepositoryException
   */
  protected void sendChildren(Node tag, JSONWriter write) throws JSONException,
      RepositoryException {

    write.object();
    write.key("jcr:name");
    write.value(tag.getName());
    write.key("jcr:path");
    write.value(tag.getPath());
    ExtendedJSONWriter.writeNodeContentsToWriter(write, tag);
    write.key("children");
    write.array();
    NodeIterator iterator = tag.getNodes();
    while (iterator.hasNext()) {
      Node node = iterator.nextNode();
      if (FileUtils.isTag(node)) {
        sendChildren(node, write);
      }
    }
    write.endArray();
    write.endObject();

  }
}
