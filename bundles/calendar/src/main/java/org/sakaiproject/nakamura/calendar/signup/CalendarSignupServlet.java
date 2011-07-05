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
package org.sakaiproject.nakamura.calendar.signup;

import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.PARTICIPANTS_NODE_NAME;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_CALENDAR_NODENAME;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_CALENDAR_PROFILE_LINK;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_CALENDAR_RT;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_EVENT_SIGNUP_PARTICIPANT_RT;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_SIGNEDUP_DATE;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_SIGNEDUP_ORIGINAL_EVENT;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_USER;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.TOPIC_CALENDAR_SIGNUP;

import com.google.common.collect.ImmutableMap;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.calendar.CalendarException;
import org.sakaiproject.nakamura.api.calendar.signup.SignupPreProcessor;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(
  name = "Calendar Signup Servlet",
  okForVersion = "0.11",
  shortDescription = "Signs up the current user to an event and copies that event to the user's calendar",
  description = "This servlet handles signing up the current user to an event then copying that event to the user's calendar.",
  bindings = {
    @ServiceBinding(type = BindingType.TYPE,
      bindings = {
        "sakai/calendar-signup"
      },
      selectors = {
        @ServiceSelector(name = "signup", description = "Binds to the signup selector.")
      }
    )
  },
  methods = {
    @ServiceMethod(name = "POST", description = "Signup for a calendar event.",
      response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully."),
        @ServiceResponse(code = 400, description = "User is already signed up for the event."),
        @ServiceResponse(code = 401, description = "POST by anonymous user."),
        @ServiceResponse(code = 500, description = "Failed to copy event to user's calendar or any exceptions encountered during processing.")
      }
    )
  }
)
@SlingServlet(resourceTypes = { "sakai/calendar-signup" }, selectors = { "signup" }, methods = { "POST" }, generateComponent = true, generateService = true)
@Reference(referenceInterface = SignupPreProcessor.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "bindPreProcessor", unbind = "unbindPreProcessor")
public class CalendarSignupServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 2770138417371548411L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(CalendarSignupServlet.class);

  protected List<SignupPreProcessor> signupPreProcessors = new ArrayList<SignupPreProcessor>();


  @Reference
  protected transient EventAdmin eventAdmin;
  @Reference
  protected Repository repository;


  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    // Keep out anon users.
    if (UserConstants.ANON_USERID.equals(request.getRemoteUser())) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "Anonymous users can't signup for events.");
      return;
    }

    // Grab the signup node.
    Resource signupResource = request.getResource();
    Content signupNode = signupResource.adaptTo(Content.class);
    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
    try {
      // Check if this user is already signed up for this event.
      checkAlreadySignedup(signupNode, session);

      // Loop over all the pre processors and make sure this request is correct.
      for (SignupPreProcessor processor : signupPreProcessors) {
        processor.checkSignup(request, signupNode, session);
      }
    } catch (CalendarException e) {
      LOGGER.error("Invalid calendar signup request.", e);
      response.sendError(e.getCode(), e.getMessage());
      return;
    }

    try {
      // Handle the signup and add the participant.
      handleSignup(signupNode, session);

      // Copy the event to the user his calendar.
      copyEventNode(signupNode, session);

      // Send an OSGi event for this sign up.
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(UserConstants.EVENT_PROP_USERID, request.getRemoteUser());
      EventUtils.sendOsgiEvent(signupResource, properties, TOPIC_CALENDAR_SIGNUP, eventAdmin);
    } catch (CalendarException e) {
      LOGGER.error("Invalid calendar signup request.", e);
      response.sendError(e.getCode(), e.getMessage());
      return;
    }

  }

  /**
   * @param signupNode
   * @throws CalendarException
   */
  protected void checkAlreadySignedup(Content signupNode, Session session) throws CalendarException {
    try {
      String user = session.getUserId();
      ContentManager contentManager = session.getContentManager();
      String path = StorageClientUtils.newPath(signupNode.getPath() ,PARTICIPANTS_NODE_NAME+user);
      if (contentManager.exists(path)) {
        throw new CalendarException(HttpServletResponse.SC_BAD_REQUEST,
            "You are already signed up for this event.");
      }

    } catch (StorageClientException e) {
      LOGGER.error("Failed to check if this user was already signed up.", e);
      throw new CalendarException(500, "Could not check if you were already signed up.");
    }
  }

  /**
   * Copy the event to the user his calendar.
   *
   * @param signupNode
   *          The node that represents the signup properties for this event.
   */
  protected void copyEventNode(Content signupNode, Session session) throws CalendarException {
    try {
      // Grab the event
      ContentManager contentManager = session.getContentManager();
      Content eventNode = contentManager.get(StorageClientUtils.getParentObjectPath(signupNode.getPath()));
      String userId = session.getUserId();

      String parentPath = StorageClientUtils.getParentObjectPath(eventNode.getPath());
      boolean found = false;
      while (!found && !StorageClientUtils.isRoot(parentPath)) {
        Content parentContent = contentManager.get(parentPath);
        if (parentContent.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            && SAKAI_CALENDAR_RT.equals(parentContent.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))) {
          found = true;
          break;
        }
        parentPath = StorageClientUtils.getParentObjectPath(parentPath);
      }

      // Get the path that we should create.
      String path = StorageClientUtils.newPath(LitePersonalUtils.getHomePath(userId), SAKAI_CALENDAR_NODENAME);
      path = StorageClientUtils.newPath(path, eventNode.getPath().substring(parentPath.length()));

      // Create the node under the user in his own calendar.
      // Copy all the properties over.
      Content ownEventNode = contentManager.get(path);
      if ( ownEventNode == null ) {
        ownEventNode = new Content(path, eventNode.getProperties());
      } else {
        for ( Entry<String, Object> e : eventNode.getProperties().entrySet()) {
          ownEventNode.setProperty(e.getKey(),e.getValue());
        }
      }
      ownEventNode.setProperty(SAKAI_SIGNEDUP_ORIGINAL_EVENT, eventNode.getPath());
      contentManager.update(ownEventNode);
    } catch ( AccessDeniedException e) {
      LOGGER
      .warn(
          "Caught repository exception when trying to copy a calendar event to a user his calendar during signup.",
          e);
      throw new CalendarException(403,
      "Failed to copy the event to the current user his calendar. "+e.getMessage());

    } catch (StorageClientException e) {
      LOGGER
          .error(
              "Caught repository exception when trying to copy a calendar event to a user his calendar during signup.",
              e);
      throw new CalendarException(500,
          "Failed to copy the event to the current user his calendar.");
    }
  }

  /**
   * Creates a participant node at ../event/signup/s/si/simong
   *
   * @param signupNode
   *          The node that represents the signup properties for this event.
   * @throws CalendarException
   *           Something went wrong, HTTP status code and message is included.
   */
  protected void handleSignup(Content signupNode, Session session) throws CalendarException {

    try {
      // Add the user to the participant list.
      // We use the same hash that is used to hash authorizables.

      // Construct the absolute path in JCR from the signup node and the authorizable
      // hash.
      String userId = session.getUserId();
      String profilePath = LitePersonalUtils.getProfilePath(userId);
      String path = signupNode.getPath() + "/" + PARTICIPANTS_NODE_NAME + "/" + userId;
      path = PathUtils.normalizePath(path);

      Session adminSession = null;
      try {
        // login as admin so we can create a subnode.
        adminSession = repository.loginAdministrative(null);
        ContentManager contentManager = adminSession.getContentManager();

        Content content = new Content(path, ImmutableMap.of(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            (Object)SAKAI_EVENT_SIGNUP_PARTICIPANT_RT,SAKAI_USER, userId,SAKAI_SIGNEDUP_DATE, Calendar.getInstance(), SAKAI_CALENDAR_PROFILE_LINK, profilePath));
        contentManager.update(content);
      } finally {
        // Destroy the admin session.
        adminSession.logout();
      }

    } catch (AccessDeniedException e) {
      LOGGER.error(
          "Caught repository exception when trying to handle a calendar signup.", e);
      throw new CalendarException(403,
          "Failed to add current user to the participant list.");
    } catch (StorageClientException e) {
      LOGGER.error(
          "Caught repository exception when trying to handle a calendar signup.", e);
      throw new CalendarException(500,
          "Failed to add current user to the participant list.");
    }
  }


  protected void bindPreProcessor(SignupPreProcessor preProcessor) {
    signupPreProcessors.add(preProcessor);
  }

  protected void unbindPreProcessor(SignupPreProcessor preProcessor) {
    signupPreProcessors.add(preProcessor);
  }
}
