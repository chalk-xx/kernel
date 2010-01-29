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

import static org.sakaiproject.kernel.api.files.FilesConstants.SAKAI_TAGS;
import static org.sakaiproject.kernel.api.files.FilesConstants.SAKAI_TAG_NAME;
import static org.sakaiproject.kernel.api.files.FilesConstants.SAKAI_TAG_UUIDS;
import static org.sakaiproject.kernel.api.files.FilesConstants.REQUIRED_MIXIN;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.util.JcrUtils;

import java.io.IOException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(generateService = true, generateComponent = true, resourceTypes = { "sling/servlet/default" }, methods = { "POST" }, selectors = { "tag" }, extensions = { "json" })
@Properties(value = {
    @Property(name = "service.description", value = "Set a tag on a node"),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class FileSetTagServlet extends SlingAllMethodsServlet {

  @Reference
  protected SlingRepository slingRepository;

  /**
   * 
   */
  private static final long serialVersionUID = -7724827744698056843L;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.servlets.post.impl.SlingPostServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node tagNode = null;
    Node node = request.getResource().adaptTo(Node.class);

    // Check if the uuid is in the request.
    RequestParameter uuidParam = request.getRequestParameter("uuid");
    if (uuidParam == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing uuid parameter");
    }

    // Grab the tagNode.
    String uuid = uuidParam.getString();
    try {
      tagNode = session.getNodeByUUID(uuid);
      if (!FileUtils.isTag(tagNode)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Provided UUID doesn't point to a tag.");
      }
    } catch (ItemNotFoundException e1) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not locate the tag.");
    } catch (RepositoryException e1) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not locate the tag.");
    }

    try {
      // Check if the user has the required minimum privilege.
      String user = request.getRemoteUser();
      if ("anon".equals(user)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }

      // We check if the node already has this tag.
      // If it does, we ignore it..
      if (!hasUuid(node, uuid)) {
        Session adminSession = null;
        try {
          adminSession = slingRepository.loginAdministrative(null);

          // Grab the node via the adminSession
          String path = node.getPath();
          node = (Node) adminSession.getItem(path);

          // Check if the mixin is on the node.
          // This is nescecary for nt:file nodes.
          if (!JcrUtils.hasMixin(node, REQUIRED_MIXIN)) {
            node.addMixin(REQUIRED_MIXIN);
          }

          // Add the reference from the tag to the node.
          String tagUuid = tagNode.getUUID();
          String tagName = tagNode.getName();
          if (tagNode.hasProperty(SAKAI_TAG_NAME)) {
            tagName = tagNode.getProperty(SAKAI_TAG_NAME).getString();
          }
          JcrUtils.addValue(adminSession, node, SAKAI_TAG_UUIDS, tagUuid,
              PropertyType.STRING);
          JcrUtils.addValue(adminSession, node, SAKAI_TAGS, tagName, PropertyType.STRING);

          if (adminSession.hasPendingChanges()) {
            adminSession.save();
          }

        } finally {
          adminSession.logout();
        }
      }

    } catch (RepositoryException e) {
      response.sendError(500, e.getMessage());
    }

  }

  /**
   * Checks if the node already has the uuid in it's properties.
   * 
   * @param node
   * @param uuid
   * @return
   * @throws RepositoryException
   */
  protected boolean hasUuid(Node node, String uuid) throws RepositoryException {
    Value[] uuids = JcrUtils.getValues(node, SAKAI_TAG_UUIDS);
    for (Value v : uuids) {
      if (v.getString().equals(uuid)) {
        return true;
      }
    }
    return false;
  }
}
