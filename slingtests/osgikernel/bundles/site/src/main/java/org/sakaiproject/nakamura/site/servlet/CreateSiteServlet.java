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
package org.sakaiproject.nakamura.site.servlet;

import static org.sakaiproject.nakamura.api.site.SiteService.SAKAI_SITE_TEMPLATE;
import static org.sakaiproject.nakamura.util.ACLUtils.ADD_CHILD_NODES_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.MODIFY_ACL_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.MODIFY_PROPERTIES_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.READ_ACL_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.READ_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.REMOVE_CHILD_NODES_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.NODE_TYPE_MANAGEMENT_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.REMOVE_NODE_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.VERSION_MANAGEMENT_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.WRITE_GRANTED;
import static org.sakaiproject.nakamura.util.ACLUtils.addEntry;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.site.SiteAuthz;
import org.sakaiproject.nakamura.site.SiteServiceImpl;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.sakaiproject.nakamura.util.StringUtils;
import org.sakaiproject.nakamura.version.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.client.HessianProxyFactory;

import edu.nyu.XythosRemote;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>CreateSiteServlet</code> creates new sites. . /site/container.createsite
 * /site/container/site.createsite If the node is of type of sakai/sites, then create the
 * site based on a request property If the note is not of type sakai/sites, and exists
 * make it a sakai/site
 * 
 * @scr.component immediate="true" label="CreateSiteServlet"
 *                description="Create site servlet"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description" value=
 *               "Supports creation of sites, either from existing folders, or new folders."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sling/servlet/default"
 *               values.1="sakai/sites"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="createsite"
 * @scr.reference name="SlingRepository"
 *                interface="org.apache.sling.jcr.api.SlingRepository"
 */
@ServiceDocumentation(name = "Create a Site", description = "The <code>CreateSiteServlet</code> creates new sites. . /site/container.createsite "
    + "/site/container/site.createsite If the node is of type of sakai/sites, then create the "
    + "site based on a request property. If the node is not of type sakai/sites, and exists make it a sakai/site", shortDescription = "Create a new Site", bindings = @ServiceBinding(type = BindingType.TYPE, bindings = {
    "sling/servlet/default", "sakai/sites" }, selectors = @ServiceSelector(name = "createsite", description = "Create Site"), extensions = @ServiceExtension(name = "html", description = "A standard HTML response for creating a node.")), methods = @ServiceMethod(name = "POST", description = {
    "Creates a site, with a name specified in :sitepath from an optional template. In the process the servlet"
        + "will also create all related structures (message stores etc) and set up any groups associated with the site. "
        + "Create permissions may be controlled by the sakai:sitegroupcreate property, containing a list of principals allowed"
        + "to create sites under that node. If the current user is not allowed to create a site in the chosen location, then"
        + "a 403 is returned. "
        + "Any parameters other than :sitepath will be stored as properties on the new site node.",
    "Example<br>" + "<pre>Example needed</pre>" }, parameters = {
    @ServiceParameter(name = ":sitepath", description = "The Path to the site being created (required)"),
    @ServiceParameter(name = SAKAI_SITE_TEMPLATE, description = "Path to a template node in JCR to use when creating the site (optional)")

}, response = {
    @ServiceResponse(code = 200, description = "Success a body is returned containing a json ove the name of the version saved"),
    @ServiceResponse(code = 400, description = {
        "If the :sitepath parameter is not present",
        "If the " + SAKAI_SITE_TEMPLATE
            + " parameter does not point to a template in JCR" }),
    @ServiceResponse(code = 403, description = "Current user is not allowed to create a site in the current location."),
    @ServiceResponse(code = 404, description = "Resource was not found."),
    @ServiceResponse(code = 500, description = "Failure with HTML explanation.") }))
public class CreateSiteServlet extends AbstractSiteServlet {

  private static final long serialVersionUID = -7996020354919244147L;

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateSiteServlet.class);

  private static final String SITE_CREATE_PRIVILEGE = "sakai:sitegroupcreate";

  private transient SlingRepository slingRepository;
  
  /**
   * @scr.property name="xythosHost"
   *               description="The remote host (and port) of the Xythos instance"
   *               value="http://localhost:9090"
   */
  protected String xythosHost = "http://localhost:9090";
  
  protected String remotePath = "/remoting/remoting/XythosService";

  /** @scr.reference */
  private transient VersionService versionService;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable currentUser = userManager.getAuthorizable(request.getRemoteUser());

      String resourceType = request.getResource().getResourceType();
      String sitePath = request.getRequestPathInfo().getResourcePath();
      String templatePath = null;

      // If the current target URL is a parent node for sites, construct the final
      // site path from it and the ":sitepath" parameter.
      if ("sakai/sites".equals(resourceType)) {
        RequestParameter relativePathParam = request.getRequestParameter(":sitepath");
        if (relativePathParam == null) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + ":sitepath" + " must be set to a relative path ");
          return;
        }
        String relativePath = relativePathParam.getString();
        if (StringUtils.isEmpty(relativePath)) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + ":sitepath" + " must be set to a relative path ");
          return;
        }

        if (sitePath.startsWith("/")) {
          sitePath = sitePath + relativePath;
        } else {
          sitePath = sitePath + "/" + relativePath;
        }
      }

      // If we base this site on a template, make sure it exists.
      RequestParameter siteTemplate = request
          .getRequestParameter(SiteService.SAKAI_SITE_TEMPLATE);
      if (siteTemplate != null) {
        templatePath = siteTemplate.getString();
        if (!session.itemExists(templatePath)) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + SiteService.SAKAI_SITE_TEMPLATE + " must be set to a site template");
          return;
        }
        // make sure it is a template site.
        if (!getSiteService().isSiteTemplate(session.getItem(templatePath))) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + SiteService.SAKAI_SITE_TEMPLATE + " must be set to a site which has the "
              + SiteService.SAKAI_IS_SITE_TEMPLATE + " set.");
          return;
        }
      }
      LOGGER.debug("The sitePath is: {}", sitePath);

      boolean granted = isCreateSiteGranted(session, sitePath, currentUser);
      Session createSession = session;
      if (granted) {
        // Switch to gain administrative powers for at least long enough
        // to create the site node and give the current user access to
        // it.
        createSession = slingRepository.loginAdministrative(null);
      }

      try {
        Node siteNode;
        if (templatePath != null) {
          siteNode = createSiteFromTemplate(createSession, templatePath, sitePath,
              currentUser.getID());
        } else {
          siteNode = JcrUtils.deepGetOrCreateNode(createSession, sitePath);
        }
        siteNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            SiteService.SITE_RESOURCE_TYPE);

        // setup the ACL's on the node. After this point, administrator
        // access should no longer be needed.
        addEntry(siteNode.getPath(), currentUser, createSession, READ_GRANTED,
            WRITE_GRANTED, REMOVE_CHILD_NODES_GRANTED, MODIFY_PROPERTIES_GRANTED,
            ADD_CHILD_NODES_GRANTED, REMOVE_NODE_GRANTED, READ_ACL_GRANTED,
            MODIFY_ACL_GRANTED, NODE_TYPE_MANAGEMENT_GRANTED, VERSION_MANAGEMENT_GRANTED);
        
//        try {
//			HessianProxyFactory factory = new HessianProxyFactory();
//			XythosRemote xythosService = (XythosRemote) factory.create(XythosRemote.class, xythosHost+remotePath, CreateSiteServlet.class.getClassLoader());
//			xythosService.createGroup(sitePath, request.getRemoteUser());
//		} catch (Exception e1) {
//			LOGGER.warn("failed to create Xythos group when creating site: " + e1.getMessage());
//		}

        if (createSession.hasPendingChanges()) {
          LOGGER.info("Saving changes");
          createSession.save();
          // Save initial version of site
          // TODO Is this really the right place to do this?
          versionService.saveNode(siteNode, currentUser.getID());
        }

        // We add a message store to this site.
        Node storeNode = siteNode.addNode("store");
        storeNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            "sakai/messagestore");

        // Handle authz configuration via a helper.
        SiteAuthz authzHelper = new SiteAuthz(siteNode);
        authzHelper.initAccess(currentUser.getID());

        if (LOGGER.isDebugEnabled()) {
          try {
            JcrUtils.logItem(LOGGER, siteNode);
          } catch (JSONException e) {
            LOGGER.warn(e.getMessage(), e);
          }
        }

        if (createSession.hasPendingChanges()) {
          createSession.save();
        }
      } finally {
        if (granted) {
          createSession.logout();
        }
      }

      // Forward to SlingPostServlet to copy other request parameters to node properties
      // in the usual way.
      RequestDispatcherOptions requestDispatcherOptions = new RequestDispatcherOptions();
      requestDispatcherOptions.setReplaceSelectors("");
      RequestDispatcher requestDispatcher = request.getRequestDispatcher(sitePath,
          requestDispatcherOptions);
      requestDispatcher.forward(request, response);
    } catch (RepositoryException ex) {
      throw new ServletException(ex.getMessage(), ex);
    }

  }

  /**
   * A special pseudo-privilege stored as a normal node property is used to determine
   * whether the current user has permission to create a site in the specified location.
   * The standard "jcr:addChildNodes" privilege is both insufficient (it does not allow
   * setting properties on the newly created node) and too powerful (it would let users
   * sabotage site trees which were otherwise inaccessible to them).
   * 
   * @param session
   * @param sitePath
   * @param userId
   * @return true if the specified user can create a site at the specified path regardless
   *         of other access restrictions; false if the user needs to rely on normal
   *         security checks
   * @throws RepositoryException
   */
  private boolean isCreateSiteGranted(Session session, String sitePath,
      Authorizable currentUser) throws RepositoryException {
    UserManager userManager = AccessControlUtil.getUserManager(session);
    PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);

    Node firstRealNode = JcrUtils.getFirstExistingNode(session, sitePath);
    // iterate up to the root looking for a site marker.
    Node siteMarker = firstRealNode;
    Set<String> principals = new HashSet<String>();
    PrincipalIterator principalIterator = principalManager.getGroupMembership(currentUser
        .getPrincipal());
    boolean granted = false;
    while (!"/".equals(siteMarker.getPath())) {
      if (siteMarker.hasProperty(SITE_CREATE_PRIVILEGE)) {
        Property p = siteMarker.getProperty(SITE_CREATE_PRIVILEGE);
        Value[] authorizableIds = p.getValues();
        for (Value authorizable : authorizableIds) {
          Authorizable grantedAuthorizable = userManager.getAuthorizable(authorizable
              .getString());
          String grantedAuthorizableName = grantedAuthorizable.getPrincipal().getName();
          if (principals.contains(grantedAuthorizableName)) {
            granted = true;
            break;
          }
          while (principalIterator.hasNext()) {
            Principal principal = principalIterator.nextPrincipal();
            if (principal.getName().equals(grantedAuthorizableName)) {
              granted = true;
              break;
            }
            principals.add(principal.getName());
          }
        }
      }
      siteMarker = siteMarker.getParent();
    }
    return granted;
  }

  /**
   * Create a site from a template node and its children.
   * 
   * @param session
   * @param templatePath
   * @param sitePath
   * @return the new site node
   * @throws RepositoryException
   */
  private Node createSiteFromTemplate(Session session, String templatePath,
      String sitePath, String userId) throws RepositoryException {
    // Workspace copy needs the destination's parent to exist and be saved.
    String parentPath = PathUtils.getParentReference(sitePath);
    JcrUtils.deepGetOrCreateNode(session, parentPath);
    if (session.hasPendingChanges()) {
      session.save();
    }
    // Copy the template files in the new folder.
    LOGGER.debug("Copying template ({}) to new dir ({})", templatePath, sitePath);
    Workspace workspace = session.getWorkspace();
    workspace.copy(templatePath, sitePath);
    Node siteNode = (Node) session.getItem(sitePath);
    if (siteNode.hasProperty(SiteService.SAKAI_IS_SITE_TEMPLATE)) {
      if (siteNode.getProperty(SiteService.SAKAI_IS_SITE_TEMPLATE).getBoolean()) {
        siteNode.setProperty(SiteService.SAKAI_IS_SITE_TEMPLATE, false);
      }
    }
    session.save();
    // Give the copied nodes an initial version
    NodeIterator it = siteNode.getNodes();
    while (it.hasNext()) {
      Node n = it.nextNode();
      versionNode(n, userId, session);
    }
    LOGGER.debug("Finished copying");
    return siteNode;
  }

  /**
   * Versions a node and all it's childnodes.
   * 
   * @param n
   * @param userID
   * @param createSession
   */
  private void versionNode(Node n, String userID, Session createSession) {
    try {
      // TODO do better check
      if (n.isNode()
          && !n.getName().startsWith("rep:")
          && !n.getName().startsWith("jcr:")
          && n.hasProperties()
          && !n.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString().equals(
              JcrConstants.NT_RESOURCE)) {
        versionService.saveNode((Node) createSession.getItem(n.getPath()), userID);
        NodeIterator it = n.getNodes();
        // Version the childnodes
        while (it.hasNext()) {
          Node childNode = it.nextNode();
          versionNode(childNode, userID, createSession);
        }
      }
    } catch (RepositoryException re) {
      LOGGER.warn("Unable to save copied node", re);
    }
  }

  /**
   * @param slingRepository
   */
  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  /**
   * @param slingRepository
   */
  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }
}
