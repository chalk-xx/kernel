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
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.ADMIN_CONFIG_PATH;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.DEBUG;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.DEBUG_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.FRAME_HEIGHT;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.FRAME_HEIGHT_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.GLOBAL_SETTINGS;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_ADMIN_NODE_NAME;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_KEY;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_KEY_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_SECRET;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_SECRET_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_URL;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_URL_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_VTOOL_ID;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.RELEASE_EMAIL;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.RELEASE_EMAIL_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.RELEASE_NAMES;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.RELEASE_NAMES_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.RELEASE_PRINCIPAL_NAME;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.RELEASE_PRINCIPAL_NAME_LOCK;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.TOPIC_BASICLTI_ACCESSED;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.TOPIC_BASICLTI_CHANGED;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.TOPIC_BASICLTI_LAUNCHED;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.TOPIC_BASICLTI_REMOVED;
import static org.sakaiproject.nakamura.basiclti.LiteBasicLTIServletUtils.getInvalidUserPrivileges;
import static org.sakaiproject.nakamura.basiclti.LiteBasicLTIServletUtils.isAdminUser;
import static org.sakaiproject.nakamura.basiclti.LiteBasicLTIServletUtils.removeProperty;
import static org.sakaiproject.nakamura.basiclti.LiteBasicLTIServletUtils.sensitiveKeys;
import static org.sakaiproject.nakamura.basiclti.LiteBasicLTIServletUtils.unsupportedKeys;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.imsglobal.basiclti.BasicLTIConstants;
import org.imsglobal.basiclti.BasicLTIUtil;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.basiclti.LiteBasicLTIContextIdResolver;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permission;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "BasicLTIConsumerServlet", okForVersion = "0.11",
    shortDescription = "Performs all activities related to BasicLTI functionality.",
    description = "Performs all activities related to BasicLTI functionality.",
    bindings = @ServiceBinding(type = BindingType.TYPE, bindings = "sakai/basiclti",
      extensions = {
        @ServiceExtension(name = "json", description = "This is the default return type if none is specified."),
        @ServiceExtension(name = "html", description = "Useful only in the context of a .launch selector and the preferred return type in that case.")
      },
      selectors = @ServiceSelector(name = "launch", description = "Used to retrieve the launch data for a BasicLTI launch (i.e. invocation of the service).")),
    methods = {
      @ServiceMethod(name = "GET", description = "Get information about a sakai/basiclti resource.",
        parameters = {
          @ServiceParameter(name = "None", description = "No parameters are required.")
        },
        response = {
          @ServiceResponse(code = HttpServletResponse.SC_OK, description = "Request has been processed successfully."),
          @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Resource could not be found."),
          @ServiceResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to process request due to a runtime error.")
        }),
      @ServiceMethod(name = "POST", description = "Create or update LTI launch parameters or widget settings.",
        parameters = {
          @ServiceParameter(name = LTI_KEY, description = "The opaque key given by the LTI provider."),
          @ServiceParameter(name = LTI_SECRET, description = "The shared secret given by the LTI provider."),
          @ServiceParameter(name = LTI_URL, description = "The LTI end point of the LTI provider."),
          @ServiceParameter(name = LTI_VTOOL_ID, description = "The virtualToolId if acting as a virtual tool."),
          @ServiceParameter(name = RELEASE_EMAIL, description = "Controls privacy of email address in launch data."),
          @ServiceParameter(name = RELEASE_NAMES, description = "Controls privacy of first/last name in launch data."),
          @ServiceParameter(name = RELEASE_PRINCIPAL_NAME, description = "Controls privacy of username in launch data."),
          @ServiceParameter(name = "*", description = "The service will try to persist any parameter that is available in the POST data. Some keys will be ignored if unsupported."),
          @ServiceParameter(name = "*@TypeHint", description = "The service adheres to the @TypeHint sling conventions as much as possible.")
        },
        response = {
          @ServiceResponse(code = HttpServletResponse.SC_OK, description = "Request has been processed successfully."),
          @ServiceResponse(code = HttpServletResponse.SC_BAD_REQUEST, description = "Multi-valued parameters are not supported."),
          @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Resource could not be found."),
          @ServiceResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to process request due to a runtime error.")
        }),
      @ServiceMethod(name = "PUT", description = "The PUT method is not supported for sakai/basiclti nodes.",
        response = {
          @ServiceResponse(code = HttpServletResponse.SC_METHOD_NOT_ALLOWED, description = "PUT method not allowed.")
        }),
      @ServiceMethod(name = "DELETE", description = "Delete a sakai/basiclti node and its corresponding data.",
        parameters = {
          @ServiceParameter(name = "None", description = "No parameters are required.")
        },
        response = {
          @ServiceResponse(code = HttpServletResponse.SC_OK, description = "Everything was deleted as expected"),
          @ServiceResponse(code = HttpServletResponse.SC_FORBIDDEN, description = "Unauthorized: The current user does not have permissions to delete the data."),
          @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Resource could not be found."),
          @ServiceResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to delete the node due to a runtime error.")
        })
    })
@SlingServlet(methods = { "GET", "POST", "PUT", "DELETE" }, resourceTypes = { "sakai/basiclti" })
public class LiteBasicLTIConsumerServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = 5985490994324951127L;
  private static final Logger LOG = LoggerFactory
      .getLogger(LiteBasicLTIConsumerServlet.class);
  /**
   * A {@link Map} containing all of the known application settings and their associated
   * locking keys.
   */
  private transient Map<String, String> applicationSettings = null;
  /**
   * Keys we never want sent in the launch data.
   */
  private static final String[] BLACKLIST = { LTI_KEY, LTI_SECRET, LTI_URL };
  /**
   * Dependency injected from OSGI container.
   */
  @Reference
  private transient SlingRepository jcrRepository;

  @Reference
  private transient Repository sparseRepository;

  @Reference
  protected transient EventAdmin eventAdmin;

  @Reference
  protected transient LiteBasicLTIContextIdResolver contextIdResolver;

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

    applicationSettings = new HashMap<String, String>(8);
    applicationSettings.put(LTI_URL, LTI_URL_LOCK);
    applicationSettings.put(LTI_SECRET, LTI_SECRET_LOCK);
    applicationSettings.put(LTI_KEY, LTI_KEY_LOCK);
    applicationSettings.put(FRAME_HEIGHT, FRAME_HEIGHT_LOCK);
    applicationSettings.put(DEBUG, DEBUG_LOCK);
    applicationSettings.put(RELEASE_NAMES, RELEASE_NAMES_LOCK);
    applicationSettings.put(RELEASE_EMAIL, RELEASE_EMAIL_LOCK);
    applicationSettings.put(RELEASE_PRINCIPAL_NAME, RELEASE_PRINCIPAL_NAME_LOCK);

    javax.jcr.Session adminSession = null;
    try {
      adminSession = jcrRepository.loginAdministrative(null);
      if (adminSession.itemExists(GLOBAL_SETTINGS)) {
        final javax.jcr.Node adminNode = (javax.jcr.Node) adminSession
            .getItem(GLOBAL_SETTINGS);
        final PropertyIterator iter = adminNode.getProperties();
        while (iter.hasNext()) {
          final Property property = iter.nextProperty();
          final String propertyName = property.getName();
          if ("instanceContactEmail".equals(propertyName)) {
            instanceContactEmail = property.getValue().getString();
            continue;
          }
          if ("instanceDescription".equals(propertyName)) {
            instanceDescription = property.getValue().getString();
            continue;
          }
          if ("instanceGuid".equals(propertyName)) {
            instanceGuid = property.getValue().getString();
            continue;
          }
          if ("instanceName".equals(propertyName)) {
            instanceName = property.getValue().getString();
            continue;
          }
          if ("instanceUrl".equals(propertyName)) {
            instanceUrl = property.getValue().getString();
            continue;
          }
        }
      } else {
        LOG.error("GLOBAL_SETTINGS node does not exist: {}", GLOBAL_SETTINGS);
        throw new IllegalStateException("GLOBAL_SETTINGS node does not exist: "
            + GLOBAL_SETTINGS);
      }
    } catch (RepositoryException e) {
      throw new Error(e);
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    }
    if (instanceContactEmail == null || instanceDescription == null
        || instanceGuid == null || instanceName == null || instanceUrl == null) {
      throw new IllegalStateException("Missing one or more required global settings!");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    LOG.debug("doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)");
    final String[] selectors = request.getRequestPathInfo().getSelectors();
    if (selectors != null && selectors.length > 0) {
      for (final String selector : request.getRequestPathInfo().getSelectors()) {
        if ("launch".equals(selector)) {
          doLaunch(request, response);

          // Send out an OSGi event that we accessed a basic/lti node.
          final Dictionary<String, String> properties = new Hashtable<String, String>();
          properties.put(UserConstants.EVENT_PROP_USERID, request.getRemoteUser());
          EventUtils.sendOsgiEvent(properties, TOPIC_BASICLTI_LAUNCHED, eventAdmin);
          return;
        }
      }
    } else { // no selectors requested check extension
      final String extension = request.getRequestPathInfo().getExtension();
      if (extension == null || "json".equals(extension)) {
        final Resource resource = request.getResource();
        if (resource == null) {
          sendError(HttpServletResponse.SC_NOT_FOUND, "Resource could not be found",
              new Error("Resource could not be found"), response);
          return;
        }
        final Content content = resource.adaptTo(Content.class);
        final javax.jcr.Node node = resource.adaptTo(javax.jcr.Node.class);
        if (content != null) { // sparse path
          try {
            response.setContentType("application/json");
            final Map<String, Object> settings = new HashMap<String, Object>(readProperties(content));
            final Session session = StorageClientUtils.adaptToSession(request
                .getResourceResolver().adaptTo(javax.jcr.Session.class));
            if (canManageSettings(content.getPath(), session)) {
              settings.putAll(readSensitiveNode(content));
            }
            final Map<String, Object> adminSettings = getAdminSettings(content, false);
            settings.putAll(adminSettings);
            renderJson(response.getWriter(), settings);
          } catch (Exception e) {
            sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                e.getLocalizedMessage(), e, response);
          }
        } else if (node != null) { // JCR path
          try {
            response.setContentType("application/json");
            final Map<String, Object> settings = readProperties(node);
            final javax.jcr.Session session = request.getResourceResolver().adaptTo(
                javax.jcr.Session.class);
            if (canManageSettings(node.getPath(), session)) {
              settings.putAll(readSensitiveNode(node));
            }
            final Map<String, Object> adminSettings = getAdminSettings(node, false);
            settings.putAll(adminSettings);
            renderJson(response.getWriter(), settings);
          } catch (Exception e) {
            sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                e.getLocalizedMessage(), e, response);
          }
        } else {
          sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find resource: "
              + resource.getPath(), null, response);
        }
      }
      response.setStatus(HttpServletResponse.SC_OK);

      // Send out an OSGi event that we accessed a basic/lti node.
      final Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(UserConstants.EVENT_PROP_USERID, request.getRemoteUser());
      EventUtils.sendOsgiEvent(properties, TOPIC_BASICLTI_ACCESSED, eventAdmin);
      return;
    }
  }

  protected void doLaunch(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    LOG.debug("doLaunch(SlingHttpServletRequest request, SlingHttpServletResponse response)");
    final Resource resource = request.getResource();
    if (resource == null) {
      sendError(HttpServletResponse.SC_NOT_FOUND, "Resource could not be found",
          new Error("Resource could not be found"), response);
    }
    final Content node = resource.adaptTo(Content.class);
    if (node == null) {
      sendError(HttpServletResponse.SC_NOT_FOUND, "Resource could not be found: "
          + resource.getPath(), new Error("Resource could not be found"), response);
    }
    final Session session = StorageClientUtils.adaptToSession(request
        .getResourceResolver().adaptTo(javax.jcr.Session.class));
    // determine virtual toolId
    try {
      // grab admin settings
      final Map<String, Object> adminSettings = getAdminSettings(node, true);
      // grab user settings
      final Map<String, Object> userSettings = getLaunchSettings(node);

      // merge admin and user properties
      final Map<String, Object> effectiveSettings = new HashMap<String, Object>(Math.max(
          adminSettings.size(), userSettings.size()));
      for (final String setting : applicationSettings.keySet()) {
        effectiveSetting(setting, effectiveSettings, adminSettings, userSettings);
      }

      final Map<String, String> launchProps = new HashMap<String, String>();

      // LTI_URL
      final String ltiUrl = (String) effectiveSettings.get(LTI_URL);
      if (ltiUrl == null || "".equals(ltiUrl)) {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, LTI_URL
            + " cannot be null",
            new IllegalArgumentException(LTI_URL + " cannot be null"), response);
        return;
      }
      // LTI_SECRET
      final String ltiSecret = (String) effectiveSettings.get(LTI_SECRET);
      if (ltiSecret == null || "".equals(ltiSecret)) {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, LTI_SECRET
            + " cannot be null", new IllegalArgumentException(LTI_SECRET
            + " cannot be null"), response);
        return;
      }
      // LTI_KEY
      final String ltiKey = (String) effectiveSettings.get(LTI_KEY);
      if (ltiKey == null || "".equals(ltiKey)) {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, LTI_KEY
            + " cannot be null",
            new IllegalArgumentException(LTI_KEY + " cannot be null"), response);
        return;
      }

      // e.g. /sites/foo/_widgets/id944280073/basiclti
      launchProps.put(RESOURCE_LINK_ID, node.getPath());

      final AuthorizableManager userManager = session.getAuthorizableManager();
      final org.sakaiproject.nakamura.api.lite.authorizable.Authorizable az = userManager
          .findAuthorizable(session.getUserId());

      final boolean releasePrincipal = (Boolean)effectiveSettings
          .get(RELEASE_PRINCIPAL_NAME);
      if (releasePrincipal) {
        launchProps.put(USER_ID, az.getId());
      }

      final Content pooledContentNode = findPooledContentNode(node, session);
      if (pooledContentNode == null) {
        final String message = "Could not locate group home node.";
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message,
            new IllegalStateException(message), response);
        return;
      }
      final String sitePath = pooledContentNode.getPath();
      final String contextId = contextIdResolver.resolveContextId(pooledContentNode);
      if (contextId == null) {
        throw new IllegalStateException("Could not resolve context_id!");
      }
      launchProps.put(CONTEXT_ID, contextId);
      if ("sakai/pooled-content".equals(pooledContentNode
          .getProperty("sling:resourceType"))) {
        launchProps.put(CONTEXT_TITLE,
            (String) pooledContentNode.getProperty("sakai:pooled-content-file-name"));
        launchProps.put(CONTEXT_LABEL,
            (String) pooledContentNode.getProperty("sakai:description"));
      } else { // sakai/group-home
        final Content groupProfileNode = session.getContentManager().get(
            pooledContentNode.getPath() + "/public/authprofile");
        if (groupProfileNode != null) {
          launchProps.put(CONTEXT_TITLE,
              (String) groupProfileNode.getProperty("sakai:group-title"));
          launchProps.put(CONTEXT_LABEL,
              (String) groupProfileNode.getProperty("sakai:group-id"));
        } else {
          // cannot find group profile data
          launchProps.put(CONTEXT_TITLE, (String) pooledContentNode.getProperty("_path"));
          launchProps.put(CONTEXT_LABEL, (String) pooledContentNode.getProperty("_path"));
        }
      }

      // FIXME how to determine site type?
      // CourseSection probably satisfies 90% of our use cases.
      // Maybe Group should be used for project sites?
      launchProps.put(CONTEXT_TYPE, "CourseSection");

      final org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager accessControlManager = session
          .getAccessControlManager();
      final boolean canManageSite = accessControlManager.can(az, Security.ZONE_CONTENT,
          sitePath, Permissions.CAN_WRITE_ACL);
      LOG.info("hasPrivileges(modifyAccessControl)=" + canManageSite);
      if (UserConstants.ANON_USERID.equals(session.getUserId())) {
        launchProps.put(ROLES, "None");
      } else if (canManageSite) {
        launchProps.put(ROLES, "Instructor");
      } else {
        launchProps.put(ROLES, "Learner");
      }

      final boolean releaseNames = (Boolean)effectiveSettings
          .get(RELEASE_NAMES);
      if (releaseNames) {
        String firstName = null;
        if (az.hasProperty("firstName")) {
          firstName = ((String)az.getProperty("firstName"));
          launchProps.put(LIS_PERSON_NAME_GIVEN, firstName);
        }
        String lastName = null;
        if (az.hasProperty("lastName")) {
          lastName = ((String)az.getProperty("lastName"));
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

      final boolean releaseEmail = (Boolean) effectiveSettings
          .get(RELEASE_EMAIL);
      if (releaseEmail) {
        if (az.hasProperty("email")) {
          final String email = ((String)az.getProperty("email"));
          launchProps.put(LIS_PERSON_CONTACT_EMAIL_PRIMARY, email);
        }
      }

      launchProps.put(LAUNCH_PRESENTATION_LOCALE, request.getLocale().toString());

      // we will always launch in an iframe for the time being
      launchProps.put(LAUNCH_PRESENTATION_DOCUMENT_TARGET, "iframe");

      final boolean debug = (Boolean) effectiveSettings.get(DEBUG);
      // might be useful for the remote end to know if debug is enabled...
      launchProps.put(DEBUG, "" + debug);

      // FYI required to pass certification test suite
      launchProps.put("simple_key", "custom_simple_value");
      launchProps.put("Complex!@#$^*(){}[]KEY", "Complex!@#$^*(){}[]Value");

      final Map<String, String> cleanProps = BasicLTIUtil.cleanupProperties(launchProps,
          BLACKLIST);
      final Map<String, String> signedProperties = BasicLTIUtil.signProperties(
          cleanProps, ltiUrl, "POST", ltiKey, ltiSecret, instanceGuid,
          instanceDescription, instanceUrl, instanceName, instanceContactEmail);
      final String extension = request.getRequestPathInfo().getExtension();
      if ("html".equalsIgnoreCase(extension)) { // return html
        final String html = BasicLTIUtil.postLaunchHTML(signedProperties, ltiUrl, debug);
        response.getWriter().write(html);
      } else { // return json
        renderJson(response.getWriter(), ltiUrl, signedProperties);
      }
    } catch (Throwable e) {
      sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), e,
          response);
      return;
    }
    response.setStatus(HttpServletResponse.SC_OK);
  }

  /**
   * Intended for nodes of <code>sling:resourceType=sakai/basiclti</code> - i.e. not
   * sensitive nodes.
   * 
   * @param node
   * @return
   * @throws RepositoryException
   */
  private Map<String, Object> getLaunchSettings(final javax.jcr.Node node)
      throws RepositoryException {
    final Map<String, Object> settings = readProperties(node);
    // sanity check for sensitive data in settings node
    final List<String> keysToRemove = new ArrayList<String>(sensitiveKeys.size());
    for (final Entry<String, Object> entry : settings.entrySet()) {
      final String key = entry.getKey();
      if (sensitiveKeys.contains(key)) {
        LOG.error("Sensitive data exposed: {} in {}", key, node.getPath());
        keysToRemove.add(key);
      }
    }
    for (final String key : keysToRemove) {
      settings.remove(key);
    }
    final Map<String, String> sensitiveData = readSensitiveNode(node);
    settings.putAll(sensitiveData);
    return settings;
  }

  /**
   * Intended for nodes of <code>sling:resourceType=sakai/basiclti</code> - i.e. not
   * sensitive nodes.
   * 
   * @param node
   * @return
   * @throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException
   * @throws StorageClientException
   * @throws ClientPoolException
   */
  private Map<String, Object> getLaunchSettings(final Content node)
      throws RepositoryException, ClientPoolException, StorageClientException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException {
    final Map<String, Object> settings = new HashMap<String,Object>(readProperties(node));
    // sanity check for sensitive data in settings node
    final List<String> keysToRemove = new ArrayList<String>(sensitiveKeys.size());
    for (final Entry<String, Object> entry : settings.entrySet()) {
      final String key = entry.getKey();
      if (sensitiveKeys.contains(key)) {
        LOG.error("Sensitive data exposed: {} in {}", key, node.getPath());
        keysToRemove.add(key);
      }
    }
    for (final String key : keysToRemove) {
      settings.remove(key);
    }
    final Map<String, String> sensitiveData = readSensitiveNode(node);
    settings.putAll(sensitiveData);
    return settings;
  }

  /**
   * Helper method to read the policy settings from /var/basiclti/* virtual tools.
   * 
   * @param node
   * @param launchMode
   *          true if sensitive settings should be included; false if not to include
   *          sesitive settings.
   * @return An empty Map if no settings could be found.
   * @throws RepositoryException
   */
  private Map<String, Object> getAdminSettings(final javax.jcr.Node node,
      final boolean launchMode) throws RepositoryException {
    // grab admin settings from /var/basiclti/* if they exist...
    String vtoolId = null;
    if (node.hasProperty(LTI_VTOOL_ID)) {
      vtoolId = node.getProperty(LTI_VTOOL_ID).getString();
    } else {
      vtoolId = "basiclti";
    }
    return getAdminSettings(vtoolId, launchMode);
  }

  /**
   * Helper method to read the policy settings from /var/basiclti/* virtual tools.
   * 
   * @param node
   * @param launchMode
   *          true if sensitive settings should be included; false if not to include
   *          sesitive settings.
   * @return An empty Map if no settings could be found.
   * @throws RepositoryException
   */
  private Map<String, Object> getAdminSettings(final Content node,
      final boolean launchMode) throws RepositoryException {
    // grab admin settings from /var/basiclti/* if they exist...
    String vtoolId = null;
    if (node.hasProperty(LTI_VTOOL_ID)) {
      vtoolId = (String)node.getProperty(LTI_VTOOL_ID);
    } else {
      vtoolId = "basiclti";
    }
    return getAdminSettings(vtoolId, launchMode);
  }

  /**
   * Helper method to read the policy settings from /var/basiclti/* virtual tools.
   * 
   * @param vtoolId
   *          The virtual tool id (e.g. <code>sakai.resources</code>)
   * @param launchMode
   *          true if sensitive settings should be included; false if not to include
   *          sesitive settings.
   * @return An empty Map if no settings could be found.
   * @throws RepositoryException
   */
  private Map<String, Object> getAdminSettings(final String vtoolId,
      final boolean launchMode) throws RepositoryException {
    // grab admin settings from /var/basiclti/* if they exist...
    final String adminNodePath = ADMIN_CONFIG_PATH + "/" + vtoolId;
    Map<String, Object> adminSettings = null;
    // begin admin elevation
    javax.jcr.Session adminSession = null;
    try {
      adminSession = jcrRepository.loginAdministrative(null);
      if (adminSession.itemExists(adminNodePath)) {
        LOG.debug("Found administrative settings for virtual tool: " + vtoolId);
        final javax.jcr.Node adminNode = (javax.jcr.Node) adminSession
            .getItem(adminNodePath);
        if (launchMode) {
          adminSettings = getLaunchSettings(adminNode);
        } else {
          adminSettings = readProperties(adminNode);
        }
      } else {
        LOG.debug(
            "No administrative settings found for virtual tool: {}. No policy to apply.",
            vtoolId);
        adminSettings = Collections.emptyMap();
      }
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    } // end admin elevation
    return adminSettings;
  }

  /**
   * Simple helper method to read Node Properties into a <code>Map<String, String></code>.
   * No sanity checking is performed for issues regarding sensitive data exposure.
   * <p>
   * Only well known application settings will be returned in the Map (i.e. other
   * properties will be filtered out).
   * 
   * @param node
   * @return
   * @throws RepositoryException
   */
  private Map<String, Object> readProperties(final javax.jcr.Node node)
      throws RepositoryException {
    // loop through Node properties
    final PropertyIterator iter = node.getProperties();
    final Map<String, Object> settings = new HashMap<String, Object>((int) iter.getSize());
    while (iter.hasNext()) {
      final Property property = iter.nextProperty();
      switch (property.getValue().getType()) {
      case PropertyType.STRING:
        settings.put(property.getName(), property.getValue().getString());
        break;
      case PropertyType.BOOLEAN:
        settings.put(property.getName(),
            Boolean.valueOf(property.getValue().getBoolean()));
        break;
      default:
        break;
      }
    }
    return settings;
  }

  /**
   * Simple helper method to read Node Properties into a <code>Map<String, String></code>.
   * No sanity checking is performed for issues regarding sensitive data exposure.
   * <p>
   * Only well known application settings will be returned in the Map (i.e. other
   * properties will be filtered out).
   * 
   * @param node
   * @return
   */
  private Map<String, Object> readProperties(final Content node) {
    Map<String, Object> props = new HashMap<String, Object>(node.getProperties());
    return props;
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
    LOG.debug("doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response)");
    final Resource resource = request.getResource();
    if (resource == null) {
      sendError(HttpServletResponse.SC_NOT_FOUND, "Resource could not be found",
          new Error("Resource could not be found"), response);
    }
    final Content node = resource.adaptTo(Content.class);
    final Session session = StorageClientUtils.adaptToSession(request
        .getResourceResolver().adaptTo(javax.jcr.Session.class));
    try {
      if (canRemoveNode(node.getPath(), session)) {
        removeSensitiveNode(node);
        session.getContentManager().delete(node.getPath());

        // Send out an OSGi event that we removed a basic/lti node.
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(UserConstants.EVENT_PROP_USERID, request.getRemoteUser());
        EventUtils.sendOsgiEvent(properties, TOPIC_BASICLTI_REMOVED, eventAdmin);
      } else {
        sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied", null, response);
      }
    } catch (Exception e) {
      sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), e,
          response);
    }
    response.setStatus(HttpServletResponse.SC_OK);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    final Resource resource = request.getResource();
    if (resource == null) {
      sendError(HttpServletResponse.SC_NOT_FOUND, "Resource could not be found",
          new Error("Resource could not be found"), response);
    }
    final Content node = resource.adaptTo(Content.class);
    if (node == null) {
      sendError(HttpServletResponse.SC_NOT_FOUND, "Resource could not be found: "
          + resource.getPath(), new Error("Resource could not be found"), response);
    }
    final Session session = StorageClientUtils.adaptToSession(request
        .getResourceResolver().adaptTo(javax.jcr.Session.class));
    try {
      final Map<String, String> sensitiveData = new HashMap<String, String>(
          sensitiveKeys.size());
      // loop through request parameters
      final RequestParameterMap requestParameterMap = request.getRequestParameterMap();
      for (final Entry<String, RequestParameter[]> entry : requestParameterMap.entrySet()) {
        final String key = entry.getKey();
        if (key.endsWith("@TypeHint")) {
          continue;
        }
        final RequestParameter[] requestParameterArray = entry.getValue();
        if (requestParameterArray == null || requestParameterArray.length == 0) {
          removeProperty(node, key);
        } else {
          if (requestParameterArray.length > 1) {
            sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Multi-valued parameters are not supported", null, response);
            return;
          } else {
            final String value = requestParameterArray[0].getString("UTF-8");
            if ("".equals(value)) {
              removeProperty(node, key);
            } else { // has a valid value
              if (sensitiveKeys.contains(key)) {
                sensitiveData.put(key, value);
              } else {
                if (!unsupportedKeys.contains(key)) {
                  final String typeHint = key + "@TypeHint";
                  if (requestParameterMap.containsKey(typeHint)
                      && "Boolean".equals(requestParameterMap.get(typeHint)[0]
                          .getString())) {
                    node.setProperty(key,Boolean.valueOf(value));
                  } else {
                    node.setProperty(key, value);
                  }
                }
              }
            }
          }
        }
      } // end request parameters loop
      // safety precaution - just to be safe
      for (String skey : sensitiveKeys) {
        removeProperty(node, skey);
      }
      session.getContentManager().update(node);
      updateSensitiveNode(node, session, sensitiveData);

      // Send out an OSGi event that we changed a basic/lti node.
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(UserConstants.EVENT_PROP_USERID, request.getRemoteUser());
      EventUtils.sendOsgiEvent(properties, TOPIC_BASICLTI_CHANGED, eventAdmin);
    } catch (Exception e) {
      sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), e,
          response);
    }
  }

  /**
   * 
   * @param parent
   * @param userSession
   * @param sensitiveData
   * @throws StorageClientException
   * @throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException
   */
  private void updateSensitiveNode(final Content parent, final Session userSession,
      Map<String, String> sensitiveData) throws StorageClientException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException {
    if (parent == null) {
      throw new IllegalArgumentException("Node parent==null");
    }
    // if (!"sakai/basiclti".equals(parent.getProperty("sling:resourceType"))) {
    // throw new
    // IllegalArgumentException("sling:resourceType != sakai/basiclti");
    // }
    if (userSession == null) {
      throw new IllegalArgumentException("userSession == null");
    }
    if (sensitiveData == null || sensitiveData.isEmpty()) {
      // do nothing - virtual tool use case
      return;
    }
    final String adminNodePath = parent.getPath() + "/" + LTI_ADMIN_NODE_NAME;
    // now let's elevate Privileges and do some admin modifications
    Session adminSession = null;
    try {
      adminSession = sparseRepository.loginAdministrative();
      final Content adminNode = adminSession.getContentManager().get(adminNodePath);
      for (final Entry<String, String> entry : sensitiveData.entrySet()) {
        adminNode.setProperty(entry.getKey(), entry.getValue());
      }
      adminSession.getContentManager().update(adminNode);
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    } // end admin elevation
    // sanity check to verify user does not have permissions to sensitive node
    boolean invalidPrivileges = false;
    if (!isAdminUser(userSession)) { // i.e. normal user
      final AccessControlManager acm = userSession.getAccessControlManager();
      final Permission[] userPrivs = acm.getPermissions(Security.ZONE_CONTENT,
          adminNodePath);
      if (userPrivs != null && userPrivs.length > 0) {
        Set<Permission> invalidUserPrivileges = getInvalidUserPrivileges(acm);
        for (final Permission permission : userPrivs) {
          if (invalidUserPrivileges.contains(permission)) {
            LOG.error("{} has invalid permission: {} on {}",
                new Object[] { userSession.getUserId(), permission.toString(),
                    adminNodePath });
            invalidPrivileges = true;
            break;
          }
        }
      }
    }
    if (invalidPrivileges) {
      throw new IllegalStateException(userSession.getUserId()
          + " has invalid privileges: " + adminNodePath);
    }
  }

  /**
   * Since normal users do not have privileges to read the sensitive node, this method
   * elevates privileges to admin to read the values.
   * <p>
   * This method should <em>only</em> be called from <code>doLaunch()</code> or if the
   * user has permissions to manage the <code>sakai/basiclti</code> settings node (i.e.
   * <code>canManageSettings()</code> from <code>doGet()</code>).
   * 
   * @param parent
   * @return
   * @throws RepositoryException
   */
  private Map<String, String> readSensitiveNode(final javax.jcr.Node parent)
      throws RepositoryException {
    final String adminNodePath = parent.getPath() + "/" + LTI_ADMIN_NODE_NAME;
    Map<String, String> settings = null;
    // now let's elevate Privileges and do some admin modifications
    javax.jcr.Session adminSession = null;
    try {
      adminSession = jcrRepository.loginAdministrative(null);
      if (adminSession.itemExists(adminNodePath)) {
        final javax.jcr.Node adminNode = (javax.jcr.Node) adminSession
            .getItem(adminNodePath);
        final PropertyIterator iter = adminNode.getProperties();
        settings = new HashMap<String, String>((int) iter.getSize());
        while (iter.hasNext()) {
          final Property property = iter.nextProperty();
          final String propertyName = property.getName();
          if (sensitiveKeys.contains(propertyName)) { // the ones we care about
            settings.put(propertyName, property.getValue().getString());
          }
        }
      } else {
        settings = new HashMap<String, String>(0);
      }
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    } // end admin elevation
    return settings;
  }

  /**
   * Since normal users do not have privileges to read the sensitive node, this method
   * elevates privileges to admin to read the values.
   * <p>
   * This method should <em>only</em> be called from <code>doLaunch()</code> or if the
   * user has permissions to manage the <code>sakai/basiclti</code> settings node (i.e.
   * <code>canManageSettings()</code> from <code>doGet()</code>).
   * 
   * @param parent
   * @return
   * @throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException
   * @throws StorageClientException
   * @throws ClientPoolException
   */
  private Map<String, String> readSensitiveNode(final Content parent)
      throws ClientPoolException, StorageClientException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException {
    final String adminNodePath = parent.getPath() + "/" + LTI_ADMIN_NODE_NAME;
    Map<String, String> settings = null;
    // now let's elevate Privileges
    Session adminSession = null;
    try {
      adminSession = sparseRepository.loginAdministrative();
      if (adminSession.getContentManager().exists(adminNodePath)) {
        final Content adminNode = adminSession.getContentManager().get(adminNodePath);
        final Map<String, Object> properties = adminNode.getProperties();
        settings = new HashMap<String, String>((int) properties.size());
        for (Entry<String, Object> entry : properties.entrySet()) {
          if (sensitiveKeys.contains(entry.getKey())) { // the ones we care about
            settings.put(entry.getKey(), (String)entry.getValue());
          }
        }
      } else {
        LOG.warn("Could not find sensitiveContent: " + adminNodePath);
        settings = new HashMap<String, String>(0);
      }
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    } // end admin elevation
    return settings;
  }

  /**
   * 
   * @param parent
   * @throws ClientPoolException
   * @throws StorageClientException
   * @throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException
   */
  private void removeSensitiveNode(final Content parent) throws ClientPoolException,
      StorageClientException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException {
    final String adminNodePath = parent.getPath() + "/" + LTI_ADMIN_NODE_NAME;
    // now let's elevate Privileges and do some admin modifications
    Session adminSession = null;
    try {
      adminSession = sparseRepository.loginAdministrative();
      if (adminSession.getContentManager().exists(adminNodePath)) {
        adminSession.getContentManager().delete(adminNodePath);
      }
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    } // end admin elevation
  }

  /**
   * Does userSession.getUserId have permission to modify the <code>sakai/basiclti</code>
   * settings node?
   * 
   * @param path
   *          Absolute path to the <code>sakai/basiclti</code> settings node.
   * @param userSession
   *          The session of the current user (i.e. not the admin session)
   * @return <code>true</code> if the session has the specified privileges;
   *         <code>false</code> otherwise.
   * @throws UnsupportedRepositoryOperationException
   * @throws RepositoryException
   */
  private boolean canManageSettings(final String path, final javax.jcr.Session userSession)
      throws UnsupportedRepositoryOperationException, RepositoryException {
    final javax.jcr.security.AccessControlManager accessControlManager = AccessControlUtil
        .getAccessControlManager(userSession);
    final javax.jcr.security.Privilege[] modifyProperties = { accessControlManager
        .privilegeFromName(javax.jcr.security.Privilege.JCR_MODIFY_PROPERTIES) };
    boolean canManageSettings = accessControlManager
        .hasPrivileges(path, modifyProperties);
    return canManageSettings;
  }

  /**
   * Does userSession.getUserId have permission to modify the <code>sakai/basiclti</code>
   * settings node?
   * 
   * @param path
   *          Absolute path to the <code>sakai/basiclti</code> settings node.
   * @param userSession
   *          The session of the current user (i.e. not the admin session)
   * @return <code>true</code> if the session has the specified privileges;
   *         <code>false</code> otherwise.
   * @throws StorageClientException
   * @throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException
   */
  private boolean canManageSettings(final String path, final Session userSession)
      throws StorageClientException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException {

    final org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager accessControlManager = userSession
        .getAccessControlManager();
    final org.sakaiproject.nakamura.api.lite.authorizable.Authorizable user = (org.sakaiproject.nakamura.api.lite.authorizable.Authorizable) userSession
        .getAuthorizableManager().findAuthorizable(userSession.getUserId());
    // TODO maybe should be Permissions.CAN_MANAGE?
    return accessControlManager.can(user, Security.ZONE_CONTENT, path,
        Permissions.CAN_WRITE_ACL);
  }

  /**
   * Does userSession.getUserId have permission to remove the <code>sakai/basiclti</code>
   * settings node?
   * 
   * @param path
   * @param userSession
   * @return
   * @throws StorageClientException
   * @throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException
   */
  private boolean canRemoveNode(final String path, final Session userSession)
      throws StorageClientException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException {
    final AccessControlManager accessControlManager = userSession
        .getAccessControlManager();
    return accessControlManager.can(userSession.getAuthorizableManager()
        .findAuthorizable(userSession.getUserId()), Security.ZONE_CONTENT, path,
        Permissions.CAN_DELETE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPut(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "PUT method not allowed.", null,
        response);
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
    for (final Entry<String, String> entry : properties.entrySet()) {
      writer.key(entry.getKey());
      writer.value(entry.getValue());
    }
    writer.endObject(); // postData
    writer.endObject(); // root object
  }

  /**
   * Simple helper to render a JSON response.
   * 
   * @param pwriter
   * @param properties
   * @throws JSONException
   */
  private void renderJson(final Writer pwriter, final Map<String, Object> properties)
      throws JSONException {
    final ExtendedJSONWriter writer = new ExtendedJSONWriter(pwriter);
    writer.object(); // root object
    for (final Entry<String, Object> entry : properties.entrySet()) {
      writer.key(entry.getKey());
      writer.value(entry.getValue());
    }
    writer.endObject(); // root object
  }

  /**
   * Simple helper to apply business logic to settings. For each setting, the adminSetting
   * becomes the default value unless the adminSetting is locked. If it is locked, then
   * the userSetting cannot override the adminSetting (i.e. it essentially is a system
   * policy instead of a default value). Once these rules have been applied, the result is
   * stored in effectiveSettings.
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
      final Map<String, Object> effectiveSettings,
      final Map<String, Object> adminSettings, final Map<String, Object> userSettings) {
    if (setting == null || effectiveSettings == null || adminSettings == null
        || userSettings == null) {
      throw new IllegalArgumentException();
    }
    final Object adminSetting = adminSettings.get(applicationSettings.get(setting));
    boolean locked = false; // default to unlocked
    if (adminSetting != null) {
      locked = (Boolean) adminSetting;
    }
    if (locked) { // the locked admin setting takes precedence
      effectiveSettings.put(setting, adminSettings.get(setting));
    } else {
      // not locked; an admin setting will be the default value
      final Object adminValue = adminSettings.get(setting);
      final Object userValue = userSettings.get(setting);
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
   * containing pooled content node. If the pooled content node cannot be found, 
   * null is returned.
   * 
   * @param startingNode
   * @param session
   * @return The containing Site node if found else return null.
   * @throws org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException
   * @throws StorageClientException
   */
  private Content findPooledContentNode(final Content startingNode, Session session)
      throws StorageClientException,
      org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException {
    Content returnNode = null;
    Content traversalNode = startingNode;
    int lastSlash = 1; // try at least once
    while (lastSlash > 0) {
      final String traversalPath = traversalNode.getPath();
      lastSlash = traversalPath.lastIndexOf("/");
      if (traversalNode.hasProperty("sling:resourceType")) {
        final String resourceType = (String) traversalNode
            .getProperty("sling:resourceType");
        if ("sakai/pooled-content".equals(resourceType)
            || "sakai/group-home".equals(resourceType)) {
          // found the parent site node
          returnNode = traversalNode;
          break;
        }
      }
      // ~group/foo/bar
      if (lastSlash > 0) {
        final String parentPath = traversalPath.substring(0, lastSlash);
        traversalNode = session.getContentManager().get(parentPath);
        if (traversalNode == null) {
          break;
        }
      } else {
        // we have walked all the way up and not found a match
        break;
      }
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
   *          {@link HttpServletResponse#isCommitted()}. It will be logged in either case.
   *          Allows null value.
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
   * @param jcrRepository
   */
  protected void bindJcrRepository(SlingRepository jcrRepository) {
    this.jcrRepository = jcrRepository;
  }

  /**
   * @param jcrRepository
   */
  protected void unbindJcrRepository(SlingRepository jcrRepository) {
    this.jcrRepository = null;
  }
}
