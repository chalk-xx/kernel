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

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.user.LiteAuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletResponse;

/**
 * Sling Post Operation implementation for deleting one or more users and/or groups from the
 * jackrabbit UserManager.

 * <h2>Rest Service Description</h2>
 * <p>
 * Deletes an Authorizable, currently a user or a group. Maps on to nodes of resourceType <code>sling/users</code> or <code>sling/users</code> like
 * <code>/rep:system/rep:userManager/rep:users</code> or <code>/rep:system/rep:userManager/rep:groups</code> mapped to a resource url
 * <code>/system/userManager/user</code> or <code>/system/userManager/group</code>. This servlet responds at
 * <code>/system/userManager/user.delete.html</code> or <code>/system/userManager/group.delete.html</code>.
 * The servlet also responds to single delete requests eg <code>/system/userManager/group/newGroup.delete.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>:applyTo</dt>
 * <dd>An array of relative resource references to Authorizables to be deleted, if this parameter is present, the url is ignored and all the Authorizables in the list are removed.</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, no body.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found</dd>
 * <dt>500</dt>
 * <dd>Failure</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl -Fgo=1 http://localhost:8080/system/userManager/user/ieb.delete.html
 * </code>
 *
 *
 */
@SlingServlet(resourceTypes={"sparse/authorizable","sparse/user","sparse/group","sparse/userManager"}, methods={"POST"}, selectors={"delete"})
@Properties(value = {
    @Property(name = "password.digest.algorithm", value = "sha1"),
    @Property(name = "servlet.post.dateFormats", value = {
        "EEE MMM dd yyyy HH:mm:ss 'GMT'Z", "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd", "dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy" })})

@ServiceDocumentation(name="Delete Authorizable (Group and User) Servlet", okForVersion = "0.11",
    description="Deletes a user or group. Maps on to nodes of resourceType sparse/userManager like " +
    		"/system/userManager. This servlet responds at " +
    		"/system/userManager.delete.html. The servlet also responds to single delete " +
    		"requests at nodes of type sparse/user and sparse/group eg /system/userManager/group/math101.delete.html" +
        "For deleting a list of users or groups, use the :applyTo parameter with a list of authorizable ids.",
    shortDescription="Delete a group or user",
    bindings=@ServiceBinding(type=BindingType.TYPE, bindings={"sparse/group", "sparse/user"},
        selectors=@ServiceSelector(name="delete", description="Deletes one or more authorizables (groups or users)"),
        extensions=@ServiceExtension(name="html", description="Posts produce html containing the update status")),
    methods=@ServiceMethod(name="POST",
        description={"Delete a group or user, or set of groups.",
            "Example<br>" +
            "<pre>curl -Fgo=1 http://localhost:8080/system/userManager/group/math101.delete.html</pre>"},
        parameters={
        @ServiceParameter(name=":applyTo", description="An array of relative resource references to groups to be deleted, if this parameter is present, the url is ignored and all listed groups are removed.")
    },
    response={
    @ServiceResponse(code=200,description="Success, a redirect is sent to the group's resource locator with HTML describing status."),
    @ServiceResponse(code=404,description="Group or User was not found."),
    @ServiceResponse(code=500,description="Failure with HTML explanation.")
        }))
public class LiteDeleteSakaiAuthorizableServlet extends LiteAbstractAuthorizablePostServlet {

  /**
   *
   */
  private static final long serialVersionUID = 3417673949322305891L;


  private static final Logger LOGGER = LoggerFactory.getLogger(LiteDeleteSakaiAuthorizableServlet.class);


  @Reference
  protected transient LiteAuthorizablePostProcessService postProcessorService;

  @Reference
  protected transient EventAdmin eventAdmin;

  /**
   * {@inheritDoc}
   * @see org.apache.sling.jackrabbit.usermanager.post.CreateUserServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void handleOperation(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes)  {

    Iterator<Resource> res = getApplyToResources(request);
    Collection<Authorizable> authorizables = new HashSet<Authorizable>();

    if (res == null) {
        Resource resource = request.getResource();
        Authorizable item = resource.adaptTo(Authorizable.class);
        if (item == null) {
            String msg = "Missing source " + resource.getPath()
                + " for delete";
            response.setStatus(HttpServletResponse.SC_NOT_FOUND, msg);
            throw new ResourceNotFoundException(msg);
        }
        authorizables.add(item);
    } else {
        while (res.hasNext()) {
            Resource resource = res.next();
            Authorizable item = resource.adaptTo(Authorizable.class);
            if (item != null) {
              authorizables.add(item);
            }
        }
    }

    LOGGER.debug("Will delete {} ",authorizables);
    
    Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
    Map<String, Boolean> authorizableEvents = new HashMap<String, Boolean>();
    try {
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      for ( Authorizable authorizable : authorizables) {
        LOGGER.debug("Deleting {} ",authorizable.getId());
        authorizableEvents.put(authorizable.getId(), (authorizable instanceof Group));
        postProcessorService.process(authorizable, session, ModificationType.DELETE, request);
        authorizableManager.delete(authorizable.getId());
      }
      // Launch an OSGi event for each authorizable.
      for (Entry<String, Boolean> entry : authorizableEvents.entrySet()) {
        try {
          Dictionary<String, String> properties = new Hashtable<String, String>();
          properties.put(UserConstants.EVENT_PROP_USERID, entry.getKey());
          String topic = UserConstants.TOPIC_USER_DELETED;
          if (entry.getValue()) {
            topic = UserConstants.TOPIC_GROUP_DELETED;
          }
          EventUtils.sendOsgiEvent(properties, topic, eventAdmin);
        } catch (Exception e) {
          // Trap all exception so we don't disrupt the normal behaviour.
          LOGGER.error("Failed to launch an OSGi event for creating a user.", e);
        }
      }
    } catch ( AccessDeniedException e) {
      if ( LOGGER.isDebugEnabled() ) {
        LOGGER.debug(e.getMessage(),e);
      } else {
        LOGGER.warn(e.getMessage());
      }
      response.setStatus(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
      return;
    } catch (Exception e) {
      // undo any changes
      LOGGER.warn(e.getMessage(),e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }
  }


  /**
   * Returns an iterator on <code>Resource</code> instances addressed in the
   * {@link SlingPostConstants#RP_APPLY_TO} request parameter. If the request
   * parameter is not set, <code>null</code> is returned. If the parameter is
   * set with valid resources an empty iterator is returned. Any resources
   * addressed in the {@link SlingPostConstants#RP_APPLY_TO} parameter is
   * ignored.
   *
   * @param request The <code>SlingHttpServletRequest</code> object used to
   *            get the {@link SlingPostConstants#RP_APPLY_TO} parameter.
   * @return The iterator of resources listed in the parameter or
   *         <code>null</code> if the parameter is not set in the request.
   */
  protected Iterator<Resource> getApplyToResources(
          SlingHttpServletRequest request) {

      String[] applyTo = request.getParameterValues(SlingPostConstants.RP_APPLY_TO);
      if (applyTo == null) {
          return null;
      }

      return new ApplyToIterator(request, applyTo);
  }

  private static class ApplyToIterator implements Iterator<Resource> {

      private final ResourceResolver resolver;

      private final Resource baseResource;

      private final String[] paths;

      private int pathIndex;

      private Resource nextResource;

      ApplyToIterator(SlingHttpServletRequest request, String[] paths) {
          this.resolver = request.getResourceResolver();
          this.baseResource = request.getResource();
          this.paths = paths;
          this.pathIndex = 0;

          nextResource = seek();
      }

      public boolean hasNext() {
          return nextResource != null;
      }

      public Resource next() {
          if (!hasNext()) {
              throw new NoSuchElementException();
          }

          Resource result = nextResource;
          nextResource = seek();

          return result;
      }

      public void remove() {
          throw new UnsupportedOperationException();
      }

      private Resource seek() {
          while (pathIndex < paths.length) {
              String path = paths[pathIndex];
              pathIndex++;

              Resource res = resolver.getResource(baseResource, path);
              if (res != null) {
                  return res;
              }
          }

          // no more elements in the array
          return null;
      }
  }


}
