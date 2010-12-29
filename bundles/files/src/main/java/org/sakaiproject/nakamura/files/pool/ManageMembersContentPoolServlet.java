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

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_MANAGER;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_VIEWER;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

@ServiceDocumentation(name = "Manage Members Content Pool Servlet", description = "List and manage the managers and viewers for a file in the content pool.", bindings = { @ServiceBinding(type = BindingType.TYPE, bindings = { "sakai/pooled-content" }, selectors = {
    @ServiceSelector(name = "members", description = "Binds to the selector members."),
    @ServiceSelector(name = "detailed", description = "(optional) Provides more detailed profile information."),
    @ServiceSelector(name = "tidy", description = "(optional) Provideds 'tidy' (formatted) JSON output.") }) }, methods = {
    @ServiceMethod(name = "GET", description = "Retrieves a list of members.", response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully.  Output is in the JSON format."),
        @ServiceResponse(code = 500, description = "Any exceptions encountered during processing.") }),
    @ServiceMethod(name = "POST", description = "Manipulate the member list for a file.", parameters = {
        @ServiceParameter(name = ":manager", description = "Set the managers on the ACL of a file."),
        @ServiceParameter(name = ":viewer", description = "Set the viewers on the ACL of a file.") }, response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully."),
        @ServiceResponse(code = 401, description = "POST by anonymous user."),
        @ServiceResponse(code = 500, description = "Any exceptions encountered during processing.") }) })
@SlingServlet(methods = { "GET", "POST" }, resourceTypes = { "sakai/pooled-content" }, selectors = { "members" })
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Manages the Managers and Viewers for pooled content.") })
public class ManageMembersContentPoolServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = 3385014961034481906L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ManageMembersContentPoolServlet.class);


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
      Resource resource = request.getResource();
      javax.jcr.Session session = resource.getResourceResolver().adaptTo(
          javax.jcr.Session.class);
      
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Content node = resource.adaptTo(Content.class);

      Map<String, Object> properties = node.getProperties();
      String[] managers = StorageClientUtils.toStringArray(properties
          .get(POOLED_CONTENT_USER_MANAGER));
      String[] viewers = StorageClientUtils.toStringArray(properties
          .get(POOLED_CONTENT_USER_VIEWER));

      boolean detailed = false;
      boolean tidy = false;
      for (String selector : request.getRequestPathInfo().getSelectors()) {
        if ("detailed".equals(selector)) {
          detailed = true;
        } else if ("tidy".equals(selector)) {
          tidy = true;
        }
      }

      // Loop over the sets and output it.
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.setTidy(tidy);
      writer.object();
      writer.key("managers");
      writer.array();
      for (String manager : StorageClientUtils.nonNullStringArray(managers)) {
        writeProfileMap(session, userManager, writer, manager, detailed);
      }
      writer.endArray();
      writer.key("viewers");
      writer.array();
      for (String viewer : StorageClientUtils.nonNullStringArray(viewers)) {
        writeProfileMap(session, userManager, writer, viewer, detailed);
      }
      writer.endArray();
      writer.endObject();
    } catch (RepositoryException e) {
      response.sendError(SC_INTERNAL_SERVER_ERROR, "Could not send profile.");
      LOGGER.warn(e.getMessage());
    } catch (JSONException e) {
      response.sendError(SC_INTERNAL_SERVER_ERROR, "Failed to generate proper JSON.");
      LOGGER.error(e.getMessage(), e);
    }

  }

  private void writeProfileMap(javax.jcr.Session session, UserManager um,
      ExtendedJSONWriter writer, String user, boolean detailed)
      throws RepositoryException, JSONException {
    Authorizable au = um.getAuthorizable(user);
    if (au != null) {
      ValueMap profileMap = null;
      if (detailed) {
        profileMap = profileService.getProfileMap(au, session);
      } else {
        profileMap = profileService.getCompactProfileMap(au, session);
      }
      if (profileMap != null) {
        writer.valueMap(profileMap);
      }
    } else {
      writer.object();
      writer.key("userid");
      writer.value(user);
      writer.endObject();
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

    try {
      // Get the node.
      Resource resource = request.getResource();
      Session session = resource.adaptTo(Session.class);
      AccessControlManager accessControlManager = session.getAccessControlManager();
      
      Content node = resource.adaptTo(Content.class);
      ContentManager contentManager = resource.adaptTo(ContentManager.class);

      Map<String, Object> properties = node.getProperties();
      String[] managers = StorageClientUtils.toStringArray(properties
          .get(POOLED_CONTENT_USER_MANAGER));
      String[] viewers = StorageClientUtils.toStringArray(properties
          .get(POOLED_CONTENT_USER_VIEWER));

      
      Set<String> managerSet = null;
      if ( managers == null ) {
        managerSet = Sets.newHashSet();
      } else {
        managerSet = Sets.newHashSet(managers);
      }
      
      Set<String> viewersSet = null;
      if ( viewers == null ) {
        viewersSet = Sets.newHashSet();
      } else {
        viewersSet = Sets.newHashSet(viewers);
      }

      List<AclModification> aclModifications = Lists.newArrayList();

      for (String addManager : StorageClientUtils.nonNullStringArray(request.getParameterValues(":manager"))) {
        if (!managerSet.contains(addManager)) {
          managerSet.add(addManager);
          AclModification.addAcl(true, Permissions.CAN_MANAGE, addManager,
              aclModifications);
        }
      }
      
      for (String removeManager : StorageClientUtils.nonNullStringArray(request.getParameterValues(":manager@Delete"))) {
        if (managerSet.contains(removeManager)) {
          managerSet.remove(removeManager);
          AclModification.removeAcl(true, Permissions.CAN_MANAGE, removeManager,
              aclModifications);
        }
      }

      for (String addViewer : StorageClientUtils.nonNullStringArray(request.getParameterValues(":viewer"))) {
        if (!viewersSet.contains(addViewer)) {
          viewersSet.add(addViewer);
          AclModification.addAcl(true, Permissions.CAN_READ, addViewer, aclModifications);
        }
      }
      for (String removeViewer : StorageClientUtils.nonNullStringArray(request.getParameterValues(":viewer@Delete"))) {
        if (viewersSet.contains(removeViewer)) {
          viewersSet.remove(removeViewer);
          if (!managerSet.contains(removeViewer)) {
            AclModification.removeAcl(true, Permissions.CAN_READ, removeViewer,
                aclModifications);
          }
        }
      }

      // if there are viewers listed, then we need to remove anon and everyone read
      // grants, otherwise we need to remove the denys and add grants back in.
      if (viewersSet.size() > 0) {
        AclModification.removeAcl(true, Permissions.CAN_READ, User.ANON_USER,
            aclModifications);
        AclModification.removeAcl(true, Permissions.CAN_READ, Group.EVERYONE,
            aclModifications);
        AclModification.addAcl(false, Permissions.CAN_READ, User.ANON_USER,
            aclModifications);
        AclModification.addAcl(false, Permissions.CAN_READ, Group.EVERYONE,
            aclModifications);
      } else {
        AclModification.removeAcl(false, Permissions.CAN_READ, User.ANON_USER,
            aclModifications);
        AclModification.removeAcl(false, Permissions.CAN_READ, Group.EVERYONE,
            aclModifications);
        AclModification.addAcl(true, Permissions.CAN_READ, User.ANON_USER,
            aclModifications);
        AclModification.addAcl(true, Permissions.CAN_READ, Group.EVERYONE,
            aclModifications);
      }

      node.setProperty(POOLED_CONTENT_USER_VIEWER,
          StorageClientUtils.toStore(viewersSet.toArray(new String[viewersSet.size()])));
      node.setProperty(POOLED_CONTENT_USER_MANAGER,
          StorageClientUtils.toStore(managerSet.toArray(new String[managerSet.size()])));
      LOGGER.debug("Set Managers to {}",Arrays.toString(managerSet.toArray(new String[managerSet.size()])));
      LOGGER.debug("Set Viewsers to {}",Arrays.toString(viewersSet.toArray(new String[managerSet.size()])));
      LOGGER.debug("ACL Modifications {}",Arrays.toString(aclModifications.toArray(new AclModification[aclModifications.size()])));

      contentManager.update(node);
      accessControlManager.setAcl(Security.ZONE_CONTENT, node.getPath(),
          aclModifications.toArray(new AclModification[aclModifications.size()]));

      response.setStatus(SC_OK);
    } catch (AccessDeniedException e) {
      LOGGER.error("Could not set some permissions on [{}] Cause:{}",
          request.getPathInfo(), e.getMessage());
      LOGGER.info(e.getMessage(), e);
      response.sendError(SC_INTERNAL_SERVER_ERROR, "Could not set permissions.");
    } catch (StorageClientException e) {
      LOGGER.error("Could not set some permissions on [{}] Cause:{}",
          request.getPathInfo(), e.getMessage());
      LOGGER.debug("Cause: ", e);
      response.sendError(SC_INTERNAL_SERVER_ERROR, "Could not set permissions.");
    }
  }


}
