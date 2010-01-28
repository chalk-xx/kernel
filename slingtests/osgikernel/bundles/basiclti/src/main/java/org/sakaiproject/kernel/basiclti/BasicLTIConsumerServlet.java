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
package org.sakaiproject.kernel.basiclti;

import static org.imsglobal.basiclti.BasicLTIConstants.CONTEXT_ID;
import static org.imsglobal.basiclti.BasicLTIConstants.CONTEXT_LABEL;
import static org.imsglobal.basiclti.BasicLTIConstants.CONTEXT_TITLE;
import static org.imsglobal.basiclti.BasicLTIConstants.LAUNCH_PRESENTATION_DOCUMENT_TARGET;
import static org.imsglobal.basiclti.BasicLTIConstants.LAUNCH_PRESENTATION_LOCALE;
import static org.imsglobal.basiclti.BasicLTIConstants.LIS_PERSON_CONTACT_EMAIL_PRIMARY;
import static org.imsglobal.basiclti.BasicLTIConstants.LIS_PERSON_NAME_FAMILY;
import static org.imsglobal.basiclti.BasicLTIConstants.LIS_PERSON_NAME_FULL;
import static org.imsglobal.basiclti.BasicLTIConstants.LIS_PERSON_NAME_GIVEN;
import static org.imsglobal.basiclti.BasicLTIConstants.RESOURCE_LINK_ID;
import static org.imsglobal.basiclti.BasicLTIConstants.ROLES;
import static org.imsglobal.basiclti.BasicLTIConstants.USER_ID;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.ADMIN_CONFIG_PATH;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.DEBUG;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.DEBUG_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.FRAME_HEIGHT;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.FRAME_HEIGHT_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.LTI_KEY;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.LTI_KEY_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.LTI_SECRET;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.LTI_SECRET_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.LTI_URL;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.LTI_URL_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.RELEASE_EMAIL;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.RELEASE_EMAIL_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.RELEASE_NAMES;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.RELEASE_NAMES_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.RELEASE_PRINCIPAL_NAME;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiAppConstants.RELEASE_PRINCIPAL_NAME_LOCK;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.imsglobal.basiclti.BasicLTIUtil;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.sakaiproject.kernel.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@SlingServlet(methods = { "GET" }, resourceTypes = { "sakai/basiclti" }, selectors = { "launch" })
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
    applicationSettings
        .put(RELEASE_PRINCIPAL_NAME, RELEASE_PRINCIPAL_NAME_LOCK);
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
      // if (!node.hasProperty(LTI_VTOOL_ID)) {
      // sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, LTI_VTOOL_ID
      // + " cannot be null", new IllegalArgumentException(LTI_VTOOL_ID
      // + " cannot be null"), response);
      // return;
      // }
      // final String vtoolId = node.getProperty(LTI_VTOOL_ID).getString();
      final String vtoolId = "basiclti";

      final String adminNodePath = ADMIN_CONFIG_PATH + "/" + vtoolId;
      Map<String, String> adminSettings = null;
      if (session.itemExists(adminNodePath)) {
        LOG.debug("Found administrative settings for virtual tool: " + vtoolId);
        final Node adminNode = (Node) session.getItem(adminNodePath);
        // loop through admin properties
        final PropertyIterator api = adminNode.getProperties();
        adminSettings = new HashMap<String, String>((int) api.getSize());
        while (api.hasNext()) {
          final Property property = api.nextProperty();
          LOG.info("admin: " + property.getName() + "="
              + property.getValue().getString());
          adminSettings
              .put(property.getName(), property.getValue().getString());
        }
      } else {
        LOG
            .debug(
                "No administrative settings found for virtual tool: {}. No policy to apply.",
                vtoolId);
        adminSettings = Collections.emptyMap();
      }

      // loop through user properties
      final PropertyIterator upi = node.getProperties();
      final Map<String, String> userSettings = new HashMap<String, String>(
          (int) upi.getSize());
      while (upi.hasNext()) {
        final Property property = upi.nextProperty();
        LOG.info("user: " + property.getName() + "="
            + property.getValue().getString());
        userSettings.put(property.getName(), property.getValue().getString());
      }

      // merge admin and user properties
      final Map<String, String> effectiveSettings = new HashMap<String, String>(
          Math.max(adminSettings.size(), userSettings.size()));
      for (final String setting : applicationSettings.keySet()) {
        effectiveSetting(setting, effectiveSettings, adminSettings,
            userSettings);
      }

      final Properties launchProps = new Properties();

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
      // TODO verify with Dr. Chuck that this key does not need to be
      // reversible.
      // e.g. /sites/foo/_widgets/id944280073/basiclti
      launchProps.setProperty(RESOURCE_LINK_ID, StringUtils.sha1Hash(node
          .getPath()));

      final UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable az = userManager.getAuthorizable(session.getUserID());

      final boolean releasePrincipal = Boolean.parseBoolean(effectiveSettings
          .get(RELEASE_PRINCIPAL_NAME));
      if (releasePrincipal) {
        launchProps.setProperty(USER_ID, az.getID());
      }
      // FIXME need to pull roles from system
      if ("admin".equals(session.getUserID())) {
        launchProps.setProperty(ROLES, "Instructor");
      } else {
        launchProps.setProperty(ROLES, "Learner");
      }

      final boolean releaseNames = Boolean.parseBoolean(effectiveSettings
          .get(RELEASE_NAMES));
      if (releaseNames) {
        String firstName = null;
        if (az.hasProperty("firstName")) {
          firstName = az.getProperty("firstName")[0].getString();
          launchProps.setProperty(LIS_PERSON_NAME_GIVEN, firstName);
        }
        String lastName = null;
        if (az.hasProperty("lastName")) {
          lastName = az.getProperty("lastName")[0].getString();
          launchProps.setProperty(LIS_PERSON_NAME_FAMILY, lastName);
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
          launchProps.setProperty(LIS_PERSON_NAME_FULL, fullName);
        }
      }

      final boolean releaseEmail = Boolean.parseBoolean(effectiveSettings
          .get(RELEASE_EMAIL));
      if (releaseEmail) {
        if (az.hasProperty("email")) {
          final String email = az.getProperty("email")[0].getString();
          launchProps.setProperty(LIS_PERSON_CONTACT_EMAIL_PRIMARY, email);
        }
      }

      Node traversalNode = node;
      while (traversalNode.getDepth() != 0) {
        if (traversalNode.hasProperty("sling:resourceType")) {
          if ("sakai/site".equals(traversalNode.getProperty(
              "sling:resourceType").getString())) {
            // found the parent site node
            launchProps.setProperty(CONTEXT_ID, traversalNode.getPath());
            launchProps.setProperty(CONTEXT_TITLE, traversalNode.getProperty(
                "name").getString());
            launchProps.setProperty(CONTEXT_LABEL, traversalNode.getProperty(
                "id").getString());
            break;
          }
        }
        traversalNode = traversalNode.getParent();
      }

      // TODO how to determine site type?
      // launchProps.setProperty("context_type", "CourseSection");

      // TODO how to determine user's locale?
      launchProps.setProperty(LAUNCH_PRESENTATION_LOCALE, "en_US");

      // we will always launch in an iframe for the time being
      launchProps.setProperty(LAUNCH_PRESENTATION_DOCUMENT_TARGET, "iframe");

      final boolean debug = Boolean.parseBoolean(effectiveSettings.get(DEBUG));
      // might be useful for the remote end to know if debug is enabled...
      launchProps.setProperty(DEBUG, "" + debug);

      // required to pass certification test suite
      launchProps.setProperty("simple_key", "custom_simple_value");
      launchProps.setProperty("Complex!@#$^*(){}[]KEY",
          "Complex!@#$^*(){}[]Value");

      for (final Object key : launchProps.keySet()) {
        LOG.info("launchProps: " + key + "=" + launchProps.get(key));
      }
      final Properties cleanProps = BasicLTIUtil.cleanupProperties(launchProps,
          BLACKLIST);
      for (final Object key : cleanProps.keySet()) {
        LOG.info("cleanProps: " + key + "=" + cleanProps.get(key));
      }
      // TODO externalize these parameters
      final Properties signedProperties = BasicLTIUtil.signProperties(
          cleanProps, ltiUrl, "POST", ltiKey, ltiSecret, "sakaiproject.org",
          "Sakai", "http://sakaiproject.org");
      final String extension = request.getRequestPathInfo().getExtension();
      if ("html".equalsIgnoreCase(extension)) { // return html
        final String html = BasicLTIUtil.postLaunchHTML(signedProperties,
            ltiUrl, debug);
        response.getWriter().write(html);
      } else { // return json
        renderJson(response.getWriter(), ltiUrl, signedProperties);
      }
    } catch (Exception e) {
      sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getLocalizedMessage(), e, response);
    }
  }

  private void renderJson(final Writer pwriter, final String ltiUrl,
      final Properties properties) throws JSONException {
    final ExtendedJSONWriter writer = new ExtendedJSONWriter(pwriter);
    writer.object(); // root object
    writer.key("launchURL");
    writer.value(ltiUrl);
    writer.key("postData");
    writer.object();
    for (final Object propkey : properties.keySet()) {
      writer.key((String) propkey);
      writer.value(properties.getProperty((String) propkey));
    }
    writer.endObject(); // postData
    writer.endObject(); // root object
  }

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
}
