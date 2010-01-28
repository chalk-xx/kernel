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

import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.ADMIN_CONFIG_PATH;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.DEBUG;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.DEBUG_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.FRAME_HEIGHT;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.FRAME_HEIGHT_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.LIS_PERSON_CONTACT_EMAIL_PRIMARY;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.LIS_PERSON_NAME_FAMILY;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.LIS_PERSON_NAME_FULL;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.LIS_PERSON_NAME_GIVEN;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.LTI_KEY;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.LTI_KEY_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.LTI_SECRET;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.LTI_SECRET_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.LTI_URL;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.LTI_URL_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.RELEASE_EMAIL;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.RELEASE_EMAIL_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.RELEASE_NAMES;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.RELEASE_NAMES_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.RELEASE_PRINCIPAL_NAME;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.RELEASE_PRINCIPAL_NAME_LOCK;
import static org.sakaiproject.kernel.api.basiclti.BasicLtiConstants.RESOURCE_LINK_ID;

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
  // TODO needs a better name
  private transient Map<String, String> availableSettings = null;

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
   */
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    availableSettings = new HashMap<String, String>(8);
    availableSettings.put(LTI_URL, LTI_URL_LOCK);
    availableSettings.put(LTI_SECRET, LTI_SECRET_LOCK);
    availableSettings.put(LTI_KEY, LTI_KEY_LOCK);
    availableSettings.put(FRAME_HEIGHT, FRAME_HEIGHT_LOCK);
    availableSettings.put(DEBUG, DEBUG_LOCK);
    availableSettings.put(RELEASE_NAMES, RELEASE_NAMES_LOCK);
    availableSettings.put(RELEASE_EMAIL, RELEASE_EMAIL_LOCK);
    availableSettings.put(RELEASE_PRINCIPAL_NAME, RELEASE_PRINCIPAL_NAME_LOCK);
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
        LOG.debug("No administrative settings found for virtual tool: "
            + vtoolId + ". No policy to apply.");
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
      for (final String setting : availableSettings.keySet()) {
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
            + " cannot be null", new IllegalArgumentException(LTI_URL
            + " cannot be null"), response);
        return;
      }
      // LTI_KEY
      final String ltiKey = effectiveSettings.get(LTI_KEY);
      if (ltiKey == null || "".equals(ltiKey)) {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, LTI_KEY
            + " cannot be null", new IllegalArgumentException(LTI_URL
            + " cannot be null"), response);
        return;
      }
      // FIXME should be using TUID
      // e.g. /sites/blargh/_widgets/id123456/basiclti.launch.html
      launchProps.setProperty(RESOURCE_LINK_ID, "tuid");

      final UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable az = userManager.getAuthorizable(session.getUserID());

      final boolean releasePrincipal = Boolean.parseBoolean(effectiveSettings
          .get(RELEASE_PRINCIPAL_NAME));
      if (releasePrincipal) {
        // TODO maybe needs to be more opaque?
        launchProps.setProperty("user_id", az.getID());
      }
      // FIXME need to pull roles from system
      launchProps.setProperty("roles", "Instructor,Student");

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
        final String email = az.getProperty("email")[0].getString();
        launchProps.setProperty(LIS_PERSON_CONTACT_EMAIL_PRIMARY, email);
      }

      // FIXME maybe should be parent or site?
      launchProps.setProperty("context_id", "context_id");

      // TODO how to determine site type?
      launchProps.setProperty("context_type", "CourseSection");
      // TODO how to resolve context title?
      launchProps.setProperty("context_title",
          "Design of Personal Environments");
      // TODO how to resolve context label?
      launchProps.setProperty("context_label", "SI182");
      // TODO how to determine user's locale?
      launchProps.setProperty("launch_presentation_locale", "en_US");
      for (final Object key : launchProps.keySet()) {
        LOG.info("launchProps: " + key + "=" + launchProps.get(key));
      }
      final Properties cleanProps = BasicLTIUtil.cleanupProperties(launchProps);
      for (final Object key : cleanProps.keySet()) {
        LOG.info("cleanProps: " + key + "=" + cleanProps.get(key));
      }
      // TODO externalize these parameters
      final Properties signedProperties = BasicLTIUtil.signProperties(
          cleanProps, ltiUrl, "POST", ltiKey, ltiSecret, "sakaiproject.org",
          "Sakai", "http://sakaiproject.org");
      final String extension = request.getRequestPathInfo().getExtension();
      if ("html".equalsIgnoreCase(extension)) { // return html
        final boolean debug = Boolean
            .parseBoolean(effectiveSettings.get(DEBUG));
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
        .get(availableSettings.get(setting)));
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
