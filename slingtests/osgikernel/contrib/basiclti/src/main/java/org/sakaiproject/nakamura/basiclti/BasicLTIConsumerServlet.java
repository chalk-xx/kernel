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
package org.sakaiproject.nakamura.basiclti;

import static org.imsglobal.basiclti.BasicLTIConstants.CONTEXT_ID;
import static org.imsglobal.basiclti.BasicLTIConstants.CONTEXT_LABEL;
import static org.imsglobal.basiclti.BasicLTIConstants.CONTEXT_TITLE;
import static org.imsglobal.basiclti.BasicLTIConstants.CONTEXT_TYPE;
import static org.imsglobal.basiclti.BasicLTIConstants.LAUNCH_PRESENTATION_DOCUMENT_TARGET;
import static org.imsglobal.basiclti.BasicLTIConstants.LAUNCH_PRESENTATION_LOCALE;
import static org.imsglobal.basiclti.BasicLTIConstants.LIS_PERSON_CONTACT_EMAIL_PRIMARY;
import static org.imsglobal.basiclti.BasicLTIConstants.LIS_PERSON_NAME_FAMILY;
import static org.imsglobal.basiclti.BasicLTIConstants.LIS_PERSON_NAME_FULL;
import static org.imsglobal.basiclti.BasicLTIConstants.LIS_PERSON_NAME_GIVEN;
import static org.imsglobal.basiclti.BasicLTIConstants.RESOURCE_LINK_ID;
import static org.imsglobal.basiclti.BasicLTIConstants.ROLES;
import static org.imsglobal.basiclti.BasicLTIConstants.USER_ID;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.ADMIN_CONFIG_PATH;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.DEBUG;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.DEBUG_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.FRAME_HEIGHT;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.FRAME_HEIGHT_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.LTI_ADMIN_NODE_NAME;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.LTI_KEY;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.LTI_KEY_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.LTI_SECRET;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.LTI_SECRET_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.LTI_URL;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.LTI_URL_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.RELEASE_EMAIL;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.RELEASE_EMAIL_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.RELEASE_NAMES;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.RELEASE_NAMES_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.RELEASE_PRINCIPAL_NAME;
import static org.sakaiproject.nakamura.api.basiclti.BasicLtiAppConstants.RELEASE_PRINCIPAL_NAME_LOCK;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.imsglobal.basiclti.BasicLTIConstants;
import org.imsglobal.basiclti.BasicLTIUtil;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ACLUtils;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * TODO remove selector binding to bind to all calls to this type. Check
 * selectors manually for launch and behave the same as today. On POST, validate
 * and create child node with correct ACLs. On default behavior, include secured
 * parameters if a site owner. OPTIONS, GET, HEAD, POST, PUT not supported,
 * DELETE must also delete child node, TRACE, CONNECT.
 */
@SlingServlet(methods = { "GET", "POST", "PUT", "DELETE" }, resourceTypes = { "sakai/basiclti" })
public class BasicLTIConsumerServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = 5985490994324951127L;
  private static final Logger LOG = LoggerFactory
      .getLogger(BasicLTIConsumerServlet.class);
  /**
   * A {@link Map} containing all of the known application settings and their
   * associated locking keys.
   */
  private transient Map<String, String> applicationSettings = null;
  /**
   * Keys we never want sent in the launch data.
   */
  private static final String[] BLACKLIST = { LTI_KEY, LTI_SECRET, LTI_URL };
  /**
   * The keys that must be specially secured from normal Sling operation.
   */
  private transient Set<String> sensitiveKeys = null;
  /**
   * Dependency injected from OSGI container.
   */
  @Reference
  private transient SlingRepository slingRepository;

  // global properties used for every tool launch
  /**
   * See: {@link BasicLTIConstants#TOOL_CONSUMER_INSTANCE_CONTACT_EMAIL}
   */
  private transient String instanceContactEmail;
  /**
   * See: {@link BasicLTIConstants#TOOL_CONSUMER_INSTANCE_DESCRIPTION}
   */
  private transient String instanceDescription;
  /**
   * See: {@link BasicLTIConstants#TOOL_CONSUMER_INSTANCE_GUID}
   */
  private transient String instanceGuid;
  /**
   * See: {@link BasicLTIConstants#TOOL_CONSUMER_INSTANCE_NAME}
   */
  private transient String instanceName;
  /**
   * See: {@link BasicLTIConstants#TOOL_CONSUMER_INSTANCE_URL}
   */
  private transient String instanceUrl;

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
   */
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    // initialize the keys that must be secured from normal Sling operation
    sensitiveKeys = new HashSet<String>(2);
    sensitiveKeys.add(LTI_KEY);
    sensitiveKeys.add(LTI_SECRET);

    applicationSettings = new HashMap<String, String>(8);
    applicationSettings.put(LTI_URL, LTI_URL_LOCK);
    applicationSettings.put(LTI_SECRET, LTI_SECRET_LOCK);
    applicationSettings.put(LTI_KEY, LTI_KEY_LOCK);
    applicationSettings.put(FRAME_HEIGHT, FRAME_HEIGHT_LOCK);
    applicationSettings.put(DEBUG, DEBUG_LOCK);
    applicationSettings.put(RELEASE_NAMES, RELEASE_NAMES_LOCK);
    applicationSettings.put(RELEASE_EMAIL, RELEASE_EMAIL_LOCK);
    applicationSettings
        .put(RELEASE_PRINCIPAL_NAME, RELEASE_PRINCIPAL_NAME_LOCK);

    // FIXME read from global settings location TBD
    instanceContactEmail = "admin@sakaiproject.org";
    instanceDescription = "The Sakai Project";
    instanceGuid = "sakaiproject.org";
    instanceName = "Sakai";
    instanceUrl = "http://sakaiproject.org";
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    LOG
        .debug("doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)");
    final String[] selectors = request.getRequestPathInfo().getSelectors();
    if (selectors != null && selectors.length > 0) {
      for (final String selector : request.getRequestPathInfo().getSelectors()) {
        if ("launch".equalsIgnoreCase(selector)) {
          doLaunch(request, response);
          return;
        }
      }
    } else { // no selectors requested
      // TODO verify permissions and return sensitive data *only* to site admins
      if ("json".equalsIgnoreCase(request.getRequestPathInfo().getExtension())) {
        final Resource resource = request.getResource();
        if (resource == null) {
          sendError(HttpServletResponse.SC_NOT_FOUND,
              "Resource could not be found", new Error(
                  "Resource could not be found"), response);
        }
        final Node node = resource.adaptTo(Node.class);
        final ExtendedJSONWriter json = new ExtendedJSONWriter(response
            .getWriter());
        try {
          response.setContentType("application/json");
          json.node(node);
        } catch (Exception e) {
          sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
              .getLocalizedMessage(), e, response);
        }
      }
    }
  }

  protected void doLaunch(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    LOG
        .debug("doLaunch(SlingHttpServletRequest request, SlingHttpServletResponse response)");
    final Resource resource = request.getResource();
    if (resource == null) {
      sendError(HttpServletResponse.SC_NOT_FOUND,
          "Resource could not be found", new Error(
              "Resource could not be found"), response);
    }
    final Node node = resource.adaptTo(Node.class);
    final Session session = request.getResourceResolver()
        .adaptTo(Session.class);
    try {
      // TODO this is commented out just for quick testing workaround
      final String vtoolId = "basiclti";
      // if (!node.hasProperty(LTI_VTOOL_ID)) {
      // sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, LTI_VTOOL_ID
      // + " cannot be null", new IllegalArgumentException(LTI_VTOOL_ID
      // + " cannot be null"), response);
      // return;
      // }
      // final String vtoolId = node.getProperty(LTI_VTOOL_ID).getString();

      final String adminNodePath = ADMIN_CONFIG_PATH + "/" + vtoolId;
      Map<String, String> adminSettings = null;
      if (session.itemExists(adminNodePath)) {
        LOG.debug("Found administrative settings for virtual tool: " + vtoolId);
        final Node adminNode = (Node) session.getItem(adminNodePath);
        adminSettings = getSettings(adminNode);
      } else {
        LOG
            .debug(
                "No administrative settings found for virtual tool: {}. No policy to apply.",
                vtoolId);
        adminSettings = Collections.emptyMap();
      }
      final Map<String, String> userSettings = getSettings(node);

      // merge admin and user properties
      final Map<String, String> effectiveSettings = new HashMap<String, String>(
          Math.max(adminSettings.size(), userSettings.size()));
      for (final String setting : applicationSettings.keySet()) {
        effectiveSetting(setting, effectiveSettings, adminSettings,
            userSettings);
      }

      final Map<String, String> launchProps = new HashMap<String, String>();

      // LTI_URL
      final String ltiUrl = effectiveSettings.get(LTI_URL);
      if (ltiUrl == null || "".equals(ltiUrl)) {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, LTI_URL
            + " cannot be null", new IllegalArgumentException(LTI_URL
            + " cannot be null"), response);
        return;
      }
      // LTI_SECRET
      final String ltiSecret = effectiveSettings.get(LTI_SECRET);
      if (ltiSecret == null || "".equals(ltiSecret)) {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, LTI_SECRET
            + " cannot be null", new IllegalArgumentException(LTI_SECRET
            + " cannot be null"), response);
        return;
      }
      // LTI_KEY
      final String ltiKey = effectiveSettings.get(LTI_KEY);
      if (ltiKey == null || "".equals(ltiKey)) {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, LTI_KEY
            + " cannot be null", new IllegalArgumentException(LTI_KEY
            + " cannot be null"), response);
        return;
      }

      // e.g. /sites/foo/_widgets/id944280073/basiclti
      launchProps.put(RESOURCE_LINK_ID, node.getPath());

      final UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable az = userManager.getAuthorizable(session.getUserID());

      final boolean releasePrincipal = Boolean.parseBoolean(effectiveSettings
          .get(RELEASE_PRINCIPAL_NAME));
      if (releasePrincipal) {
        launchProps.put(USER_ID, az.getID());
      }

      final Node siteNode = findSiteNode(node);
      if (siteNode == null) {
        final String message = "Could not locate site node.";
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message,
            new IllegalStateException(message), response);
        return;
      }
      launchProps.put(CONTEXT_ID, siteNode.getPath());
      launchProps.put(CONTEXT_TITLE, siteNode.getProperty("name").getString());
      launchProps.put(CONTEXT_LABEL, siteNode.getProperty("id").getString());

      // TODO how to determine site type?
      // CourseSection probably satisfies 90% of our use cases.
      // Maybe Group should be used for project sites?
      launchProps.put(CONTEXT_TYPE, "CourseSection");

      // FIXME need to pull roles from system
      final AccessControlManager accessControlManager = AccessControlUtil
          .getAccessControlManager(session);
      final Privilege[] modifyACLs = { accessControlManager
          .privilegeFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL) };
      // TODO replace with real site path
      boolean canManageSite = accessControlManager.hasPrivileges("/sites/foo",
          modifyACLs);
      LOG.info("hasPrivileges(modifyAccessControl)=" + canManageSite);
      if ("anonymous".equals(session.getUserID())) {
        launchProps.put(ROLES, "None");
      } else if (canManageSite) {
        launchProps.put(ROLES, "Instructor");
      } else {
        launchProps.put(ROLES, "Learner");
      }

      final boolean releaseNames = Boolean.parseBoolean(effectiveSettings
          .get(RELEASE_NAMES));
      if (releaseNames) {
        String firstName = null;
        if (az.hasProperty("firstName")) {
          firstName = az.getProperty("firstName")[0].getString();
          launchProps.put(LIS_PERSON_NAME_GIVEN, firstName);
        }
        String lastName = null;
        if (az.hasProperty("lastName")) {
          lastName = az.getProperty("lastName")[0].getString();
          launchProps.put(LIS_PERSON_NAME_FAMILY, lastName);
        }
        StringBuilder sb = new StringBuilder();
        if (firstName != null) {
          sb.append(firstName);
          sb.append(" ");
        }
        if (lastName != null) {
          sb.append(lastName);
        }
        final String fullName = sb.toString();
        if (firstName != null || lastName != null) {
          // only if at least one name is not null
          launchProps.put(LIS_PERSON_NAME_FULL, fullName);
        }
      }

      final boolean releaseEmail = Boolean.parseBoolean(effectiveSettings
          .get(RELEASE_EMAIL));
      if (releaseEmail) {
        if (az.hasProperty("email")) {
          final String email = az.getProperty("email")[0].getString();
          launchProps.put(LIS_PERSON_CONTACT_EMAIL_PRIMARY, email);
        }
      }

      launchProps.put(LAUNCH_PRESENTATION_LOCALE, request.getLocale()
          .toString());

      // we will always launch in an iframe for the time being
      launchProps.put(LAUNCH_PRESENTATION_DOCUMENT_TARGET, "iframe");

      final boolean debug = Boolean.parseBoolean(effectiveSettings.get(DEBUG));
      // might be useful for the remote end to know if debug is enabled...
      launchProps.put(DEBUG, "" + debug);

      // TODO required to pass certification test suite
      launchProps.put("simple_key", "custom_simple_value");
      launchProps.put("Complex!@#$^*(){}[]KEY", "Complex!@#$^*(){}[]Value");

      // TODO remove debug output
      for (final Entry<String, String> entry : launchProps.entrySet()) {
        LOG.info("launchProps: " + entry.getKey() + "=" + entry.getValue());
      }
      final Map<String, String> cleanProps = BasicLTIUtil.cleanupProperties(
          launchProps, BLACKLIST);
      // TODO remove debug output
      for (final Entry<String, String> entry : cleanProps.entrySet()) {
        LOG.info("cleanProps: " + entry.getKey() + "=" + entry.getValue());
      }
      final Map<String, String> signedProperties = BasicLTIUtil.signProperties(
          cleanProps, ltiUrl, "POST", ltiKey, ltiSecret, instanceGuid,
          instanceDescription, instanceUrl, instanceName, instanceContactEmail);
      final String extension = request.getRequestPathInfo().getExtension();
      if ("html".equalsIgnoreCase(extension)) { // return html
        final String html = BasicLTIUtil.postLaunchHTML(signedProperties,
            ltiUrl, debug);
        response.getWriter().write(html);
      } else { // return json
        renderJson(response.getWriter(), ltiUrl, signedProperties);
      }
    } catch (Throwable e) {
      sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getLocalizedMessage(), e, response);
    }
  }

  /**
   * Intended for nodes of sling:resourceType=sakai/basiclti - i.e. not
   * sensitive nodes.
   * 
   * @param node
   * @return
   * @throws RepositoryException
   */
  private Map<String, String> getSettings(final Node node)
      throws RepositoryException {
    // loop through Node properties
    final PropertyIterator iter = node.getProperties();
    Map<String, String> settings = new HashMap<String, String>((int) iter
        .getSize());
    while (iter.hasNext()) {
      final Property property = iter.nextProperty();
      final String propertyName = property.getName();
      if (sensitiveKeys.contains(propertyName)) {
        LOG.error("Sensitive data exposed: {} in {}!", propertyName, node
            .getPath());
      }
      settings.put(propertyName, property.getValue().getString());
    }
    final Map<String, String> sensitiveData = readSensitiveNode(node);
    settings.putAll(sensitiveData);
    return settings;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doDelete(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doDelete(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    // TODO Delete child node or is it auto deleted?
    super.doDelete(request, response);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    // TODO Create child node to store sensitive data and ACL it
    final Resource resource = request.getResource();
    if (resource == null) {
      sendError(HttpServletResponse.SC_NOT_FOUND,
          "Resource could not be found", new Error(
              "Resource could not be found"), response);
    }
    final Node node = resource.adaptTo(Node.class);
    final Session session = request.getResourceResolver()
        .adaptTo(Session.class);
    try {
      final Map<String, String> sensitiveData = new HashMap<String, String>(
          sensitiveKeys.size());
      // loop through request parameters
      for (final Entry<String, RequestParameter[]> entry : request
          .getRequestParameterMap().entrySet()) {
        final String key = entry.getKey();
        if (entry.getValue() == null || entry.getValue().length == 0) {
          removeProperty(node, key);
        } else {
          if (entry.getValue().length > 1) {
            sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Multi-valued parameters are not supported", null, response);
            return;
          } else {
            final String value = entry.getValue()[0].getString("UTF-8");
            if ("".equals(value)) {
              removeProperty(node, key);
            } else { // has a valid value
              if (sensitiveKeys.contains(key)) {
                sensitiveData.put(key, value);
              } else {
                node.setProperty(key, value);
              }
            }
          }
        }
      } // end request parameters loop
      // safety precaution - just to be safe
      for (String skey : sensitiveKeys) {
        removeProperty(node, skey);
      }
      if (session.hasPendingChanges()) {
        session.save();
      }
      createSensitiveNode(node, session, sensitiveData);
    } catch (Exception e) {
      sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getLocalizedMessage(), e, response);
    }
  }

  private void createSensitiveNode(final Node parent,
      final Session userSession, Map<String, String> sensitiveData)
      throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException {
    final String adminNodePath = parent.getPath() + "/" + LTI_ADMIN_NODE_NAME;
    // sanity check to verify user does not have permissions to admin node
    boolean invalidPrivileges = false;
    if (!isAdminUser(userSession)) { // i.e. normal user
      try {
        final AccessControlManager acm = AccessControlUtil
            .getAccessControlManager(userSession);
        Privilege[] userPrivs = acm.getPrivileges(adminNodePath);
        Set<Privilege> invalidUserPrivileges = getInvalidUserPrivileges(acm);
        for (Privilege privilege : userPrivs) {
          System.out.println("Privilege=" + privilege.getName());
          if (invalidUserPrivileges.contains(privilege)) {
            invalidPrivileges = true;
          }
        }
      } catch (PathNotFoundException e) { // This is to be expected
        LOG
            .debug(
                "The node does not exist or the user does not have permission(?): {}",
                adminNodePath);
      }
    }
    if (invalidPrivileges) {
      LOG.error("{} can access sensitive data: {}", userSession.getUserID(),
          adminNodePath);
      // Will be repaired by accessControlSensitiveNode(adminNodePath,
      // adminSession) below.
    }
    // now let's elevate Privileges and do some admin modifications
    Session adminSession = null;
    try {
      adminSession = slingRepository.loginAdministrative(null);
      final Node adminNode = JcrUtils.deepGetOrCreateNode(adminSession,
          adminNodePath);
      for (final Entry<String, String> entry : sensitiveData.entrySet()) {
        adminNode.setProperty(entry.getKey(), entry.getValue());
      }
      // ensure only admins can read the node
      accessControlSensitiveNode(adminNodePath, adminSession);
      if (adminSession.hasPendingChanges()) {
        adminSession.save();
      }
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    } // end admin elevation
  }

  private void accessControlSensitiveNode(final String adminNodePath,
      final Session adminSession) throws AccessDeniedException,
      UnsupportedRepositoryOperationException, RepositoryException {
    final UserManager userManager = AccessControlUtil
        .getUserManager(adminSession);
    final PrincipalManager principalManager = AccessControlUtil
        .getPrincipalManager(adminSession);
    final Authorizable anonymous = userManager
        .getAuthorizable(UserConstants.ANON_USERID);
    final Authorizable everyone = userManager.getAuthorizable(principalManager
        .getEveryone());
    ACLUtils.addEntry(adminNodePath, anonymous, adminSession,
        ACLUtils.ALL_DENIED);
    ACLUtils.addEntry(adminNodePath, everyone, adminSession,
        ACLUtils.ALL_DENIED);
  }

  private Map<String, String> readSensitiveNode(final Node parent)
      throws RepositoryException {
    final String adminNodePath = parent.getPath() + "/" + LTI_ADMIN_NODE_NAME;
    Map<String, String> settings = null;
    // now let's elevate Privileges and do some admin modifications
    Session adminSession = null;
    try {
      adminSession = slingRepository.loginAdministrative(null);
      if (adminSession.itemExists(adminNodePath)) {
        Node adminNode = (Node) adminSession.getItem(adminNodePath);
        final PropertyIterator iter = adminNode.getProperties();
        settings = new HashMap<String, String>((int) iter.getSize());
        while (iter.hasNext()) {
          final Property property = iter.nextProperty();
          final String propertyName = property.getName();
          if ("jcr:mixinTypes".equals(propertyName)) { // skip this property
            continue;
          }
          settings.put(propertyName, property.getValue().getString());
        }
      } else {
        throw new PathNotFoundException("Node does not exist: " + adminNodePath);
      }
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    } // end admin elevation
    return settings;
  }

  /**
   * Checks to see if the current user is a member of the administrators group.
   * 
   * @param session
   * @return
   * @throws UnsupportedRepositoryOperationException
   * @throws RepositoryException
   */
  private boolean isAdminUser(final Session session)
      throws UnsupportedRepositoryOperationException, RepositoryException {
    final UserManager userManager = AccessControlUtil.getUserManager(session);
    final Authorizable authorizable = userManager.getAuthorizable(session
        .getUserID());
    boolean isAdmin = false;
    if (authorizable != null) {
      final Principal principal = authorizable.getPrincipal();
      if (principal != null) {
        final PrincipalManager principalManager = AccessControlUtil
            .getPrincipalManager(session);
        final PrincipalIterator it = principalManager
            .getGroupMembership(principal);
        while (it.hasNext()) {
          if (SecurityConstants.ADMINISTRATORS_NAME.equals(it.nextPrincipal()
              .getName())) {
            isAdmin = true;
            break;
          }
        }
      }
    }
    return isAdmin;
  }

  private Set<Privilege> getInvalidUserPrivileges(final AccessControlManager acm)
      throws AccessControlException, RepositoryException {
    Set<Privilege> invalidUserPrivileges = new HashSet<Privilege>(9);
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_ALL));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_READ));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_WRITE));
    invalidUserPrivileges.add(acm
        .privilegeFromName(Privilege.JCR_MODIFY_PROPERTIES));
    invalidUserPrivileges.add(acm
        .privilegeFromName(Privilege.JCR_READ_ACCESS_CONTROL));
    invalidUserPrivileges.add(acm
        .privilegeFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL));
    invalidUserPrivileges.add(acm
        .privilegeFromName(Privilege.JCR_ADD_CHILD_NODES));
    invalidUserPrivileges.add(acm
        .privilegeFromName(Privilege.JCR_REMOVE_CHILD_NODES));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_REMOVE_NODE));
    return invalidUserPrivileges;
  }

  private void removeProperty(final Node node, final String property)
      throws VersionException, LockException, ConstraintViolationException,
      PathNotFoundException, RepositoryException {
    if (node.hasProperty(property)) {
      node.getProperty(property).remove();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPut(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPut(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
        "PUT method not allowed.", null, response);
  }

  /**
   * Simple helper to render a JSON response.
   * 
   * @param pwriter
   * @param ltiUrl
   * @param properties
   * @throws JSONException
   */
  private void renderJson(final Writer pwriter, final String ltiUrl,
      final Map<String, String> properties) throws JSONException {
    final ExtendedJSONWriter writer = new ExtendedJSONWriter(pwriter);
    writer.object(); // root object
    writer.key("launchURL");
    writer.value(ltiUrl);
    writer.key("postData");
    writer.object();
    for (final Object propkey : properties.keySet()) {
      writer.key((String) propkey);
      writer.value(properties.get((String) propkey));
    }
    writer.endObject(); // postData
    writer.endObject(); // root object
  }

  /**
   * Simple helper to apply business logic to settings. For each setting, the
   * adminSetting becomes the default value unless the adminSetting is locked.
   * If it is locked, then the userSetting cannot override the adminSetting
   * (i.e. it essentially is a system policy instead of a default value). Once
   * these rules have been applied, the result is stored in effectiveSettings.
   * 
   * @param setting
   *          The key.
   * @param effectiveSettings
   *          Mutated during normal operation.
   * @param adminSettings
   *          The admin policies and default values. Locks are applied.
   * @param userSettings
   *          Tool placement settings. Locks are ignored.
   */
  private void effectiveSetting(final String setting,
      final Map<String, String> effectiveSettings,
      final Map<String, String> adminSettings,
      final Map<String, String> userSettings) {
    if (setting == null || effectiveSettings == null || adminSettings == null
        || userSettings == null) {
      throw new IllegalArgumentException();
    }
    final boolean locked = Boolean.parseBoolean(adminSettings
        .get(applicationSettings.get(setting)));
    if (locked) { // the locked admin setting takes precedence
      effectiveSettings.put(setting, adminSettings.get(setting));
    } else {
      // not locked; an admin setting will be the default value
      final String adminValue = adminSettings.get(setting);
      final String userValue = userSettings.get(setting);
      if (userValue == null) { // user did *not* supply a setting
        if (adminValue != null) { // only if value exists
          effectiveSettings.put(setting, adminValue);
        }
      } else { // user *did* supply a setting
        effectiveSettings.put(setting, userValue);
      }
    }
    return;
  }

  /**
   * Utility method to start walking back up the hierarchy looking for the
   * containing Site node. If the Site node cannot be found, null is returned.
   * 
   * @param startingNode
   * @return The containing Site node if found else return null.
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws ItemNotFoundException
   * @throws AccessDeniedException
   * @throws RepositoryException
   */
  private Node findSiteNode(final Node startingNode)
      throws ValueFormatException, PathNotFoundException,
      ItemNotFoundException, AccessDeniedException, RepositoryException {
    Node returnNode = null;
    Node traversalNode = startingNode;
    while (traversalNode.getDepth() != 0) {
      if (traversalNode.hasProperty("sling:resourceType")) {
        if ("sakai/site".equals(traversalNode.getProperty("sling:resourceType")
            .getString())) {
          // found the parent site node
          returnNode = traversalNode;
          break;
        }
      }
      traversalNode = traversalNode.getParent();
    }
    return returnNode;
  }

  /**
   * 
   * @param errorCode
   *          See: {@link HttpServletResponse}
   * @param message
   *          Message to be emitted in error response.
   * @param exception
   *          Optional exception that will be thrown only if
   *          {@link HttpServletResponse#isCommitted()}. It will be logged in
   *          either case. Allows null value.
   * @param response
   */
  private void sendError(int errorCode, String message, Throwable exception,
      HttpServletResponse response) {
    if (!response.isCommitted()) {
      try {
        LOG.error(errorCode + ": " + message, exception);
        response.sendError(errorCode, message);
      } catch (IOException e) {
        throw new Error(e);
      }
    } else {
      LOG.error(errorCode + ": " + message, exception);
      throw new Error(message, exception);
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
