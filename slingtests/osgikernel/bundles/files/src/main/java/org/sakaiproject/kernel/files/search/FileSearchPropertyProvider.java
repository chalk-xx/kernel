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
package org.sakaiproject.kernel.files.search;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.kernel.api.connections.ConnectionManager;
import org.sakaiproject.kernel.api.connections.ConnectionState;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.search.SearchPropertyProvider;
import org.sakaiproject.kernel.api.site.SiteException;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.util.JcrUtils;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;

/**
 * Provides properties to process the search
 * 
 */
@Component(immediate = true, label = "FileSearchPropertyProvider", description = "Property provider for file searches")
@Service(value = SearchPropertyProvider.class)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value = "Files") })
public class FileSearchPropertyProvider implements SearchPropertyProvider {

  @Reference
  private SiteService siteService;

  @Reference
  private ConnectionManager connectionManager;

  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {

    Session session = request.getResourceResolver().adaptTo(Session.class);
    String user = request.getRemoteUser();

    // Set the userid.
    propertiesMap.put("_me", user);

    // Set the contacts.
    propertiesMap.put("_mycontacts", getMyContacts(user));

    // Set all mysites.
    propertiesMap.put("_mysites", getMySites(session, user));

    // Set all my bookmarks
    propertiesMap.put("_mybookmarks", getMyBookmarks(session, user));

    // request specific.
    // Sorting order
    propertiesMap.put("_order", doSortOrder(request));

    // Filter by site
    propertiesMap.put("_usedin", doUsedIn(request));

    // Filter by tags
    propertiesMap.put("_tags", doTags(request));

    // ###########################
    // TODO /var/search/files/resources.json should be deleted.
    // Resource types (used in resources.json).
    // ###########################
    RequestParameter resourceParam = request.getRequestParameter("resource");
    String resourceTypes = "@sling:resourceType=\"sakai/link\" or @sling:resourceType=\"sakai/folder\"";
    if (resourceParam != null) {
      String type = resourceParam.getString();
      if ("link".equals(type)) {
        resourceTypes = "@sling:resourceType=\"sakai/link\"";
      } else if ("folder".equals(type)) {
        resourceTypes = "@sling:resourceType=\"sakai/folder\"";
      }
    }
    propertiesMap.put("_resourceTypes", resourceTypes);

    // ###########################
    // Resource types (used in files.json)
    // ###########################
    String types[] = request.getParameterValues("type");
    String typesWhere = "";
    String search = getSearchValue(request);
    if (types != null && types.length > 0) {
      StringBuilder sb = new StringBuilder("");
      sb.append("(");
      for (String s : types) {
        if (s.equals("sakai/file")) {
          // Every sakai/file with search in it's filename or content.
          sb.append("(sling:resourceType=\"sakai/file\" and (jcr:contains(.,\"").append(
              search).append("\") or jcr:contains(jcr:content,\"").append(search).append(
              "\"))) or ");
        } else if (s.equals("sakai/link")) {
          // Every link that has the search param in the filename.
          sb.append("(sling:resourceType=\"sakai/link\" and jcr:contains(., \"").append(
              search).append("\")) or ");
        } else {
          // Every other file that contains the search param in it's filename or in it's
          // content.
          sb.append("jcr:contains(.,\"");
          sb.append(search);
          sb.append("\") or ");
        }
      }

      typesWhere = sb.toString();
      typesWhere = typesWhere.substring(0, typesWhere.length() - 4);
      typesWhere += ")";
    } else {
      // Default is sakai/files
      typesWhere = "(sling:resourceType=\"sakai/file\" and jcr:contains(.,\"*" + search
          + "*\"))";
    }
    propertiesMap.put("_typesWhere", typesWhere);
  }

  /**
   * Filter files by looking up where they are used.
   * 
   * @param request
   * @return
   */
  private String doUsedIn(SlingHttpServletRequest request) {
    String usedin[] = request.getParameterValues("usedin");
    if (usedin != null && usedin.length > 0) {
      StringBuilder sb = new StringBuilder();

      for (String u : usedin) {
        sb.append("jcr:contains(@sakai:linkpaths,\"").append(u).append("\") or ");
      }

      String usedinClause = sb.toString();
      int i = usedinClause.lastIndexOf(" or ");
      if (i > -1) {
        usedinClause = usedinClause.substring(0, i);
      }
      if (usedinClause.length() > 0) {
        usedinClause = " and (" + usedinClause + ")";
        return usedinClause;
      }
    }
    return "";
  }

  private String getSearchValue(SlingHttpServletRequest request) {
    RequestParameter searchParam = request.getRequestParameter("search");
    String search = "*";
    if (searchParam != null) {
      search = escapeString(searchParam.getString(), Query.XPATH);
      if (search.equals(""))
        search = "*";
    }
    return search;
  }

  /**
   * Returns default sort order.
   * 
   * @param request
   * @return
   */
  private String doSortOrder(SlingHttpServletRequest request) {
    RequestParameter sortOnParam = request.getRequestParameter("sortOn");
    RequestParameter sortOrderParam = request.getRequestParameter("sortOrder");
    String sortOn = "sakai:filename";
    String sortOrder = "ascending";
    if (sortOrderParam != null
        && (sortOrderParam.getString().equals("ascending") || sortOrderParam.getString()
            .equals("descending"))) {
      sortOrder = sortOrderParam.getString();
    }
    if (sortOnParam != null) {
      sortOn = sortOnParam.getString();
    }
    return " order by @" + sortOn + " " + sortOrder;
  }

  /**
   * Gets a clause for a query by looking at the sakai:tags request parameter.
   * 
   * @param request
   * @return
   */
  private String doTags(SlingHttpServletRequest request) {
    String[] tags = request.getParameterValues("sakai:tags");
    if (tags != null) {
      StringBuilder sb = new StringBuilder();

      for (String t : tags) {
        sb.append("@sakai:tags=\"");
        sb.append(t);
        sb.append("\" and ");
      }
      String tagClause = sb.toString();
      int i = tagClause.lastIndexOf(" and ");
      if (i > -1) {
        tagClause = tagClause.substring(0, i);
      }
      if (tagClause.length() > 0) {
        tagClause = " and (" + tagClause + ")";
        return tagClause;
      }
    }
    return "";
  }

  /**
   * Gets the user his bookmarks in a string that can be used in a query.
   * 
   * @param session
   * @param user
   * @return
   */
  private String getMyBookmarks(Session session, String user) {
    String userPath = PersonalUtils.getPrivatePath(user, "");
    String bookmarksPath = userPath + "/mybookmarks";
    String ids = "and (@sakai:id=\"somenoneexistingid\")";
    try {
      if (session.itemExists(bookmarksPath)) {
        Node node = (Node) session.getItem(bookmarksPath);
        Value[] values = JcrUtils.getValues(node, "files");

        StringBuilder sb = new StringBuilder("");

        for (Value val : values) {
          sb.append("@sakai:id=\"").append(val.getString()).append("\" or ");
        }

        String bookmarks = sb.toString();
        int i = bookmarks.lastIndexOf(" or ");
        if (i > -1) {
          bookmarks = bookmarks.substring(0, i);
        }
        if (bookmarks.length() > 0) {
          bookmarks = " and (" + bookmarks + ")";
          return bookmarks;
        }
      }
    } catch (RepositoryException e) {
      // Well, we failed..
      e.printStackTrace();
    }
    return ids;
  }

  /**
   * Get a user his sites.
   * 
   * @param session
   * @param user
   * @return
   */
  @SuppressWarnings(justification="siteService is OSGi managed", value={"NP_UNWRITTEN_FIELD", "UWF_UNWRITTEN_FIELD"})
  private String getMySites(Session session, String user) {
    try {
      StringBuilder sb = new StringBuilder();
      Map<String, List<Group>> membership = siteService.getMembership(session, user);
      for (Entry<String, List<Group>> site : membership.entrySet()) {
        sb.append("@sakai:sites=\"").append(site.getKey()).append("\" or ");
      }
      String sites = sb.toString();
      int i = sites.lastIndexOf(" or ");
      if (i > -1) {
        sites = sites.substring(0, i);
      }
      if (sites.length() > 0) {
        sites = " and (" + sites + ")";
      }
      return sites;

    } catch (SiteException e1) {
      return "";
    }
  }

  /**
   * Escape a parameter string so it doesn't contain anything that might break the query.
   * 
   * @param value
   * @param queryLanguage
   * @return
   */
  private String escapeString(String value, String queryLanguage) {
    String escaped = null;
    if (value != null) {
      if (queryLanguage.equals(Query.XPATH) || queryLanguage.equals(Query.SQL)) {
        // See JSR-170 spec v1.0, Sec. 6.6.4.9 and 6.6.5.2
        escaped = value.replaceAll("\\\\(?![-\"])", "\\\\\\\\").replaceAll("'", "\\\\'")
            .replaceAll("'", "''");
      }
    }
    return escaped;
  }

  /**
   * Get a string of all the connected users.
   * 
   * @param user
   *          The user to get the contacts for.
   * @return and (@sakai:user=\"simon\" or @sakai:user=\"ieb\")
   */
  @SuppressWarnings(justification="connectionManager is OSGi managed", value={"NP_UNWRITTEN_FIELD", "UWF_UNWRITTEN_FIELD"})
  private String getMyContacts(String user) {
    List<String> connectedUsers = connectionManager.getConnectedUsers(user,
        ConnectionState.ACCEPTED);
    StringBuilder sb = new StringBuilder();
    for (String u : connectedUsers) {
      sb.append("@sakai:user=\"").append(u).append("\" or ");
    }
    String usersClause = sb.toString();
    int i = usersClause.lastIndexOf(" or ");
    if (i > -1) {
      usersClause = usersClause.substring(0, i);
    }
    if (usersClause.length() > 0) {
      usersClause = " and (" + usersClause + ")";
      return usersClause;
    }

    return "";
  }

}
