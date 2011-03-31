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
package org.sakaiproject.nakamura.activity;

import static org.sakaiproject.nakamura.api.activity.ActivityConstants.ACTIVITY_FEED_RESOURCE_TYPE;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.ACTIVITY_ITEM_RESOURCE_TYPE;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.ACTIVITY_STORE_NAME;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.ACTIVITY_STORE_RESOURCE_TYPE;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.LITE_EVENT_TOPIC;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.PARAM_ACTOR_ID;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.PARAM_APPLICATION_ID;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.PARAM_TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.activity.ActivityUtils;
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
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.StringUtils;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * In addition to the required parameters, properties should be included that will be used
 * to fill the bundle (i.e. macro expansions).
 * <p>
 * Required parameters:<br/>
 * applicationId: (i.e. used to locate the bundles)<br/>
 * templateId: (i.e. you know - the templateId. Locale will be appended to the templateId
 * for resolution.)
 */
@SlingServlet(selectors = { "activity" }, methods = { "POST" }, resourceTypes = { "sparse/Content" }, generateService = true, generateComponent = true)
@Properties(value = {
    @Property(name = "service.description", value = "Records the activity related to a particular content"),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
@ServiceDocumentation(name = "ActivityCreateServlet", shortDescription = "Record activity related to a specific node.", description = "Record activity related to a specific node.", bindings = @ServiceBinding(type = BindingType.PATH, bindings = "*", selectors = @ServiceSelector(name = "activity")), methods = { @ServiceMethod(name = "POST", description = "Perform a post to a particular resource to record activity related to it.", parameters = {
    @ServiceParameter(name = "sakai:activity-appid", description = "i.e. used to locate the bundles"),
    @ServiceParameter(name = "sakai:activity-templateid", description = "The id of the template that will be used for text and macro expansion."
        + "Locale will be appended to the templateId for resolution"),
    @ServiceParameter(name = "*", description = "You should also include any parameters necessary to fill the template specified in sakai:activity-templateid.") }, response = {
    @ServiceResponse(code = 400, description = "if(applicationId == null || templateId == null || request.getRemoteUser() == null)"),
    @ServiceResponse(code = HttpServletResponse.SC_PRECONDITION_FAILED, description = "Cannot record activities on activity content!"),
    @ServiceResponse(code = 404, description = "The node was not found.") }) })
public class LiteActivityCreateServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = -5367330873214708635L;
  private static final Logger LOG = LoggerFactory
      .getLogger(LiteActivityCreateServlet.class);

  @Reference
  protected transient EventAdmin eventAdmin;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // Let's perform some validation on the request parameters.
    // Do we have the minimum required?
    RequestParameter applicationId = request.getRequestParameter(PARAM_APPLICATION_ID);
    if (applicationId == null || "".equals(applicationId.toString())) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The applicationId parameter must not be null");
      return;
    }
    RequestParameter templateId = request.getRequestParameter(PARAM_TEMPLATE_ID);
    if (templateId == null || "".equals(templateId.toString())) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The templateId parameter must not be null");
      return;
    }
    final String currentUser = request.getRemoteUser();
    if (currentUser == null || "".equals(currentUser)) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "CurrentUser could not be determined, user must be identifiable");
      return;
    }

    // Create or verify that an ActivityStore exists for content node
    // An activity store will be created for each node where a .activity gets executed.
    Content location = request.getResource().adaptTo(Content.class);
    if (location == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    final String resourceType = (String) location.getProperty("sling:resourceType");
    // Do not allow for reserved activity resource types
    if (ACTIVITY_STORE_RESOURCE_TYPE.equals(resourceType)
        || ACTIVITY_FEED_RESOURCE_TYPE.equals(resourceType)
        || ACTIVITY_ITEM_RESOURCE_TYPE.equals(resourceType)) {
      LOG.info(
          "Denied attempt to record an activity against a reserved resourceType: {}",
          resourceType);
      response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
      return;
    }
    Session session = null;
    try {
      session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(
          javax.jcr.Session.class));
      ContentManager contentManager = session.getContentManager();
      // create activityStore if it does not exist
      String path = StorageClientUtils.newPath(location.getPath(), ACTIVITY_STORE_NAME);
      if (!contentManager.exists(path)) {
        contentManager.update(new Content(path, ImmutableMap.of(
            JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            (Object) ActivityConstants.ACTIVITY_STORE_RESOURCE_TYPE)));
        // set ACLs so that everyone can add activities; anonymous = none.
        session.getAccessControlManager().setAcl(
            Security.ZONE_CONTENT,
            path,
            new AclModification[] {
                new AclModification(AclModification.denyKey(User.ANON_USER),
                    Permissions.ALL.getPermission(), Operation.OP_REPLACE),
                new AclModification(AclModification.grantKey(Group.EVERYONE),
                    Permissions.CAN_READ.getPermission(), Operation.OP_REPLACE),
                new AclModification(AclModification.grantKey(Group.EVERYONE),
                    Permissions.CAN_WRITE.getPermission(), Operation.OP_REPLACE),
                new AclModification(AclModification.grantKey(session.getUserId()),
                    Permissions.ALL.getPermission(), Operation.OP_REPLACE) });
      }
      // create activity within activityStore
      String activtyPath = StorageClientUtils.newPath(path, ActivityUtils.createId());
      if (!contentManager.exists(activtyPath)) {
        contentManager.update(new Content(activtyPath, ImmutableMap.of(
            JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            (Object) ActivityConstants.ACTIVITY_ITEM_RESOURCE_TYPE)));
      }

      RequestPathInfo requestPathInfo = request.getRequestPathInfo();
      // Wrapper which needs to remove the .activity selector from RequestPathInfo to
      // avoid
      // an infinite loop.
      final RequestPathInfo wrappedPathInfo = createRequestPathInfo(requestPathInfo,
          activtyPath);

      // Next insert the new RequestPathInfo into a wrapped Request
      SlingHttpServletRequest wrappedRequest = new SlingHttpServletRequestWrapper(request) {
        @Override
        public RequestPathInfo getRequestPathInfo() {
          return wrappedPathInfo;
        }
      };

      // We redispatch the request to the created sakai/activity.
      // This allows for custom properties to be set by the UI.
      Content activtyNode = contentManager.get(activtyPath);
      SparseContentResource target = new SparseContentResource(activtyNode, session, request.getResourceResolver());
      request.getRequestDispatcher(target).forward(wrappedRequest, response);

      activtyNode = contentManager.get(activtyPath);
      activtyNode.setProperty(PARAM_ACTOR_ID, currentUser);
      contentManager.update(activtyNode);
      // post the asynchronous OSGi event
      final Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(UserConstants.EVENT_PROP_USERID, request.getRemoteUser());
      properties.put(ActivityConstants.EVENT_PROP_PATH, activtyPath);
      EventUtils.sendOsgiEvent(target, properties, LITE_EVENT_TOPIC, eventAdmin);
    } catch (StorageClientException e) {
      throw new ServletException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new ServletException(e.getMessage(), e);
    }
  }

  /**
   * @param requestPathInfo
   * @param activityItemPath
   * @return
   */
  protected RequestPathInfo createRequestPathInfo(final RequestPathInfo requestPathInfo,
      final String activityItemPath) {
    return new RequestPathInfo() {
      public String getSuffix() {
        return requestPathInfo.getSuffix();
      }

      public String[] getSelectors() {
        return StringUtils.removeString(requestPathInfo.getSelectors(), "activity");
      }

      public String getSelectorString() {
        return requestPathInfo.getSelectorString().replaceAll("\\.activity", "");
      }

      public String getResourcePath() {
        return activityItemPath;
      }

      public String getExtension() {
        return requestPathInfo.getExtension();
      }
    };
  }

  /**
   * @param eventAdmin
   *          the new EventAdmin service to bind to this service.
   */
  protected void bindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  /**
   * @param eventAdmin
   *          the EventAdminService to be unbound from this service.
   */
  protected void unbindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = null;
  }
}
