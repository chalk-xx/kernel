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

import static org.sakaiproject.nakamura.api.site.SiteService.PARAM_COPY_FROM;
import static org.sakaiproject.nakamura.api.site.SiteService.PARAM_MOVE_FROM;
import static org.sakaiproject.nakamura.api.site.SiteService.PARAM_SITE_PATH;
import static org.sakaiproject.nakamura.api.site.SiteService.SAKAI_IS_SITE_TEMPLATE;
import static org.sakaiproject.nakamura.api.site.SiteService.SAKAI_SITE_TEMPLATE;
import static org.sakaiproject.nakamura.api.site.SiteService.SITES_CONTAINER_RESOURCE_TYPE;
import static org.sakaiproject.nakamura.api.site.SiteService.SITE_RESOURCE_TYPE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
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
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.site.SiteAuthz;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.sakaiproject.nakamura.util.StringUtils;
import org.sakaiproject.nakamura.version.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 */
@Component(immediate = true, label = "%site.createSiteServlet.label", description = "%site.createSiteServlet.desc")
@SlingServlet(resourceTypes = { "sling/servlet/default", "sakai/sites" }, methods = "POST", selectors = "createsite", generateComponent = false)
@ServiceDocumentation(name="Create a Site",
    description="The <code>CreateSiteServlet</code> creates new sites. . /site/container.createsite " +
    		"/site/container/site.createsite If the node is of type " + SITES_CONTAINER_RESOURCE_TYPE + ", then create the " +
    		"site based on a request property. If the node is not of type " + SITES_CONTAINER_RESOURCE_TYPE + ", and exists make it a " + SITE_RESOURCE_TYPE,
    shortDescription="Create a new Site",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sling/servlet/default",SITES_CONTAINER_RESOURCE_TYPE},
        selectors=@ServiceSelector(name="createsite", description="Create Site"),
        extensions=@ServiceExtension(name="html", description="A standard HTML response for creating a node.")),
    methods=@ServiceMethod(name="POST",
        description={"Creates a site, with a name specified in " + PARAM_SITE_PATH + " from an optional template. In the process the servlet" +
        		"will also create all related structures (message stores etc) and set up any groups associated with the site. " +
        		"Create permissions may be controlled by the sakai:sitegroupcreate property, containing a list of principals allowed" +
        		"to create sites under that node. If the current user is not allowed to create a site in the chosen location, then" +
        		"a 403 is returned. " +
        		"Any parameters other than " + PARAM_SITE_PATH + " will be stored as properties on the new site node.",
            "Example<br>" +
            "<pre>Example needed</pre>"
        },
        parameters={
          @ServiceParameter(name=PARAM_SITE_PATH, description="The Path to the site being created (required)"),
          @ServiceParameter(name=SAKAI_SITE_TEMPLATE, description="Path to a template node in JCR to use when creating the site (optional)"),
          @ServiceParameter(name=PARAM_MOVE_FROM, description="Path to an existing site which should be renamed and relocated to the new site path (optional)"),
          @ServiceParameter(name=PARAM_COPY_FROM, description="Path to an existing site which should be copied to the new site path (optional)")
        },
        response={
          @ServiceResponse(code=200,description="Success a body is returned containing a json ove the name of the version saved"),
          @ServiceResponse(code=400,description={
              "If the " + PARAM_SITE_PATH + " parameter is not present",
              "If the " + SAKAI_SITE_TEMPLATE + " parameter does not point to a template in JCR"
          }),
          @ServiceResponse(code=403,description="Current user is not allowed to create a site in the current location."),
          @ServiceResponse(code=404,description="Resource was not found."),
          @ServiceResponse(code=500,description="Failure with HTML explanation.")}
    ))

public class CreateSiteServlet extends AbstractSiteServlet {

  private static final long serialVersionUID = -7996020354919244147L;

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateSiteServlet.class);

  @org.apache.felix.scr.annotations.Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @org.apache.felix.scr.annotations.Property(value = "Supports creation of sites, either from existing folders, or new folders.")
  static final String SERVICE_DESCRIPTION = "service.description";

  private static final String SITE_CREATE_PRIVILEGE = "sakai:sitegroupcreate";

  @Reference
  private transient SlingRepository slingRepository;

  @Reference
  private transient VersionService versionService;

  @Reference
  private transient AuthorizablePostProcessService postProcessService;

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
      if ( UserConstants.ANON_USERID.equals(session.getUserID()) ) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable currentUser = userManager.getAuthorizable(request.getRemoteUser());

      String templatePath = null;
      String copyFromPath = null;
      String moveFromPath = null;

      String sitePath = getSitePath(request, response);
      if (sitePath == null) {
        return;
      }
      LOGGER.debug("The sitePath is: {}", sitePath);
     
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
        if (!getSiteService().isSiteTemplate(session.getItem(templatePath))) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + SAKAI_SITE_TEMPLATE + " must be set to a site which has the "
              + SAKAI_IS_SITE_TEMPLATE + " set.");
          return;
        }
      }
      // If we have been asked to move a site, make sure it exists.
      RequestParameter requestParameter = request.getRequestParameter(PARAM_MOVE_FROM);
      if (requestParameter != null) {
        moveFromPath = requestParameter.getString();
        if (!session.itemExists(moveFromPath) || !getSiteService().isSite(session.getItem(moveFromPath))) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + PARAM_MOVE_FROM + " must be set to an existing site.");
          return;
        }
      }
      // If we have been asked to copy a site, make sure it exists.
      requestParameter = request.getRequestParameter(PARAM_COPY_FROM);
      if (requestParameter != null) {
        copyFromPath = requestParameter.getString();
        if (!session.itemExists(copyFromPath) || !getSiteService().isSite(session.getItem(copyFromPath))) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + PARAM_COPY_FROM + " must be set to an existing site.");
          return;
        }
      }
      // We can only create a site in one way at a time.
      if ( ((templatePath != null) && ((moveFromPath != null) || (copyFromPath != null)))
          || ((moveFromPath != null) && (copyFromPath != null)) ) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Only one of " + SAKAI_SITE_TEMPLATE + ", " + PARAM_MOVE_FROM +
            ", and " + PARAM_COPY_FROM + " can be specified.");
        return;
      }

      Session adminSession = slingRepository.loginAdministrative(null);
      boolean granted = isCreateSiteGranted(session, adminSession, sitePath, currentUser);
      Session createSession = session;
      if (granted) {
        // Switch to gain administrative powers for at least long enough
        // to create the site node and give the current user access to
        // it.
        createSession = adminSession;
      } else {
        
        adminSession.logout();
        adminSession = null;
      }
      
      LOGGER.info("Creating Site {} for user {} with session {}",new Object[] { sitePath, currentUser.getID(), session.getUserID()});
      	

      // Perform the actual creation or move.
      try {
        Node siteNode;
        if (templatePath != null) {
          siteNode = createSiteFromTemplate(createSession, templatePath, sitePath, currentUser);
        } else if (copyFromPath != null) {
          siteNode = copySite(createSession, copyFromPath, sitePath, currentUser);
        } else if (moveFromPath != null) {
          siteNode = moveSite(createSession, moveFromPath, sitePath, currentUser);
        } else {
          siteNode = createSiteWithoutTemplate(createSession, sitePath, currentUser);
        }
        if (LOGGER.isDebugEnabled()) {
          try {
            JcrUtils.logItem(LOGGER, siteNode);
          } catch (JSONException e) {
            LOGGER.warn(e.getMessage(), e);
          }
        }
      } finally {
        if (adminSession != null) {
          adminSession.logout();
        }
      }

      // Forward to SlingPostServlet to copy other request parameters to node properties
      // in the usual way.
      RequestDispatcherOptions requestDispatcherOptions = new RequestDispatcherOptions();
      requestDispatcherOptions.setReplaceSelectors("");
      RequestDispatcher requestDispatcher = request.getRequestDispatcher(sitePath, requestDispatcherOptions);
      requestDispatcher.forward(request, response);
    } catch (RepositoryException ex) {
      throw new ServletException(ex.getMessage(), ex);
    }

  }

  /**
   * A special pseudo-privilege stored as a normal node property is used
   * to determine whether the current user has permission to create a site
   * in the specified location. The standard "jcr:addChildNodes" privilege
   * is both insufficient (it does not allow setting properties on the newly
   * created node) and too powerful (it would let users sabotage site
   * trees which were otherwise inaccessible to them).
   *
   * @param session
   * @param sitePath
   * @param adminSession needed to check node paths in case the current user does
   *   not have read access
   * @param userId
   * @return true if the specified user can create a site at the specified path
   *   regardless of other access restrictions; false if the user needs to rely on
   *   normal security checks
   * @throws RepositoryException
   */
  private boolean isCreateSiteGranted(Session session, Session adminSession, String sitePath, Authorizable currentUser) throws RepositoryException {
    UserManager userManager = AccessControlUtil.getUserManager(session);
    PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);

    Node firstRealNode = null;
    for (String firstRealNodePath = sitePath;
      (firstRealNode == null) && (firstRealNodePath != null);
      firstRealNodePath = PathUtils.getParentReference(firstRealNodePath)) {
      if (adminSession.itemExists(firstRealNodePath)) {
        firstRealNode = (Node)adminSession.getItem(firstRealNodePath);
      }
    }
    if (firstRealNode == null) {
      return false;
    }
    // If the target path already exists, do not circumvent normal access checks.
    if (firstRealNode.getPath().equals(sitePath)) {
      return false;
    }

    // iterate up to (but not including) the root looking for a site marker.
    Node siteMarker = firstRealNode;
    Set<String> principals = new HashSet<String>();
    PrincipalIterator principalIterator = principalManager
        .getGroupMembership(currentUser.getPrincipal());
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
      } else if (getSiteService().isSite(siteMarker)) {
        // Do not circumvent normal access checks under an existing site, no matter
        // what its parent is.
        return false;
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
  private Node createSiteFromTemplate(Session session, String templatePath, String sitePath, Authorizable creator) throws RepositoryException {
    ensureParent(session, sitePath);
    
    // Copy the template files in the new folder.
    LOGGER.debug("Copying template ({}) to new dir ({})", templatePath,
        sitePath);
    Workspace workspace = session.getWorkspace();
    workspace.copy(templatePath, sitePath);
    Node siteNode = (Node) session.getItem(sitePath);
    if (siteNode.hasProperty(SAKAI_IS_SITE_TEMPLATE)) {
      if (siteNode.getProperty(SAKAI_IS_SITE_TEMPLATE).getBoolean()) {
        siteNode.setProperty(SAKAI_IS_SITE_TEMPLATE, false);
      }
    }
    session.save();

    initializeAccess(session, siteNode, creator);
    initializeNewSite(session, siteNode);
    if (session.hasPendingChanges()) {
      session.save();
    }
    versionNodeAndChildren(siteNode, creator.getID(), session);
    LOGGER.debug("Finished copying");
    return siteNode;
  }
  
  private Node createSiteWithoutTemplate(Session session, String sitePath, Authorizable creator) throws RepositoryException {
    Node siteNode = JcrUtils.deepGetOrCreateNode(session, sitePath);
    session.save();

    initializeAccess(session, siteNode, creator);
    initializeNewSite(session, siteNode);
    if (session.hasPendingChanges()) {
      session.save();
    }
    versionNodeAndChildren(siteNode, creator.getID(), session);
    LOGGER.debug("Finished copying");
    return siteNode;
  }
  
  private Node copySite(Session session, String fromPath, String sitePath, Authorizable creator) throws RepositoryException {
    ensureParent(session, sitePath);
    
    // Copy the template files in the new folder.
    LOGGER.debug("Copying site ({}) to new dir ({})", fromPath,
        sitePath);
    Workspace workspace = session.getWorkspace();
    workspace.copy(fromPath, sitePath);
    Node siteNode = (Node) session.getItem(sitePath);
    session.save();

    initializeAccess(session, siteNode, creator);
    if (session.hasPendingChanges()) {
      session.save();
    }
    versionNodeAndChildren(siteNode, creator.getID(), session);
    LOGGER.debug("Finished copying");
    return siteNode;
  }
  
  private Node moveSite(Session session, String fromPath, String sitePath, Authorizable creator) throws RepositoryException {
    ensureParent(session, sitePath);
    
    // Copy the template files in the new folder.
    LOGGER.debug("Moving site ({}) to new dir ({})", fromPath,
        sitePath);
    Workspace workspace = session.getWorkspace();
    workspace.move(fromPath, sitePath);
    Node siteNode = (Node) session.getItem(sitePath);
    session.save();

    if (session.hasPendingChanges()) {
      session.save();
    }
    versionService.saveNode(siteNode, creator.getID());
    LOGGER.debug("Finished move");
    return siteNode;
  }

  /**
   * Workspace copy/move needs the destination's parent to exist and be saved.
   * @param session
   * @param sitePath
   * @throws RepositoryException 
   */
  private void ensureParent(Session session, String sitePath) throws RepositoryException {
    String parentPath = PathUtils.getParentReference(sitePath);
    JcrUtils.deepGetOrCreateNode(session, parentPath);
    if (session.hasPendingChanges()) {
      session.save();
    }
  }
  
  private void initializeAccess(Session session, Node site, Authorizable creator) throws RepositoryException {
    // Give the creator full rights on the site tree.
    AccessControlUtil.replaceAccessControlEntry(session, site.getPath(), creator.getPrincipal(),
        new String[] {"jcr:all"}, null, null);

    // Handle authz configuration via a helper.
    SiteAuthz authzHelper = new SiteAuthz(site, postProcessService);
    authzHelper.initAccess(creator.getID());
  }
  
  private void initializeNewSite(Session session, Node site) throws RepositoryException {
    site.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
        SiteService.SITE_RESOURCE_TYPE);

    // Add a message store to this site.
    // TODO Is there any reason this can't be handled by site templates?
    Node storeNode = site.addNode("store");
    storeNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
        "sakai/messagestore");
  }
  
  /**
   * Parse the request to get the destination of the new or moved site.
   * @param request
   * @param response
   * @return null if an error needs to be returned to the user
   * @throws IOException 
   */
  private String getSitePath(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
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

  /**
   * Versions a node and all its child nodes.
   *
   * @param n
   * @param userID
   * @param createSession
   */
  private void versionNodeAndChildren(Node n, String userID, Session createSession) {
    try {
      // TODO do better check
      if (n.isNode() && !n.getName().startsWith("rep:") && !n.getName().startsWith("jcr:") && n.hasProperties() && !n.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString().equals(JcrConstants.NT_RESOURCE)) {
        versionService.saveNode((Node) createSession.getItem(n.getPath()), userID);
        NodeIterator it = n.getNodes();
        // Version the childnodes
        while (it.hasNext()) {
          Node childNode = it.nextNode();
          versionNodeAndChildren(childNode, userID, createSession);
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
