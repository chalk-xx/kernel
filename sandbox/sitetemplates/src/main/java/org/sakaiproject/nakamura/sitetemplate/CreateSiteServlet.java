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
package org.sakaiproject.nakamura.sitetemplate;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import static org.sakaiproject.nakamura.api.site.SiteService.PARAM_COPY_FROM;
import static org.sakaiproject.nakamura.api.site.SiteService.PARAM_MOVE_FROM;
import static org.sakaiproject.nakamura.api.site.SiteService.PARAM_SITE_PATH;
import static org.sakaiproject.nakamura.api.site.SiteService.SAKAI_IS_SITE_TEMPLATE;
import static org.sakaiproject.nakamura.api.site.SiteService.SAKAI_SITE_TEMPLATE;
import static org.sakaiproject.nakamura.api.site.SiteService.SITES_CONTAINER_RESOURCE_TYPE;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.sakaiproject.nakamura.util.StringUtils;
import org.sakaiproject.nakamura.version.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = { "POST" }, resourceTypes = { "sling/servlet/default",
    SiteService.SITES_CONTAINER_RESOURCE_TYPE }, selectors = { "template" }, generateComponent = true, generateService = true)
public class CreateSiteServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final String GROUPS_SITE_NODENAME = "groups";
  /**
   *
   */
  private static final long serialVersionUID = 6687687185254684084L;
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateSiteServlet.class);
  private static final String GROUPS_SITE_NODENAME_GROUP = "group";

  @Reference
  protected transient SiteService siteService;
  @Reference
  protected transient VersionService versionService;
  @Reference
  protected transient SlingRepository slingRepository;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    Node templateNode = null;
    JSONObject siteJSON = null;
    ResourceResolver resolver = request.getResourceResolver();
    Session session = resolver.adaptTo(Session.class);
    String currentUser = session.getUserID();

    String templatePath = null;
    String copyFromPath = null;
    String moveFromPath = null;

    // Do the necessary checks to see if this request is valid.
    if (UserConstants.ANON_USERID.equals(currentUser)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    // Check path values.
    String sitePath = getSitePath(request, response);
    if (sitePath == null) {
      return;
    }
    LOGGER.debug("The sitePath is: {}", sitePath);

    try {
      // If we base this site on a template, make sure it exists.
      RequestParameter siteTemplateParam = request
          .getRequestParameter(SAKAI_SITE_TEMPLATE);
      if (siteTemplateParam != null) {
        templatePath = siteTemplateParam.getString();
        if (!session.itemExists(templatePath)) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + SAKAI_SITE_TEMPLATE + " must be set to a site template");
          return;
        }
        // make sure it is a template site.
        templateNode = (Node) session.getItem(templatePath);
        if (!siteService.isSiteTemplate(templateNode)) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + SAKAI_SITE_TEMPLATE + " must be set to a site which has the "
              + SAKAI_IS_SITE_TEMPLATE + " set.");
          return;
        }
      }

      // Check what we will do (create, copy, move)
      // If we have been asked to move a site, make sure it exists.
      RequestParameter requestParameter = request.getRequestParameter(PARAM_MOVE_FROM);
      if (requestParameter != null) {
        moveFromPath = requestParameter.getString();
        if (!session.itemExists(moveFromPath)
            || !siteService.isSite(session.getItem(moveFromPath))) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + PARAM_MOVE_FROM + " must be set to an existing site.");
          return;
        }
      }
      // If we have been asked to copy a site, make sure it exists.
      requestParameter = request.getRequestParameter(PARAM_COPY_FROM);
      if (requestParameter != null) {
        copyFromPath = requestParameter.getString();
        if (!session.itemExists(copyFromPath)
            || !siteService.isSite(session.getItem(copyFromPath))) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + PARAM_COPY_FROM + " must be set to an existing site.");
          return;
        }
      }
      // We can only create a site in one way at a time.
      if (((templatePath != null) && ((moveFromPath != null) || (copyFromPath != null)))
          || ((moveFromPath != null) && (copyFromPath != null))) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Only one of "
            + SAKAI_SITE_TEMPLATE + ", " + PARAM_MOVE_FROM + ", and " + PARAM_COPY_FROM
            + " can be specified.");
        return;
      }

      RequestParameter siteParam = request.getRequestParameter("site");
      siteJSON = new JSONObject(siteParam.getString("UTF-8"));

    } catch (JSONException e) {
      LOGGER.error("Provided JSON object was invalid.", e);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The provided json was invalid.");
      return;
    } catch (RepositoryException e) {
      LOGGER.error("Caught a RepositoryException when trying to create a site.", e);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The path to the template is invalid.");
      return;
    }

    // Perform the actual creation or move.
    try {
      if (templatePath != null) {
        createSiteFromTemplate(session, sitePath, templateNode, siteJSON, resolver);
      } else if (copyFromPath != null) {
        copySite(session, copyFromPath, sitePath, currentUser);
      } else if (moveFromPath != null) {
        moveSite(session, moveFromPath, sitePath, currentUser);
      }

      // Save everything.
      if (session.hasPendingChanges()) {
        session.save();
      }

    } catch (Exception e) {
      LOGGER.error("Could not create site via templating engine.", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }
  }

  /**
   * Creates a site based on a template structure and a JSONObject provided by the UI.
   * 
   * @param session
   *          A session that allows us to create sites.
   * @param sitePath
   *          The path where the site should be created.
   * @param templateNode
   *          The node that represents the template structure.
   * @param siteJSON
   *          The JSON object that contains the data to be filled in at various points in
   *          the template node structure.
   * @param resolver
   *          A resource resolver to do queries against.
   * @return The site node.
   * @throws RepositoryException
   *           Failed to create the site.
   */
  private Node createSiteFromTemplate(Session session, String sitePath,
      Node templateNode, JSONObject siteJSON, ResourceResolver resolver)
      throws RepositoryException {
    TemplateBuilder builder = new TemplateBuilder(templateNode, siteJSON, resolver);

    Session adminSession = null;
    Node siteNode = null;
    try {
      // Login trough the admin session to create the site node and the groups.
      adminSession = slingRepository.loginAdministrative(null);

      // Create the sitenode.
      siteNode = JcrUtils.deepGetOrCreateNode(adminSession, sitePath);

      // Create the groups.
      createGroups(builder, adminSession, siteNode);

      // We get the site node trough our own session, so we can modify it.
      // siteNode = session.getNode(sitePath);
      // Create the site structure.
      createSiteStructure(builder, siteNode, adminSession);

      if (adminSession.hasPendingChanges()) {
        adminSession.save();
      }

      return siteNode;
    } finally {
      if (adminSession != null)
        adminSession.logout();
    }
  }

  /**
   * Creates the structure for the site.
   * 
   * @param builder
   *          The TemplateBuilder that contains the Map of nodes that needs to be created.
   * @param siteNode
   *          The Node that represents the site.
   * @param adminSession
   *          The admin session, this will only be used for setting ACLs.
   * @throws RepositoryException
   */
  private void createSiteStructure(TemplateBuilder builder, Node siteNode,
      Session adminSession) throws RepositoryException {
    Map<String, Object> structure = builder.getSiteMap();
    handleNode(structure, siteNode, adminSession);
  }

  /**
   * Converts a map into a node structure.
   * 
   * @param structure
   *          A Map that represents the node structure. The values can be of the following
   *          types: Value, Value[], Map<String, Object>
   * @param node
   *          The node where the map should be applied on.
   * @throws RepositoryException
   */
  @SuppressWarnings("unchecked")
  private void handleNode(Map<String, Object> structure, Node node, Session adminSession)
      throws RepositoryException {
    Session session = node.getSession();
    // Handle the ACEs before we do anything else.
    if (structure.containsKey("rep:policy")) {
      List<ACE> lst = (List<ACE>) structure.get("rep:policy");
      for (ACE ace : lst) {
        AccessControlUtil.replaceAccessControlEntry(adminSession, node.getPath(), ace
            .getPrincipal(), ace.getGrantedPrivileges(), ace.getDeniedPrivileges(), null,
            null);
      }
      // We have to do a save for the ACEs to get picked up.
      if (adminSession.hasPendingChanges()) {
        adminSession.save();
      }
      session.refresh(true);
    }
    if (structure.containsKey(JCR_MIXINTYPES)) {
      Value[] mixins = (Value[]) structure.get(JCR_MIXINTYPES);
      for (Value v : mixins) {
        String mixin = v.getString();
        if (node.canAddMixin(mixin)) {
          node.addMixin(v.getString());
        }
      }
    }

    for (Entry<String, Object> entry : structure.entrySet()) {

      String key = entry.getKey();
      if (key.equals(JCR_PRIMARYTYPE) || key.equals(JCR_MIXINTYPES)) {
        continue;
      }

      // Handle the properties ..
      if (entry.getValue() instanceof Value) {
        node.setProperty(key, (Value) entry.getValue());
      } else if (entry.getValue() instanceof Value[]) {
        node.setProperty(key, (Value[]) entry.getValue());
      }

      // Handle the child nodes.
      else if (entry.getValue() instanceof Map<?, ?>) {
        Map<String, Object> map = (Map<String, Object>) entry.getValue();
        String nt = "nt:unstructured";
        if (map.containsKey(JcrConstants.JCR_PRIMARYTYPE)) {
          nt = ((Value) map.get(JCR_PRIMARYTYPE)).getString();
        }
        Node childNode = node.addNode(key, nt);
        handleNode(map, childNode, adminSession);
      }
    }
  }

  /**
   * Creates the groups in the system.
   * 
   * @param builder
   * @param session
   * @throws RepositoryException
   */
  @SuppressWarnings("unchecked")
  private void createGroups(TemplateBuilder builder, Session session, Node siteNode)
      throws RepositoryException {

    Node groupNodes = null;
    if (siteNode.hasNode(GROUPS_SITE_NODENAME)) {
      groupNodes = siteNode.getNode(GROUPS_SITE_NODENAME);
    } else {
      groupNodes = siteNode.addNode(GROUPS_SITE_NODENAME);
    }

    UserManager um = AccessControlUtil.getUserManager(session);
    Map<Principal, Map<String, Object>> groups = builder.getGroups();
    int i = 0;
    for (Entry<Principal, Map<String, Object>> g : groups.entrySet()) {
      // Maybe the group already exists.
      // If it does, we use that one.
      Group group = (Group) um.getAuthorizable(g.getKey());

      // Create the authorizable.
      if (group == null) {
        group = um.createGroup(g.getKey());
      }

      // Set any additional properties
      Map<String, Object> map = g.getValue();
      for (Entry<String, Object> entry : map.entrySet()) {
        if (entry.getValue() instanceof Value) {
          group.setProperty(entry.getKey(), (Value) entry.getValue());
        } else if (entry.getValue() instanceof Value[]) {
          group.setProperty(entry.getKey(), (Value[]) entry.getValue());
        } else if (entry.getValue() instanceof List<?>) {
          // The member list
          List<String> members = (List<String>) entry.getValue();
          for (String m : members) {
            Authorizable authorizable = um.getAuthorizable(m);
            group.addMember(authorizable);
          }
        }
      }

      // add the group node to the site groupNode.
      boolean isMaintainer = false;
      if (map.containsKey(TemplateBuilder.GROUPS_PROPERTY_IS_MAINTAINER)) {
        Value v = (Value) map.get(TemplateBuilder.GROUPS_PROPERTY_IS_MAINTAINER);
        isMaintainer = v.getBoolean();
      }

      Node groupNode = null;
      if (groupNodes.hasNode(GROUPS_SITE_NODENAME_GROUP + i)) {
        groupNode = groupNodes.getNode(GROUPS_SITE_NODENAME_GROUP + i);
      } else {
        groupNode = groupNodes.addNode(GROUPS_SITE_NODENAME_GROUP + i);
      }

      groupNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, "sakai/site-group");
      groupNode.setProperty(TemplateBuilder.GROUPS_PROPERTY_IS_MAINTAINER, isMaintainer);
      i++;
    }
  }

  /**
   * Copy a site over.
   * 
   * @param session
   * @param fromPath
   * @param sitePath
   * @param creator
   * @return
   * @throws RepositoryException
   */
  private Node copySite(Session session, String fromPath, String sitePath, String creator)
      throws RepositoryException {
    ensureParent(session, sitePath);

    // Copy the template files in the new folder.
    LOGGER.debug("Copying site ({}) to new dir ({})", fromPath, sitePath);
    Workspace workspace = session.getWorkspace();
    workspace.copy(fromPath, sitePath);
    Node siteNode = (Node) session.getItem(sitePath);
    session.save();

    if (session.hasPendingChanges()) {
      session.save();
    }
    versionNodeAndChildren(siteNode, creator);
    LOGGER.debug("Finished copying");
    return siteNode;
  }

  /**
   * Moves a site. When the move is done it will version the site node.
   * 
   * @param session
   *          Session to use to do the move.
   * @param fromPath
   *          The path to the site that needs to be moved.
   * @param sitePath
   *          The path to where the site should be moved
   * @param creator
   *          The Authorizable that does the versioning -- the current user.
   * @return The new site node.
   * @throws RepositoryException
   */
  private Node moveSite(Session session, String fromPath, String sitePath, String creator)
      throws RepositoryException {
    ensureParent(session, sitePath);

    // Copy the template files in the new folder.
    LOGGER.debug("Moving site ({}) to new dir ({})", fromPath, sitePath);
    Workspace workspace = session.getWorkspace();
    workspace.move(fromPath, sitePath);
    Node siteNode = (Node) session.getItem(sitePath);

    versionService.saveNode(siteNode, creator);
    LOGGER.debug("Finished move");
    return siteNode;
  }

  /**
   * Workspace copy/move needs the destination's parent to exist and be saved. The save is
   * necessary to do a Workspace.move or .copy .
   * 
   * @param session
   *          The session that can be used to create the parent node.
   * @param sitePath
   *          The path where the site should be created. This method will create the path
   *          to the parent.
   * @throws RepositoryException
   */
  private void ensureParent(Session session, String sitePath) throws RepositoryException {
    String parentPath = PathUtils.getParentReference(sitePath);
    JcrUtils.deepGetOrCreateNode(session, parentPath);
    if (session.hasPendingChanges()) {
      session.save();
    }
  }

  /**
   * Versions a node and all its child nodes.
   * 
   * @param n
   *          The node (and it's subtree) that needs to be versioned.
   * @param userID
   *          The userID that does the versioning -- current user.
   * @param createSession
   *          Session to use for versioning.
   */
  private void versionNodeAndChildren(Node n, String userID) {
    try {

      Session session = n.getSession();
      // TODO do better check
      if (n.isNode()
          && !n.getName().startsWith("rep:")
          && !n.getName().startsWith("jcr:")
          && n.hasProperties()
          && !n.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString().equals(
              JcrConstants.NT_RESOURCE)) {
        versionService.saveNode((Node) session.getItem(n.getPath()), userID);
        NodeIterator it = n.getNodes();
        // Version the childnodes
        while (it.hasNext()) {
          Node childNode = it.nextNode();
          versionNodeAndChildren(childNode, userID);
        }
      }
    } catch (RepositoryException re) {
      LOGGER.warn("Unable to save copied node", re);
    }
  }

  /**
   * Parse the request to get the destination of the new or moved site.
   * 
   * @param request
   *          The request that contains the parameters for this create action.
   * @param response
   *          The response to write to if something was malformed or incorrect.
   * @return null if an error needs to be returned to the user
   * @throws IOException
   */
  private String getSitePath(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws IOException {
    String resourceType = request.getResource().getResourceType();
    String sitePath = request.getRequestPathInfo().getResourcePath();
    // If the current target URL is a parent node for sites, construct the final
    // site path from it and the ":sitepath" parameter.
    if (SITES_CONTAINER_RESOURCE_TYPE.equals(resourceType)) {
      RequestParameter relativePathParam = request.getRequestParameter(PARAM_SITE_PATH);
      if (relativePathParam == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
            + PARAM_SITE_PATH + " must be set to a relative path ");
        return null;
      }
      String relativePath = relativePathParam.getString();
      if (StringUtils.isEmpty(relativePath)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
            + PARAM_SITE_PATH + " must be set to a relative path ");
        return null;
      }
      if (sitePath.startsWith("/")) {
        sitePath = sitePath + relativePath;
      } else {
        sitePath = sitePath + "/" + relativePath;
      }
    }
    return sitePath;
  }

}
