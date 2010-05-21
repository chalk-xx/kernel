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
package org.sakaiproject.nakamura.meservice;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(
    name = "MeServlet",
    shortDescription = "Returns information about the current active user.",
    description = "Presents information about current user in JSON format.",
    bindings = @ServiceBinding(
        type = BindingType.PATH,
        bindings = "/system/me"
    ),
    methods = @ServiceMethod(
        name = "GET",
        description = "Get information about current user.",
        response = {@ServiceResponse(
            code = 200,
            description = "Request for information was successful. <br />" +
                "A JSON representation of the current user is returned. E.g. for an anonymous user:" +
                "<pre>{\"user\":\n" +
                "{\"anon\":true,\"subjects\":[],\"superUser\":false},\n" +
                "\"profile\":{\n" +
                "\"jcr:path\":\"/_user/a/an/anonymous/public/authprofile\",\n" +
                "\"jcr:name\":\"authprofile\",\n" +
                "\"rep:userId\":\"anonymous\",\n" +
                "\"sling:resourceType\":\"sakai/user-profile\",\n" +
                "\"jcr:uuid\":\"4af50bac-dca3-4517-81e0-1b83208f3f1d\",\n" +
                "\"jcr:mixinTypes\":[\"mix:referenceable\"],\n" +
                "\"path\":\"/a/an/anonymous\",\n" +
                "\"jcr:primaryType\":\"nt:unstructured\"}\n" +
                "}<pre>"
          ),
          @ServiceResponse(
            code = 401,
            description = "Unauthorized: credentials provided were not acceptable to return information for."
          ),
          @ServiceResponse(
            code = 500,
            description = "Unable to return information about current user."
          )
        }
    )
)
@SlingServlet(paths = { "/system/me" }, generateComponent = true, generateService = true, methods = { "GET" })
public class MeServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -3786472219389695181L;
  private static final Logger LOG = LoggerFactory.getLogger(MeServlet.class);
  private static final String LOCALE_FIELD = "locale";
  private static final String TIMEZONE_FIELD = "timezone";

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      Session session = request.getResourceResolver().adaptTo(Session.class);
      UserManager um = AccessControlUtil.getUserManager(session);
      Authorizable au = um.getAuthorizable(session.getUserID());
      PrintWriter w = response.getWriter();
      ExtendedJSONWriter writer = new ExtendedJSONWriter(w);
      writer.object();
      // User info
      writer.key("user");
      writeUserJSON(writer, session, au);

      // Dump this user his info
      writer.key("profile");
      String profilePath = PersonalUtils.getProfilePath(au);
      Node profileNode = (Node) session.getItem(profilePath);
      ExtendedJSONWriter.writeNodeTreeToWriter(writer, profileNode);

      writer.endObject();
    } catch (JSONException e) {
      LOG.error("Failed to create proper JSON response in /system/me", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to create proper JSON response.");
    } catch (RepositoryException e) {
      LOG.error("Failed to get a user his profile node in /system/me", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to get the profile node.");
    }

  }

  /**
   * 
   * @param write
   * @param session
   * @param authorizable
   * @throws RepositoryException
   * @throws JSONException
   */
  protected void writeUserJSON(ExtendedJSONWriter write, Session session,
      Authorizable authorizable) throws RepositoryException, JSONException {

    String user = session.getUserID();
    boolean isAnonymous = (UserConstants.ANON_USERID.equals(user));
    if (isAnonymous) {

      write.object();
      write.key("anon").value(true);
      write.key("subjects");
      write.array();
      write.endArray();
      write.key("superUser");
      write.value(false);
      write.endObject();
    } else {
      PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
      Set<String> subjects = getSubjects(authorizable, principalManager);
      Map<String, Object> properties = getProperties(authorizable);

      write.object();
      writeGeneralInfo(write, authorizable, subjects, properties);
      writeLocale(write, properties);
      write.endObject();
    }

  }

  /**
   * Writes the local and timezone information.
   * 
   * @param write
   * @param properties
   * @throws JSONException
   */
  protected void writeLocale(ExtendedJSONWriter write, Map<String, Object> properties)
      throws JSONException {

    /* Get the correct locale */
    Locale l = Locale.getDefault();
    if (properties.containsKey(LOCALE_FIELD)) {
      String locale[] = properties.get(LOCALE_FIELD).toString().split("_");
      if (locale.length == 2) {
        l = new Locale(locale[0], locale[1]);
      }
    }

    /* Get the correct time zone */
    TimeZone tz = TimeZone.getDefault();
    if (properties.containsKey(TIMEZONE_FIELD)) {
      String timezone = properties.get(TIMEZONE_FIELD).toString();
      tz = TimeZone.getTimeZone(timezone);
    }
    int offset = tz.getRawOffset() + tz.getDSTSavings();

    /* Add the locale information into the output */
    write.key("locale");
    write.object();
    write.key("country");
    write.value(l.getCountry());
    write.key("displayCountry");
    write.value(l.getDisplayCountry(l));
    write.key("displayLanguage");
    write.value(l.getDisplayLanguage(l));
    write.key("displayName");
    write.value(l.getDisplayName(l));
    write.key("displayVariant");
    write.value(l.getDisplayVariant(l));
    write.key("ISO3Country");
    write.value(l.getISO3Country());
    write.key("ISO3Language");
    write.value(l.getISO3Language());
    write.key("language");
    write.value(l.getLanguage());
    write.key("variant");
    write.value(l.getVariant());

    /* Add the timezone information into the output */
    write.key("timezone");
    write.object();
    write.key("name");
    write.value(tz.getID());
    write.key("GMT");
    write.value(offset / 3600000);
    write.endObject();

    write.endObject();
  }

  /**
   * Writes the general information about a user such as the userid, storagePrefix, wether
   * he is a superUser or not..
   * 
   * @param write
   * @param user
   * @param session
   * @throws JSONException
   * @throws RepositoryException
   */
  protected void writeGeneralInfo(ExtendedJSONWriter write, Authorizable user,
      Set<String> subjects, Map<String, Object> properties) throws JSONException,
      RepositoryException {

    write.key("userid").value(user.getID());
    write.key("userStoragePrefix");
    // For backwards compatibility we substring the first slash out and append one at the back.
    write.value(PathUtils.getSubPath(user).substring(1) + "/");
    write.key("userProfilePath");
    write.value(PersonalUtils.getProfilePath(user));
    write.key("superUser");
    write.value(subjects.contains("administrators"));
    write.key("properties");
    ValueMap jsonProperties = new ValueMapDecorator(properties);
    write.valueMap(jsonProperties);
    write.key("subjects");
    write.array();
    for (String groupName : subjects) {
      write.value(groupName);
    }
    write.endArray();
  }

  /**
   * All the names of the {@link Group groups} a user is a member of.
   * 
   * @param authorizable
   *          The {@link Authorizable authorizable} that represents the user.
   * @param principalManager
   *          The {@link PrincipalManager principalManager} that can be used to retrieve
   *          the group membership.
   * @return All the names of the {@link Group groups} a user is a member of.
   * @throws RepositoryException
   */
  protected Set<String> getSubjects(Authorizable authorizable,
      PrincipalManager principalManager) throws RepositoryException {
    Set<String> subjects = new HashSet<String>();
    if (authorizable != null) {
      Principal principal = authorizable.getPrincipal();
      if (principal != null) {
        PrincipalIterator it = principalManager.getGroupMembership(principal);
        while (it.hasNext()) {
          subjects.add(it.nextPrincipal().getName());
        }
      }
    }
    return subjects;
  }

  private Map<String, Object> getProperties(Authorizable authorizable)
      throws RepositoryException {
    Map<String, Object> result = new HashMap<String, Object>();
    if (authorizable != null) {
      for (Iterator<String> it = authorizable.getPropertyNames(); it.hasNext();) {
        String propName = it.next();
        if (propName.startsWith("rep:"))
          continue;
        Value[] values = authorizable.getProperty(propName);
        switch (values.length) {
        case 0:
          continue;
        case 1:
          result.put(propName, values[0].getString());
          break;
        default: {
          StringBuilder valueString = new StringBuilder("");
          for (int i = 0; i < values.length; i++) {
            valueString.append("," + values[i].getString());
          }
          result.put(propName, valueString.toString().substring(1));
        }
        }
      }
    }
    return result;
  }
}
