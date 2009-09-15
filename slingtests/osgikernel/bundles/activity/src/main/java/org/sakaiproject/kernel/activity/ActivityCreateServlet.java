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
package org.sakaiproject.kernel.activity;

import static org.sakaiproject.kernel.api.activity.ActivityConstants.ACTOR_PROPERTY;
import static org.sakaiproject.kernel.api.activity.ActivityConstants.REQUEST_PARAM_APPLICATION_ID;
import static org.sakaiproject.kernel.api.activity.ActivityConstants.REQUEST_PARAM_TEMPLATE_ID;
import static org.sakaiproject.kernel.api.activity.ActivityConstants.ACTIVITY_STORE_NAME;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.kernel.api.activity.ActivityConstants;
import org.sakaiproject.kernel.api.activity.ActivityUtils;
import org.sakaiproject.kernel.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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
 * 
 * @scr.component immediate="true" label="ActivityCreateServlet"
 *                description="Records the activity related to a particular node"
 * @scr.property name="service.description"
 *               value="Records the activity related to a particular node"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.selectors" value="activity"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/servlet/default"
 * @scr.reference name="EventAdmin" bind="bindEventAdmin" unbind="unbindEventAdmin"
 *                interface="org.osgi.service.event.EventAdmin"
 */
public class ActivityCreateServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = 1375206766455341437L;
  private static final Logger LOG = LoggerFactory.getLogger(ActivityCreateServlet.class);
  private EventAdmin eventAdmin;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("doPost(SlingHttpServletRequest " + request
          + ", SlingHttpServletResponse " + response + ")");
    }
    // Let's perform some validation on the request parameters. Do we have the minimum
    // required?
    RequestParameter applicationId = request.getRequestParameter(REQUEST_PARAM_APPLICATION_ID);
    if (applicationId == null || "".equals(applicationId.toString())) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The applicationId parameter must not be null");
      return;
    }
    RequestParameter templateId = request.getRequestParameter(REQUEST_PARAM_TEMPLATE_ID);
    if (templateId == null || "".equals(templateId.toString())) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The templateId parameter must not be null");
      return;
    }
    final String currentUser = request.getRemoteUser();
    if (currentUser == null || "".equals(currentUser)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "CurrentUser could not be determined, user must be identifiable");
      return;
    }

    // Create or verify that an ActivityStore exists for content node
    Node location = request.getResource().adaptTo(Node.class);
    Node activityStoreNode = null;
    Session session = null;
    String path = null;
    try {
      session = location.getSession();
      if (location.hasNode(ACTIVITY_STORE_NAME)) { // activityStore exists already
        activityStoreNode = location.getNode(ACTIVITY_STORE_NAME);
      } else { // need to create an activityStore
        path = location.getPath() + "/" + ACTIVITY_STORE_NAME;
        activityStoreNode = JcrUtils.deepGetOrCreateNode(session, path);
        activityStoreNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            ActivityConstants.ACTIVITY_STORE_RESOURCE_TYPE);
      }
      path = ActivityStoreServlet.getHashedPath(activityStoreNode.getPath(), UUID
          .randomUUID().toString());
      // for some odd reason I must manually create the Node before dispatching to
      // Sling...
      JcrUtils.deepGetOrCreateNode(session, path);
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
      throw new Error(e);
    }

    final String activityItemPath = path;
    final RequestPathInfo requestPathInfo = request.getRequestPathInfo();
    // Wrapper which needs to remove the .activity selector from RequestPathInfo to avoid
    // an infinite loop.
    final RequestPathInfo wrappedPathInfo = new RequestPathInfo() {
      @Override
      public String getSuffix() {
        return requestPathInfo.getSuffix();
      }

      @Override
      public String[] getSelectors() {
        // TODO Probably should just *remove* the ".activity" selector from array
        return new String[0];
      }

      @Override
      public String getSelectorString() {
        // TODO Probably should just *remove* the ".activity" selector from string
        return null;
      }

      @Override
      public String getResourcePath() {
        // LOG.debug("requestPathInfo.getResourcePath()=" + resourcePath);
        return activityItemPath;
      }

      @Override
      public String getExtension() {
        return requestPathInfo.getExtension();
      }
    };

    // Next insert the new RequestPathInfo into a wrapped Request
    SlingHttpServletRequest wrappedRequest = new SlingHttpServletRequestWrapper(request) {
      @Override
      public RequestPathInfo getRequestPathInfo() {
        return wrappedPathInfo;
      }
    };

    // let Sling do it's thing...
    Resource target = request.getResourceResolver().resolve(activityItemPath);
    LOG.debug("dispatch to {}  ", target);
    request.getRequestDispatcher(target).forward(wrappedRequest, response);

    // next add the current user to the actor property
    try {
      Node activity = (Node) session.getItem(activityItemPath);
      activity.setProperty(ACTOR_PROPERTY, currentUser);
      if (session.hasPendingChanges()) {
        session.save();
      }
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
      throw new Error(e);
    }
    // post the asynchronous OSGi event
    eventAdmin.postEvent(ActivityUtils.createEvent(activityItemPath));
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
