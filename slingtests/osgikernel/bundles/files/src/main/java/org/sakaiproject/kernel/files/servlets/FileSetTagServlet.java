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

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.util.JcrUtils;

import java.io.IOException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
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
   * The mixin required on the node to do a tag.
   */
  protected String REQUIRED_MIXIN = "sakai:propertiesmix";
  /**
   * The required privilege a user has to have to tag a node.
   */
  protected String REQUIRED_PRIVILEGE = "jcr:read";

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.servlets.post.impl.SlingPostServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws IOException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node tagNode = null;
    Node node = request.getResource().adaptTo(Node.class);

    // Check if the uuid is in the request.
    RequestParameter uuidParam = request.getRequestParameter("uuid");
    if (uuidParam == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Missing uuid parameter");
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
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Could not locate the tag.");
    } catch (RepositoryException e1) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Could not locate the tag.");
    }

    try {
      // Check if the user has the required minimum privilege.
      if (!hasRequiredAccess(session, node)) {
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
          if (!hasRequiredMixin(session, node)) {
            node.addMixin(REQUIRED_MIXIN);
          }

          // Add the reference to the tag to the node.
          Value[] uuids = getUpdatedUuids(adminSession, node, tagNode);
          Value[] tagNames = getUpdatedTags(adminSession, node, tagNode);

          node.setProperty(SAKAI_TAG_UUIDS, uuids);
          node.setProperty(SAKAI_TAGS, tagNames);

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

  /**
   * Get the current tag names on the node and add's the name of the tag to the list.
   * 
   * @param adminSession
   * @param node
   * @param tagNode
   * @return
   * @throws RepositoryException
   */
  private Value[] getUpdatedTags(Session adminSession, Node node, Node tagNode)
      throws RepositoryException {

    // Grab the name of the tag.
    String tagName = tagNode.getName();
    if (tagNode.hasProperty(SAKAI_TAG_NAME)) {
      tagName = tagNode.getProperty(SAKAI_TAG_NAME).getString();
    }

    Value[] tags = JcrUtils.getValues(node, SAKAI_TAGS);
    Value[] newTags = new Value[tags.length + 1];
    for (int i = 0; i < tags.length; i++) {
      Value v = tags[i];
      newTags[i] = v;
    }

    // Create a value that points to the tag node.
    newTags[tags.length] = adminSession.getValueFactory().createValue(tagName);
    return newTags;
  }

  /**
   * @param adminSession
   * @param node
   * @param tagNode
   * @return
   * @throws RepositoryException
   */
  protected Value[] getUpdatedUuids(Session adminSession, Node node,
      Node tagNode) throws RepositoryException {
    Value[] uuids = JcrUtils.getValues(node, SAKAI_TAG_UUIDS);
    Value[] newUuids = new Value[uuids.length + 1];
    for (int i = 0; i < uuids.length; i++) {
      Value v = uuids[i];
      newUuids[i] = v;
    }

    // Create a value that points to the tag node.
    newUuids[uuids.length] = adminSession.getValueFactory()
        .createValue(tagNode);
    return newUuids;
  }

  /**
   * Checks if a user has the minimum access.
   * 
   * @param session
   * @param node
   * @return
   * @throws RepositoryException
   */
  protected boolean hasRequiredAccess(Session session, Node node)
      throws RepositoryException {
    // Check if the user has the required READ access.
    String path = node.getPath();
    AccessControlManager acm = AccessControlUtil
        .getAccessControlManager(session);
    Privilege p = acm.privilegeFromName(REQUIRED_PRIVILEGE);
    Privilege[] privileges = { p };

    return acm.hasPrivileges(path, privileges);

  }

  /**
   * Checks if the node can have properties on it.
   * 
   * @param session
   * @param node
   *          The node in question
   */
  protected boolean hasRequiredMixin(Session session, Node node)
      throws RepositoryException {
    NodeType[] nodetypes = node.getMixinNodeTypes();
    boolean hasRequiredMixin = false;
    for (NodeType type : nodetypes) {
      if (type.getName().equals(REQUIRED_MIXIN)) {
        hasRequiredMixin = true;
        break;
      }
    }

    return hasRequiredMixin;
  }
}
