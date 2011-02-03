/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.user.lite.servlet;

import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_GROUP_MANAGERS;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_GROUP_VIEWERS;
import static org.sakaiproject.nakamura.api.user.UserConstants.SYSTEM_USER_MANAGER_GROUP_PREFIX;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.resource.RequestProperty;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.user.lite.resource.LiteAuthorizableResourceProvider;
import org.sakaiproject.nakamura.user.lite.resource.LiteNameSanitizer;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Sling Post Servlet implementation for creating a group in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Creates a new group. Maps on to nodes of resourceType <code>sling/groups</code> like
 * <code>/rep:system/rep:userManager/rep:groups</code> mapped to a resource url
 * <code>/system/userManager/group</code>. This servlet responds at
 * <code>/system/userManager/group.create.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>:name</dt>
 * <dd>The name of the new group (required)</dd>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the group node (optional)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the group resource locator. The redirect comes with
 * HTML describing the status.</dd>
 * <dt>500</dt>
 * <dd>Failure, including group already exists. HTML explains the failure.</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl -F:name=newGroupA  -Fproperty1=value1 http://localhost:8080/system/userManager/group.create.html
 * </code>
 *
 * <h4>Notes</h4>
 *
 *
 */
@SlingServlet(resourceTypes={"sparse/groups"}, methods={"POST"}, selectors={"create"})
@Properties(value={
    @Property(name="servlet.post.dateFormats",
              value={"EEE MMM dd yyyy HH:mm:ss 'GMT'Z",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ", 
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd", 
                "dd.MM.yyyy HH:mm:ss",
                "dd.MM.yyyy"})})
@ServiceDocumentation(name="Create Group Servlet",
    description="Creates a new group. Maps on to nodes of resourceType sling/groups like " +
    		"/rep:system/rep:userManager/rep:groups mapped to a resource url " +
    		"/system/userManager/group. This servlet responds at /system/userManager/group.create.html",
    shortDescription="Creates a new group",
    bindings=@ServiceBinding(type=BindingType.PATH,bindings="/system/userManager/group.create.html",
        selectors=@ServiceSelector(name="create", description="Creates a new group"),
        extensions=@ServiceExtension(name="html", description="Posts produce html containing the update status")),
    methods=@ServiceMethod(name="POST",
        description={"Creates a new group with a name :name, " +
            "storing additional parameters as properties of the new group.",
            "Example<br>" +
            "<pre>curl -F:name=g-groupname -Fproperty1=value1 http://localhost:8080/system/userManager/group.create.html</pre>"},
        parameters={
        @ServiceParameter(name=":name", description="The name of the new group (required)"),
        @ServiceParameter(name="",description="Additional parameters become group node properties, " +
            "except for parameters starting with ':', which are only forwarded to post-processors (optional)")
        },
        response={
        @ServiceResponse(code=200,description="Success, a redirect is sent to the groups resource locator with HTML describing status."),
        @ServiceResponse(code=500,description="Failure, including group already exists. HTML explains failure.")
        }))

public class LiteCreateSakaiGroupServlet extends LiteAbstractSakaiGroupPostServlet implements
    ManagedService {


  /**
   *
   */
  private static final long serialVersionUID = 6587376522316825454L;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(LiteCreateSakaiGroupServlet.class);

  /**
   * Used to launch OSGi events.
   *
   */
  @Reference
  protected transient EventAdmin eventAdmin;

  /**
   * Used to create the group.
   *
   */
  @Reference
  protected transient LiteAuthorizablePostProcessService postProcessorService;

  /**
   */
  @Property(value="authenticated,everyone")
  public static final String GROUP_AUTHORISED_TOCREATE = "groups.authorized.tocreate";

  private Set<String> authorizedGroups = ImmutableSet.of();


  /*
   * (non-Javadoc)
   *
   * @seeorg.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet#
   * handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void handleOperation(SlingHttpServletRequest request,
      HtmlResponse response, List<Modification> changes) throws AuthorizableExistsException, ClientPoolException, StorageClientException, AccessDeniedException {

    // KERN-432 dont allow anon users to access create group.
    if ( User.ANON_USER.equals(request.getRemoteUser()) ) {
      response.setStatus(403, "AccessDenied");
      return;
    }

    // check that the submitted parameter values have valid values.
    final String principalName = request.getParameter(SlingPostConstants.RP_NODE_NAME);
    if (principalName == null) {
        throw new IllegalArgumentException("Group name was not submitted");
    }

    LiteNameSanitizer san = new LiteNameSanitizer(principalName, false);
    san.validate();

    // check for allow create Group
    boolean allowCreateGroup = false;
    User currentUser = null;

    try {
      Session currentSession = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
      AuthorizableManager authorizableManager = currentSession.getAuthorizableManager();
      currentUser = (User) authorizableManager.findAuthorizable(currentSession.getUserId());
      if (currentUser.isAdmin()) {
        LOGGER.debug("User is an admin ");
        allowCreateGroup = true;
      } else {
        LOGGER.debug("Checking for membership of one of {} ",authorizedGroups);
        for (String groupName : currentUser.getPrincipals()) {
          if (authorizedGroups.contains(groupName)) {
            allowCreateGroup = true;
            break;
          }
        }
        // TODO: Implement Dynamic Group membership checks
      }
    } catch (Exception ex) {
      LOGGER.warn("Failed to determin if the user is an admin, assuming not. Cause: "
          + ex.getMessage());
      allowCreateGroup = false;
    }

    if (!allowCreateGroup) {
      LOGGER.debug("User is not allowed to create groups ");
      response.setStatus(HttpServletResponse.SC_FORBIDDEN,
          "User is not allowed to create groups");
      return;
    }

    Session session = getSession();

        try {
            AuthorizableManager authorizableManager = session.getAuthorizableManager();
            Authorizable authorizable = authorizableManager.findAuthorizable(principalName);

            if (authorizable != null) {
                // principal already exists!
              response.setStatus(400,
                  "A principal already exists with the requested name: " + principalName);
              return;
            } else {
                if ( authorizableManager.createGroup(principalName, principalName, null) ) {
                  Group group = (Group) authorizableManager.findAuthorizable(principalName);
                String groupPath = LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
                   + group.getId();
                Map<String, RequestProperty> reqProperties = collectContent(
                    request, response, groupPath);

                response.setPath(groupPath);
                response.setLocation(groupPath);
                response.setParentLocation(LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH);
                changes.add(Modification.onCreated(groupPath));

                Map<String, Object> toSave = Maps.newLinkedHashMap();

                // It is not allowed to touch the rep:group-managers property directly.
                String key = SYSTEM_USER_MANAGER_GROUP_PREFIX + principalName + "/";
                reqProperties.remove(key + PROP_GROUP_MANAGERS);
                reqProperties.remove(key + PROP_GROUP_VIEWERS);

                // write content from form
                writeContent(session, group, reqProperties, changes, toSave);

                dumpToSave(toSave,"after write content");
                // update the group memberships, although this uses session from the request, it
                // only
                // does so for finding authorizables, so its ok that we are using an admin session
                // here.
                updateGroupMembership(request, session, group, changes, toSave);

                dumpToSave(toSave, " after update group membership");

                // TODO We should probably let the client decide whether the
                // current user belongs in the managers list or not.
                updateOwnership(request, group, new String[] {currentUser.getId()}, changes, toSave);

                dumpToSave(toSave, "before save");

                saveAll(session, toSave);
                try {
                  postProcessorService.process(group, session, ModificationType.CREATE, request);
                } catch (Exception e) {
                  LOGGER.warn(e.getMessage(), e);
                  response
                     .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                  return;
                }

                // Launch an OSGi event for creating a group.
                try {
                  Dictionary<String, String> properties = new Hashtable<String, String>();
                  properties.put(UserConstants.EVENT_PROP_USERID, principalName);
                  EventUtils
                      .sendOsgiEvent(properties, UserConstants.TOPIC_GROUP_CREATED, eventAdmin);
                } catch (Exception e) {
                  // Trap all exception so we don't disrupt the normal behaviour.
                  LOGGER.error("Failed to launch an OSGi event for creating a user.", e);
                }
                } else {
                  throw new AuthorizableExistsException("Failed to create group, already exists ");
                }
            }
        } finally {
            ungetSession(session);
        }
  }





  /** Returns the JCR repository used by this service. */
  @Override
  protected Repository getRepository() {
    return repository;
  }

  /**
   * Returns an administrative session to the default workspace.
   * @throws AccessDeniedException 
   * @throws StorageClientException 
   * @throws ClientPoolException 
   */
  private Session getSession() throws ClientPoolException, StorageClientException, AccessDeniedException {
    return getRepository().loginAdministrative();
  }

  /**
   * Return the administrative session and close it.
   */
  private void ungetSession(final Session session) {
    if (session != null) {
      try {
        session.logout();
      } catch (Throwable t) {
        LOGGER.error("Unable to log out of session: " + t.getMessage(), t);
      }
    }
  }

  // ---------- SCR integration ---------------------------------------------

  /**
   * Activates this component.
   *
   * @param componentContext
   *          The OSGi <code>ComponentContext</code> of this component.
   */
  @Override
  protected void activate(ComponentContext componentContext) {
    super.activate(componentContext);
    String groupList = (String) componentContext.getProperties().get(
        GROUP_AUTHORISED_TOCREATE);
    if (groupList != null) {
      authorizedGroups = ImmutableSet.of(StringUtils.split(groupList, ','));
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("rawtypes")
  public void updated(Dictionary dictionary) throws ConfigurationException {
    String groupList = (String) dictionary.get(GROUP_AUTHORISED_TOCREATE);
    if (groupList != null) {
      authorizedGroups = ImmutableSet.of(StringUtils.split(groupList, ','));
    }
  }

}
