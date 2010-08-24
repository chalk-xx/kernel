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
package org.sakaiproject.nakamura.files.pool;

import static javax.jcr.security.Privilege.JCR_ALL;
import static javax.jcr.security.Privilege.JCR_READ;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.sling.jcr.base.util.AccessControlUtil.replaceAccessControlEntry;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_MANAGER;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_RT;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_VIEWER;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.servlet.ServletException;

@SlingServlet(methods = { "GET", "POST" }, resourceTypes = { "sakai/pooled-content" }, selectors = { "members" })
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Manages the Managers and Viewers for pooled content.") })
public class ManageMembersContentPoolServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = 3385014961034481906L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ManageMembersContentPoolServlet.class);

  @Reference
  protected transient SlingRepository slingRepository;

  @Reference
  protected transient ProfileService profileService;

  /**
   * Retrieves the list of members.
   *
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      // Get hold of the actual file.
      Node node = request.getResource().adaptTo(Node.class);
      Session session = node.getSession();

      // Get hold of the members node that is under the file.
      // This node contains a list of managers and viewers.
      Map<String, Boolean> users = getMembers(node);

      UserManager um = AccessControlUtil.getUserManager(session);

      // Loop over the sets and output it.
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.object();
      writer.key("managers");
      writer.array();
      for (Entry<String, Boolean> entry : users.entrySet()) {
        if (entry.getValue()) {
          writeProfileMap(session, um, writer, entry);
        }
      }
      writer.endArray();
      writer.key("viewers");
      writer.array();
      for (Entry<String, Boolean> entry : users.entrySet()) {
        if (!entry.getValue()) {
          writeProfileMap(session, um, writer, entry);
        }
      }
      writer.endArray();
      writer.endObject();
    } catch (RepositoryException e) {
      response.sendError(SC_INTERNAL_SERVER_ERROR, "Could not lookup ACL list.");
    } catch (JSONException e) {
      response.sendError(SC_INTERNAL_SERVER_ERROR, "Failed to generate proper JSON.");
    }

  }

  private void writeProfileMap(Session session, UserManager um,
      ExtendedJSONWriter writer, Entry<String, Boolean> entry)
      throws RepositoryException, JSONException {
    Authorizable au = um.getAuthorizable(entry.getKey());
    if (au != null) {
      ValueMap profileMap = profileService.getCompactProfileMap(au, session);
      writer.valueMap(profileMap);
    } else {
      writer.key(entry.getKey());
      writer.value(null);
    }
  }

  /**
   * Manipulate the member list for this file.
   *
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // Anonymous users cannot do anything.
    // This is just a safety check really, they SHOULD NOT even be able to get to this
    // point.
    if (UserConstants.ANON_USERID.equals(request.getRemoteUser())) {
      response.sendError(SC_UNAUTHORIZED, "Anonymous users cannot manipulate content.");
      return;
    }

    Session adminSession = null;
    try {
      // Get the node.
      Node node = request.getResource().adaptTo(Node.class);
      Session session = node.getSession();

      // The privileges for the managers and viewers.
      // Viewers only get READ, managers get ALL
      String[] managerPrivs = new String[] { JCR_ALL };
      String[] viewerPrivs = new String[] { JCR_READ };

      // We need an admin session because we might only have READ access on this node.
      // Yes, that is sufficient to share a file with somebody else.
      // We also re-fetch the node because we need to make some changes to the underlying
      // structure.
      // Only the admin has WRITE on that structure.
      adminSession = slingRepository.loginAdministrative(null);
      node = adminSession.getNode(node.getPath());
      UserManager um = AccessControlUtil.getUserManager(adminSession);

      // Get all the managers for this file.
      Set<Authorizable> managers = getManagers(node, um);

      // If you have READ access than you can give other people read as well.
      manipulateACL(request, adminSession, node, viewerPrivs, um, ":viewer",
          POOLED_CONTENT_USER_VIEWER, managers);

      // Only managers can make other people managers, so we need to do some checks.
      AccessControlManager acm = AccessControlUtil.getAccessControlManager(session);
      Privilege allPriv = acm.privilegeFromName(JCR_ALL);
      if (acm.hasPrivileges(node.getPath(), new Privilege[] { allPriv })) {
        manipulateACL(request, adminSession, node, managerPrivs, um, ":manager",
            POOLED_CONTENT_USER_MANAGER, managers);
      }

      // Persist any changes.
      if (adminSession.hasPendingChanges()) {
        adminSession.save();
      }
      response.setStatus(SC_OK);
    } catch (RepositoryException e) {
      LOGGER
          .error("Could not set some permissions on '" + request.getPathInfo() + "'", e);
      response.sendError(SC_INTERNAL_SERVER_ERROR, "Could not set permissions.");
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    }
  }

  /**
   * Looks at the values of some request parameters (specified by the key) and sets some
   * ACLs.
   *
   * @param request
   *          The request that contains the request parameters.
   * @param session
   *          A session that can change permissions on the specified path.
   * @param path
   *          The path for which the permissions should be changed.
   * @param privilege
   *          Which privileges should be granted (and removed for those specied in the
   *          "key@Delete" request parameter.
   * @param um
   *          A UserManager to retrieve the correct authorizables.
   * @param key
   *          The key that should be used to look for the request parameters. A key of
   *          'manager' will result in 2 parameters to be looked up.
   *          <ul>
   *          <li>manager : A multi-valued request parameter that contains the IDs of the
   *          principals that should be granted the specified privileges</li>
   *          <li>manager@Delete : A multi-valued request parameter that contains the IDs
   *          of the principals whose privileges should be revoked.</li>
   *          </ul>
   * @param property
   * @param managers
   *          A set of Authorizables who represent the managers for this node. They will
   *          be given READ access on the node.
   * @throws RepositoryException
   */
  protected void manipulateACL(SlingHttpServletRequest request, Session session,
      Node fileNode, String[] privilege, UserManager um, String key, String property,
      Set<Authorizable> managers) throws RepositoryException {

    // Get all the IDs of the authorizables that should be added and removed from the
    // request.
    String[] toAdd = request.getParameterValues(key);
    Set<Authorizable> toAddSet = new HashSet<Authorizable>();
    String[] toDelete = request.getParameterValues(key + "@Delete");
    Set<Authorizable> toDeleteSet = new HashSet<Authorizable>();
    String path = fileNode.getPath();

    // Resolve the IDs to authorizables.
    resolveNames(um, toAdd, toAddSet);
    resolveNames(um, toDelete, toDeleteSet);
    Principal everyone = new Principal() {
      public String getName() {
        return "everyone";
      }
    };

    // Give the privileges to the set that should be added.
    for (Authorizable au : toAddSet) {
      // Give the user his privilege on the actual file.
      replaceAccessControlEntry(session, path, au.getPrincipal(), privilege, null, null,
          null);

      // We store the user in JCR as well, because we need to be able to search for the
      // files associated with a user/group.
      String newPath = path + PersonalUtils.getUserHashedPath(au);
      Node node = JcrUtils.deepGetOrCreateNode(session, newPath);
      node.setProperty(SLING_RESOURCE_TYPE_PROPERTY, POOLED_CONTENT_USER_RT);
      node.setProperty(property, au.getID());

      // Nobody can read this node except managers.
      // We do this because only the managers can know who has access to this file.
      replaceAccessControlEntry(session, newPath, everyone, null,
          new String[] { JCR_READ }, null, null);
      for (Authorizable m : managers) {
        replaceAccessControlEntry(session, newPath, m.getPrincipal(),
            new String[] { JCR_ALL }, null, null, null);

      }
    }

    // Remove the privileges for the people that should be
    // TODO Maybe remove the entire entry instead of just denying the privilege?
    for (Authorizable a : toDeleteSet) {
      // Deny the privilege for this user/group
      replaceAccessControlEntry(session, path, a.getPrincipal(), null, privilege, null,
          null);

      // Remove the property in JCR as well.
      String newPath = path + PersonalUtils.getUserHashedPath(a);
      if (session.itemExists(newPath)) {
        Node node = JcrUtils.deepGetOrCreateNode(session, newPath);
        // Removing the property.
        node.setProperty(property, (Value) null);
      }
    }
  }

  /**
   * Resolves each string in the array of names and adds them to the set of authorizables.
   * Authorizables that cannot be found, will not be added to the set.
   *
   * @param um
   *          A UserManager that can be used to find authorizables.
   * @param names
   *          An array of strings that contain the names.
   * @param authorizables
   *          A Set of Authorizables where each principal can be added to.
   * @throws RepositoryException
   */
  protected void resolveNames(UserManager um, String[] names,
      Set<Authorizable> authorizables) throws RepositoryException {
    if (names != null) {
      for (String principalName : names) {
        Authorizable au = um.getAuthorizable(principalName);
        if (au != null) {
          authorizables.add(au);
        }
      }
    }
  }

  /**
   * Gets all the "members" for a file.
   *
   * @param node
   *          The node that represents the file.
   * @return A map where each key is a userid, the value is a boolean that states if it is
   *         a manager or not.
   * @throws RepositoryException
   */
  protected Map<String, Boolean> getMembers(Node node) throws RepositoryException {
    Session session = node.getSession();
    Map<String, Boolean> users = new HashMap<String, Boolean>();

    // Perform a query that gets all the "member" nodes.
    String path = ISO9075.encodePath(node.getPath());
    StringBuilder sb = new StringBuilder("/jcr:root/");
    sb.append(path).append("//*[@").append(SLING_RESOURCE_TYPE_PROPERTY);
    sb.append("='").append(POOLED_CONTENT_USER_RT).append("']");
    QueryManager qm = session.getWorkspace().getQueryManager();
    Query q = qm.createQuery(sb.toString(), "xpath");
    QueryResult qr = q.execute();
    NodeIterator iterator = qr.getNodes();

    // Loop over the "member" nodes.
    while (iterator.hasNext()) {
      Node memberNode = iterator.nextNode();
      if (memberNode.hasProperty(POOLED_CONTENT_USER_MANAGER)) {
        users.put(memberNode.getName(), true);
      } else if (memberNode.hasProperty(POOLED_CONTENT_USER_VIEWER)) {
        users.put(memberNode.getName(), false);
      }
    }

    return users;
  }

  /**
   * Get all the manager authorizables for a pooled content node.
   *
   * @param node
   *          The pooled content node
   * @param um
   *          A UserManager that can be used to resolve IDs to authorizables.
   * @return A Set of Authorizables that represent the managers for this node.
   * @throws RepositoryException
   */
  protected Set<Authorizable> getManagers(Node node, UserManager um)
      throws RepositoryException {
    Map<String, Boolean> users = getMembers(node);
    Set<Authorizable> authorizables = new HashSet<Authorizable>();

    for (Entry<String, Boolean> entry : users.entrySet()) {
      if (entry.getValue()) {
        authorizables.add(um.getAuthorizable(entry.getKey()));
      }
    }

    return authorizables;
  }
}
