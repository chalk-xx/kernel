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

import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.util.LitePersonalUtils;

import java.io.IOException;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@Component(immediate = true, label = "%group.joinServlet.label", description = "%group.joinServlet.desc")
@SlingServlet(resourceTypes = "sparse/joinrequests", methods = "POST", selectors = "create", generateComponent = false)
public class LiteGroupJoinRequestServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = -6508149691456203381L;

  private static final String PARAM_USERID = "userid";

  @Reference
  @SuppressWarnings(value = "NP_UNWRITTEN_FIELD", justification = "Injected by OSGi")
  protected transient Repository repository;

  /**
   * The OSGi Event Admin Service.
   */
  @Reference
  @SuppressWarnings(value = "NP_UNWRITTEN_FIELD, UWF_UNWRITTEN_FIELD", justification = "Injected by OSGi")
  private transient EventAdmin eventAdmin;


  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Content joinrequests = request.getResource().adaptTo(Content.class);
    if (joinrequests == null) {
      response.sendError(HttpServletResponse.SC_NO_CONTENT,
          "Couldn't find joinrequests node");
      return;
    }
    try {
      Session session = StorageClientUtils.adaptToSession(request.getResource().getResourceResolver().adaptTo(javax.jcr.Session.class));
      ContentManager contentManager = session.getContentManager();
      String requestedBy = session.getUserId();
      if (ANON_USERID.equals(requestedBy)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "anonymous user may not request to join a group");
        return;
      }
      Content group = contentManager.get(StorageClientUtils.getParentObjectPath(joinrequests.getPath()));
      RequestParameter userToJoin = request.getRequestParameter(PARAM_USERID);
      if (userToJoin == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "requesting user must be specified in the request parameter " + PARAM_USERID);
        return;
      }
      if (!userToJoin.getString().equalsIgnoreCase(requestedBy)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "You may not request someone other than yourself join a group");
        return;
      }
      joinGroup(group, userToJoin.getString());
    } catch (Exception e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
    }
  }

  private void joinGroup(Content group, String userId) throws StorageClientException, AccessDeniedException  {
    Session session = null;
    try {
      
    String groupId = StorageClientUtils.getObjectName(group.getPath());
    session = repository.loginAdministrative();
    ContentManager contentManager = session.getContentManager();
    Content profileContent = contentManager.get(group.getPath()+"/"+LitePersonalUtils.PATH_PUBLIC+"/"+LitePersonalUtils.PATH_AUTH_PROFILE);
    AuthorizableManager authorizableManager = session.getAuthorizableManager();
    Group targetGroup = (Group) authorizableManager.findAuthorizable(groupId);
    Joinable joinable = Joinable.no;
    String joinability = (String) profileContent.getProperty("sakai:group-joinable");
    if (joinability != null) {
      joinable = Joinable.valueOf(joinability);
    }

    switch (joinable) {
      case no:
        break;
      case yes:
        // membership is automatically granted
        targetGroup.addMember(userId);
        Dictionary<String, Object> eventProps = new Hashtable<String, Object>();
        eventAdmin.postEvent(new Event(GroupEvent.joinedSite.getTopic(), eventProps));
        break;
      case withauth:
        // check to see if this user is already there
        if (contentManager.exists(group.getPath() + "/joinrequests/"+userId)) {
          // just update the date
          Content joinRequestUpdate = contentManager.get(group.getPath() + "/joinrequests/"+userId);
          joinRequestUpdate.setProperty("requested", Calendar.getInstance());
          contentManager.update(joinRequestUpdate);
        } else {
          contentManager.update(new Content(group.getPath() + "/joinrequests/"+userId, ImmutableMap.of(
              "requested", (Object)Calendar.getInstance(),
              "profile", LitePersonalUtils.getProfilePath(userId),
              "sling:resourceType", "sakai/joinrequest"
          )));
        }
        break;
      default:
        break;
    }
    } finally {
      if ( session != null ) {
        session.logout();
      }
    }
  }

  /**
   * The joinable property
   */
  public enum Joinable {
    /**
     * The site is joinable.
     */
    yes(),
    /**
     * The site is not joinable.
     */
    no(),
    /**
     * The site is joinable with approval.
     */
    withauth();
  }

  /**
   * An Event Enumeration for all the events that the Site Service might emit.
   */
  public enum GroupEvent {
    /**
     * Event posted to indicate a site has been created
     */
    created(),
    /**
     * This event is posted to indicate the at join workflow should be started for the
     * user.
     */
    startJoinWorkflow(),
    /**
     * Indicates that a user just joined the site.
     */
    joinedSite(),
    /**
     * Indicates a user just left the site.
     */
    unjoinedSite();
    /**
     * The topic that the event is sent as.
     */
    public static final String TOPIC = "org/sakaiproject/nakamura/api/group/event/";
    /**
     * The user that is the subject of the event.
     */
    public static final String USER = "user";
    /**
     * The target group of the request.
     */
    public static final String GROUP = "group";

    /**
     * @return a topic ID for sites, bound to the operation being performed.
     */
    public String getTopic() {
      return TOPIC + toString();
    }

  }

}
