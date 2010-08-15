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
package org.sakaiproject.nakamura.user.servlet;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Also, when KERN-949 is fixed, we should change the getManagers() method.
 *
 *
 * Provides a listing for the members and managers of this group.
 */
@SlingServlet(resourceTypes = { "sling/group" }, methods = { "GET" }, selectors = {
    "members", "managers" }, extensions = { "json" })
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Renders the members or managers for a group") })
public class GroupMemberServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 7976930178619974246L;

  @Reference
  protected transient ProfileService profileService;

  static final String ITEMS = "items";
  static final String PAGE = "page";

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Authorizable authorizable = null;
    Resource resource = request.getResource();
    if (resource != null) {
      authorizable = resource.adaptTo(Authorizable.class);
    }

    if (authorizable == null || !authorizable.isGroup()) {
      response.sendError(HttpServletResponse.SC_NO_CONTENT, "Couldn't find group");
      return;
    }

    Group group = (Group) authorizable;

    List<String> selectors = Arrays.asList(request.getRequestPathInfo().getSelectors());
    ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
    writer.setTidy(selectors.contains("tidy"));

    // Get the sorting order, default is ascending or the natural sorting order (which is
    // null for a TreeMap.)
    Comparator<String> comparator = null;
    String order = "ascending";
    if (request.getRequestParameter("sortOrder") != null) {
      order = request.getRequestParameter("sortOrder").getString();
      if (order.equals("descending")) {
        comparator = Collections.reverseOrder();
      }
    }

    try {
      TreeMap<String, Authorizable> map = null;
      if (selectors.contains("managers")) {
        map = getManagers(request, group, comparator);
      } else {
        // Members is the default.
        map = getMembers(request, group, comparator);
      }

      // Do some paging.
      long items = (request.getParameter(ITEMS) != null) ? Long.parseLong(request
          .getParameter(ITEMS)) : 25;
      long page = (request.getParameter(PAGE) != null) ? Long.parseLong(request
          .getParameter(PAGE)) : 0;
      if (page < 0) {
        page = 0;
      }
      if (items < 0) {
        items = 25;
      }
      Iterator<Entry<String, Authorizable>> iterator = getInPlaceIterator(request, map,
          items, page);

      // Write the whole lot out.
      Session session = request.getResourceResolver().adaptTo(Session.class);
      writer.array();
      int i = 0;
      while (iterator.hasNext() && i < items) {
        Entry<String, Authorizable> entry = iterator.next();
        Authorizable au = entry.getValue();
        ValueMap profile = profileService.getCompactProfileMap(au, session);
        writer.valueMap(profile);
        i++;
      }
      writer.endArray();

    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to retrieve members/managers.");
      return;
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to build a proper JSON output.");
      return;
    }

  }

  /**
   * @param request
   * @param map
   * @return
   */
  private Iterator<Entry<String, Authorizable>> getInPlaceIterator(
      SlingHttpServletRequest request, TreeMap<String, Authorizable> map,
      long items, long page) {
    Iterator<Entry<String, Authorizable>> iterator = map.entrySet().iterator();
    long skipNum = items * page;

    while (skipNum > 0) {
      iterator.next();
      skipNum--;
    }

    return iterator;
  }

  /**
   * @param request
   * @param group
   * @param writer
   * @throws RepositoryException
   * @throws JSONException
   */
  protected TreeMap<String, Authorizable> getMembers(SlingHttpServletRequest request,
      Group group, Comparator<String> comparator) throws RepositoryException,
      JSONException {
    TreeMap<String, Authorizable> map = new TreeMap<String, Authorizable>(comparator);

    // Only the direct members are required.
    // If we would do group.getMembers() that would also retrieve all the indirect ones.
    Iterator<Authorizable> members = group.getDeclaredMembers();
    while (members.hasNext()) {
      Authorizable member = members.next();
      String name = getName(member);
      map.put(name, member);
    }
    return map;
  }

  /**
   * Get the managers for a group. These should be stored in the
   * {@link UserConstants#PROP_GROUP_MANAGERS}.
   *
   * @param request
   * @param group
   * @param writer
   * @throws RepositoryException
   * @throws JSONException
   */
  protected TreeMap<String, Authorizable> getManagers(SlingHttpServletRequest request,
      Group group, Comparator<String> comparator) throws RepositoryException,
      JSONException {
    TreeMap<String, Authorizable> map = new TreeMap<String, Authorizable>(comparator);

    // KERN-949 will probably change this.
    Session session = request.getResourceResolver().adaptTo(Session.class);
    UserManager um = AccessControlUtil.getUserManager(session);
    Value[] managerValues = group.getProperty(UserConstants.PROP_GROUP_MANAGERS);
    if (managerValues != null) {
      for (Value manager : managerValues) {
        Authorizable au = um.getAuthorizable(manager.getString());
        String name = getName(au);
        map.put(name, au);
      }
    }
    return map;

  }

  /**
   * Get's the name for an authorizable on what the list should be sorted.
   * sakai:group-title for Groups, lastName for Users.
   *
   * @param member
   *          The authorizable to get a name for.
   * @return The name.
   * @throws RepositoryException
   */
  private String getName(Authorizable member) throws RepositoryException {
    String name = member.getID();
    if (member.isGroup()) {
      Value[] values = member.getProperty("sakai:group-title");
      if (values != null && values.length != 0) {
        name = values[0].getString();
      }
    } else {
      Value[] values = member.getProperty("lastName");
      if (values != null && values.length != 0) {
        name = values[0].getString();
      }
    }
    // We need to add the ID to keep the keys unique.
    return name + member.getID();
  }

}
