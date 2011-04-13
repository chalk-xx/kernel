/*
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

import static org.sakaiproject.nakamura.api.connections.ConnectionState.ACCEPTED;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.INVITED;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.PENDING;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.connections.ConnectionConstants;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucketException;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucketService;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.BasicUserInfo;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "MeServlet", shortDescription = "Returns information about the current active user.", description = "Presents information about current user in JSON format.", bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/system/me"), methods = @ServiceMethod(name = "GET", description = "Get information about current user.", response = {
    @ServiceResponse(code = 200, description = "Request for information was successful. <br />"
        + "A JSON representation of the current user is returned. E.g. for an anonymous user:"
        + "<pre>{\"user\":\n"
        + "{\"anon\":true,\"subjects\":[],\"superUser\":false},\n"
        + "\"profile\":{\n"
        + "\"jcr:path\":\"/_user/a/an/anonymous/public/authprofile\",\n"
        + "\"jcr:name\":\"authprofile\",\n"
        + "\"rep:userId\":\"anonymous\",\n"
        + "\"sling:resourceType\":\"sakai/user-profile\",\n"
        + "\"jcr:uuid\":\"4af50bac-dca3-4517-81e0-1b83208f3f1d\",\n"
        + "\"jcr:mixinTypes\":[\"mix:referenceable\"],\n"
        + "\"path\":\"/a/an/anonymous\",\n"
        + "\"jcr:primaryType\":\"nt:unstructured\"}\n" + "}<pre>"),
    @ServiceResponse(code = 401, description = "Unauthorized: credentials provided were not acceptable to return information for."),
    @ServiceResponse(code = 500, description = "Unable to return information about current user.") }))
@SlingServlet(paths = { "/system/me" }, generateComponent = true, generateService = true, methods = { "GET" })
public class LiteMeServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -3786472219389695181L;
  private static final Logger LOG = LoggerFactory.getLogger(LiteMeServlet.class);
  private static final String LOCALE_FIELD = "locale";
  private static final String TIMEZONE_FIELD = "timezone";

  @Reference
  protected transient LiteMessagingService messagingService;

  @Reference
  protected transient ConnectionManager connectionManager;

  @Reference
  protected transient ProfileService profileService;

  @Reference
  private MessageBucketService messageBucketService;

  @Reference
  SolrSearchServiceFactory searchServiceFactory;

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
      javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(javax.jcr.Session.class);
      final Session session = StorageClientUtils.adaptToSession(request
          .getResourceResolver().adaptTo(javax.jcr.Session.class));
      if (session == null) {
        LOG.error("########## session is null");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
        "Access denied error.");
        return;
      }
      AuthorizableManager um = session.getAuthorizableManager();
      Authorizable au = um.findAuthorizable(session.getUserId());
      PrintWriter w = response.getWriter();
      ExtendedJSONWriter writer = new ExtendedJSONWriter(w);
      writer.object();
      // User info
      writer.key("user");
      writeUserJSON(writer, session, au, request);

      try {
        String messageBucketUrl = messageBucketService.getBucketUrl(request, "default");
        if ( messageBucketUrl != null) {
          writer.key("eventbus");
          writer.value(messageBucketUrl);
        }
      } catch ( MessageBucketException e) {
        LOG.warn("Failed to create message bucket URL {} "+e.getMessage());
        LOG.debug("Failed to create message bucket URL {} "+e.getMessage(),e);

      }

      // Dump this user his info
      writer.key("profile");
      ValueMap profile = profileService.getProfileMap(au,jcrSession);
      writer.valueMap(profile);

      // Dump this user his number of unread messages.
      writer.key("messages");
      writeMessageCounts(writer, session, au, request);

      // Dump this user his number of contacts.
      writer.key("contacts");
      writeContactCounts(writer, session, au, request);

      // Dump the groups for this user.
      writer.key("groups");
      writeGroups(writer, session, au, jcrSession);

      writer.endObject();
    } catch (JSONException e) {
      LOG.error("Failed to create proper JSON response in /system/me", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to create proper JSON response.");
    } catch (StorageClientException e) {
      LOG.error("Failed to get a user his profile node in /system/me", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Sparse storage client error.");
    } catch (AccessDeniedException e) {
      LOG.error("Failed to get a user his profile node in /system/me", e);
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "Access denied error.");
    } catch (RepositoryException e) {
      LOG.error("Failed to get a user his profile node in /system/me", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Sparse storage client error.");
    } catch (MessagingException e) {
      LOG.error("Failed to get a user his message counts in /system/me", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Messaging error.");
    } catch (SolrSearchException e) {
      LOG.error("Failed to execute a Solr search in /system/me", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Solr search error.");
    }

  }

  /**
   * @param writer
   * @param session
   * @param au
   * @throws JSONException
   * @throws StorageClientException 
   * @throws AccessDeniedException 
   * @throws RepositoryException 
   */
  protected void writeGroups(ExtendedJSONWriter writer, Session session, Authorizable au, javax.jcr.Session jcrSession)
      throws JSONException, StorageClientException, AccessDeniedException, RepositoryException {
    AuthorizableManager authorizableManager = session.getAuthorizableManager();
    writer.array();
    if (!UserConstants.ANON_USERID.equals(au.getId())) {
      // It might be better to just use au.declaredMemberOf() .
      // au.memberOf will fetch ALL the groups this user is a member of, including
      // indirect ones.
      String[] principals = au.getPrincipals();
      for(String principal : principals) {
        Authorizable group = authorizableManager.findAuthorizable(principal);
        if (group == null || !(group instanceof Group) || group.getId().equals(Group.EVERYONE)) {
          // we don't want the "everyone" group in this feed
          continue;
        }
        if (group.hasProperty("sakai:managed-group")) {
          // fetch the group that the manager group manages
          group = authorizableManager.findAuthorizable((String) group.getProperty("sakai:managed-group"));
          if (group == null || !(group instanceof Group)) {
            continue;
          }
        }
        BasicUserInfo basicUserInfo = new BasicUserInfo();
        ValueMap groupProfile = new ValueMapDecorator(basicUserInfo.getProperties(group));
        if (groupProfile != null) {
          writer.valueMap(groupProfile);
        }
      }
    }
    writer.endArray();
  }

  /**
   * Writes a JSON Object that contains the number of contacts for a user split up in
   * PENDING, ACCEPTED.
   *
   * @param writer
   * @param session
   * @param au
   * @throws JSONException
   * @throws SolrSearchException
   * @throws RepositoryException
   */
  protected void writeContactCounts(ExtendedJSONWriter writer, Session session,
      Authorizable au, SlingHttpServletRequest request) throws JSONException, SolrSearchException {
    writer.object();

    // We don't do queries for anonymous users. (Possible ddos hole).
    String userID = au.getId();
    if (UserConstants.ANON_USERID.equals(userID)) {
      writer.endObject();
      return;
    }

    // Get the path to the store for this user.
    Map<String, Integer> contacts = new HashMap<String, Integer>();
    contacts.put(ACCEPTED.toString().toLowerCase(), 0);
    contacts.put(INVITED.toString().toLowerCase(), 0);
    contacts.put(PENDING.toString().toLowerCase(), 0);
    try {
      // This could just use ConnectionUtils.getConnectionPathBase, but that util class is
      // in the private package unfortunately.
      String store = LitePersonalUtils.getHomePath(userID) + "/"
          + ConnectionConstants.CONTACT_STORE_NAME;
      store = ISO9075.encodePath(store);
      String queryString = "path:" + ClientUtils.escapeQueryChars(store) + " AND resourceType:sakai/contact AND state:(ACCEPTED OR INVITED OR PENDING)";
      Query query = new Query(queryString, null);
      LOG.debug("Submitting Query {} ", query);
      SolrSearchResultSet resultSet = searchServiceFactory.getSearchResultSet(
          request, query, false);
      Iterator<Result> resultIterator = resultSet.getResultSetIterator();
      while (resultIterator.hasNext()) {
        Result contact = resultIterator.next();
        if (contact.getProperties().containsKey("state")) {
          String state = (String) contact.getProperties().get("state").iterator().next();
          int count = 0;
          if (contacts.containsKey(state)) {
            count = contacts.get(state);
          }
          contacts.put(state, count + 1);
        }
      }
    } finally {
      for (Entry<String, Integer> entry : contacts.entrySet()) {
        writer.key(entry.getKey());
        writer.value(entry.getValue());
      }
    }
    writer.endObject();
  }

  /**
   * Writes a JSON Object that contains the unread messages for a user.
   *
   * @param writer
   *          The writer
   * @param session
   *          A JCR session to perform queries with. This session needs read access on the
   *          authorizable's message box.
   * @param au
   *          An authorizable to look up the messages for.
   * @throws JSONException
   * @throws RepositoryException
   * @throws MessagingException
   * @throws SolrSearchException
   */
  protected void writeMessageCounts(ExtendedJSONWriter writer, Session session,
      Authorizable au, SlingHttpServletRequest request) throws JSONException, MessagingException, SolrSearchException {
    writer.object();
    writer.key("unread");

    // We don't do queries for anonymous users. (Possible ddos hole).
    String userID = au.getId();
    if (UserConstants.ANON_USERID.equals(userID)) {
      writer.value(0);
      writer.endObject();
      return;
    }

    long count = 0;
    try {
      String store = messagingService.getFullPathToStore(au.getId(), session);
      store = ISO9075.encodePath(store);
      store = store.substring(0, store.length() - 1);
      String queryString = "path:" + ClientUtils.escapeQueryChars(store) + " AND resourceType:sakai/message AND type:internal AND messagebox:inbox AND read:false";
      Query query = new Query(queryString, null);
      LOG.debug("Submitting Query {} ", query);
      SolrSearchResultSet resultSet = searchServiceFactory.getSearchResultSet(
          request, query, false);
      Iterator<Result> resultIterator = resultSet.getResultSetIterator();
      while (resultIterator.hasNext()) {
        count++;
        resultIterator.next();
      }
    } finally {
      writer.value(count);
    }
    writer.endObject();
  }

  /**
   *
   * @param write
   * @param session
   * @param authorizable
   * @throws RepositoryException
   * @throws JSONException
   * @throws StorageClientException
   */
  protected void writeUserJSON(ExtendedJSONWriter write, Session session,
      Authorizable authorizable, SlingHttpServletRequest request)
      throws JSONException, StorageClientException {

    String user = session.getUserId();
    boolean isAnonymous = (UserConstants.ANON_USERID.equals(user));
    if (isAnonymous || authorizable == null) {

      write.object();
      write.key("anon").value(true);
      write.key("subjects");
      write.array();
      write.endArray();
      write.key("superUser");
      write.value(false);
      write.endObject();
    } else {
      Set<String> subjects = getSubjects(authorizable, session.getAuthorizableManager());
      Map<String, Object> properties = getProperties(authorizable);

      write.object();
      writeGeneralInfo(write, authorizable, subjects, properties);
      writeLocale(write, properties, request);
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
  protected void writeLocale(ExtendedJSONWriter write, Map<String, Object> properties,
      SlingHttpServletRequest request) throws JSONException {

    /* Get the correct locale */
    Locale l = request.getLocale();
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
      Set<String> subjects, Map<String, Object> properties) throws JSONException {

    write.key("userid").value(user.getId());
    write.key("userStoragePrefix");
    // For backwards compatibility we substring the first slash out and append one at the
    // back.
    write.value("~" + user.getId() + "/");
    write.key("userProfilePath");
    write.value(LitePersonalUtils.getProfilePath(user.getId()));
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
   * @param authorizableManager
   *          The {@link AuthorizableManager authorizableManager} that can be used to retrieve
   *          the group membership.
   * @return All the names of the {@link Group groups} a user is a member of.
   * @throws RepositoryException
   */
  protected Set<String> getSubjects(Authorizable authorizable,
      AuthorizableManager authorizableManager) {
    Set<String> subjects = new HashSet<String>();
    if (authorizable != null) {
      String principal = authorizable.getId();
      if (principal != null) {
        Iterator<Group> it = authorizable.memberOf(authorizableManager);
        while (it.hasNext()) {
          Group aGroup = it.next();
          if (!aGroup.getId().equals(Group.EVERYONE)) {
            subjects.add(aGroup.getId());
          }
        }
      }
    }
    return subjects;
  }

  private Map<String, Object> getProperties(Authorizable authorizable) {
    Map<String, Object> result = new HashMap<String, Object>();
    if (authorizable != null) {
      for (String propName : authorizable.getSafeProperties().keySet()) {
        if (propName.startsWith("rep:"))
          continue;
        Object o = authorizable.getProperty(propName);
        if ( o instanceof Object[] ) {
          Object[] values = (Object[]) o;
          switch (values.length) {
          case 0:
            continue;
          case 1:
            result.put(propName, values[0]);
            break;
          default: {
            StringBuilder valueString = new StringBuilder("");
            for (int i = 0; i < values.length; i++) {
              valueString.append("," + values[i]);
            }
            result.put(propName, valueString.toString().substring(1));
          }
          }
        } else {
          result.put(propName, o);
        }
      }
    }
    return result;
  }
}
