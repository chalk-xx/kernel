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
package org.sakaiproject.kernel.files.servlets;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceResponse;
import org.sakaiproject.kernel.api.doc.ServiceSelector;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.files.search.FileSearchBatchResultProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

/**
 * Dumps all the files of a sakai/folder.
 * 
 */
@SlingServlet(resourceTypes = { "sakai/folder" }, methods = { "GET" }, selectors = { "files" })
@Properties(value = {
    @Property(name = "service.description", value = "Links nodes to files."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
@ServiceDocumentation(
    name = "FolderServlet",
    shortDescription = "Pretty print child items of a sakai/folder",
    description = "Dumps all the sakai/link, sakai/file and any other file under a sakai/folder node." +
    		"This is the same output as a search result.",
    bindings = @ServiceBinding(
        type = BindingType.TYPE,
        bindings = "sakai/folder",
        selectors = @ServiceSelector(
            name = "files", 
            description = "Get all files underneath this folder."
        )
    ), 
    methods = @ServiceMethod(
        name = "GET", 
        response = {
            @ServiceResponse(
                code = 200,
                description = "Success, a body is returned."
            ),
            @ServiceResponse(
              code = 500,
              description = "Failure, explanation is in the HTML."
            )
        }
    )
) 
public class FolderServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 2602398397981186645L;
  private static final Logger LOGGER = LoggerFactory.getLogger(FolderServlet.class);

  @Reference
  private transient SiteService siteService;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    Node node = (Node) request.getResource().adaptTo(Node.class);
    String path = "";

    // Get all the children of this node.
    try {

      if (LOGGER.isDebugEnabled()) {
        path = node.getPath();
      }
      JSONWriter write = new JSONWriter(response.getWriter());
      NodeIterator it = node.getNodes();

      FileSearchBatchResultProcessor processor = new FileSearchBatchResultProcessor(siteService);
      write.array();

      // FIXME Doing iterator.getSize isn't performant but I don't see any
      // other way of getting the nr of children. And since we strive not to have
      // > 255 childNodes this should still be in reasonable limit
      processor.writeNodes(request, write, it, 0, it.getSize());

      write.endArray();

    } catch (RepositoryException e) {
      LOGGER.warn("Unable to list all the file/folders for {}", path);
      e.printStackTrace();
      response.sendError(500, "Unable to list all the file/folders");
    } catch (JSONException e) {
      LOGGER.warn("Unable to list all the file/folders for {}", path);
      e.printStackTrace();
      response.sendError(500, "Unable to list all the file/folders");
    }

  }
}
