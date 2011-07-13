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
package org.sakaiproject.nakamura.user.lite.servlet;

import static org.sakaiproject.nakamura.api.user.UserConstants.ANON_USERID;

import com.google.common.collect.Maps;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that handles users leaving groups. This was created because a non-manager user
 * can not edit the group and therefore can't remove themself. This servlet acts on behalf
 * of the user to leave a group and does not allow removal of any other user.
 */
@ServiceDocumentation(name = "LiteGroupLeaveServlet documentation", okForVersion = "0.11",
  shortDescription = "Servlet to allow a user to leave a group",
  description = "Servlet to allow a user to leave a group, responding to groups using the 'leave' selector. Only works for removing the logged in user.",
  bindings = @ServiceBinding(type = BindingType.TYPE, bindings = "sparse/joinrequests",
    selectors = @ServiceSelector(name = "leave", description = ""),
    extensions = { @ServiceExtension(name = "json") }),
  methods = {
    @ServiceMethod(name = "POST", description = {
        "Create a new join request.",
        "curl --referer http://localhost:8080 -u user:pass -F go=1 http://localhost:8080/system/userManager/group/testGroup.leave.html"
      },
      response = {
        @ServiceResponse(code = HttpServletResponse.SC_OK, description = "Request has been processed successfully."),
        @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Resource could not be found."),
        @ServiceResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to process request due to a runtime error.")
      }
    )
  }
)
@Component(immediate = true)
@SlingServlet(resourceTypes = "sparse/group", methods = "POST", selectors = "leave", generateComponent = false)
public class LiteGroupLeaveServlet extends LiteAbstractSakaiGroupPostServlet {

  /**
   *
   */
  private static final long serialVersionUID = -6508149691456203381L;

  /**
   * The OSGi Event Admin Service.
   */
  @Reference
  @SuppressWarnings(value = "NP_UNWRITTEN_FIELD, UWF_UNWRITTEN_FIELD", justification = "Injected by OSGi")
  protected transient EventAdmin eventAdmin;

  @Override
  protected void handleOperation(SlingHttpServletRequest request,
      HtmlResponse htmlResponse, List<Modification> changes)
      throws StorageClientException, AccessDeniedException {
    Group groupToLeave = null;
    Resource resource = request.getResource();

    if (resource != null) {
      groupToLeave = (Group) resource.adaptTo(Group.class);
    }

    // check that the group was located.
    if (groupToLeave == null) {
      throw new ResourceNotFoundException("Group to leave could not be determined");
    }

    String requestedBy = request.getRemoteUser();
    if (ANON_USERID.equals(requestedBy)) {
      throw new AccessDeniedException(Security.ZONE_AUTHORIZABLES, groupToLeave.getId(), "", requestedBy);
    }

    // if no userids submitted, try to remove current user
    Map<String, Object> toSave = Maps.newHashMap();
    removeAuth(groupToLeave, requestedBy, toSave);

    if (groupToLeave.isModified()) {
      // remove current user using an admin session since non-managers can't edit the group
      Session adminSession = null;
      try {
        adminSession = repository.loginAdministrative();

        adminSession.getAuthorizableManager().updateAuthorizable(groupToLeave);
        changes.add(Modification.onDeleted(requestedBy));
      } finally {
        if (adminSession != null) {
          adminSession.logout();
        }
      }
    }
  }

  private void removeAuth(Group groupToLeave, String authId, Map<String, Object> toSave) {
    // remove member permissions
    int count = groupToLeave.getMembersRemoved().length;
    groupToLeave.removeMember(authId);
    if (count < groupToLeave.getMembersRemoved().length) {
      toSave.put(groupToLeave.getId(), groupToLeave);
    }

    // remove viewer permissions
    handleAuthorizablesOnProperty(groupToLeave, UserConstants.PROP_GROUP_VIEWERS,
        new String[] { authId }, null, toSave);

    // remove manager permissions
    handleAuthorizablesOnProperty(groupToLeave, UserConstants.PROP_GROUP_MANAGERS,
        new String[] { authId }, null, toSave);
  }
}
