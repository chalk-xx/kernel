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
package org.sakaiproject.kernel.cluster;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.kernel.api.cluster.ClusterTrackingService;
import org.sakaiproject.kernel.api.cluster.ClusterUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * This rest end point is restricted to users that can read the resource and optionally to
 * requests that have embeded a shared trusted token in their request. It is presented
 * with a user cookie, and responds with the user object for that cookie.
 * </p>
 * <p>
 * Trusted tokens are stored in the multi value property
 * <code>sakai:shared-token<code> and if this is present
 * requests must provide one of those tokens in the http header <code>Sakai-Trust-Token</code>
 * .
 * </p>
 * <p>
 * The servlet translated the cookie SAKAI-TRACKING on client requests into a User object.
 * This cookie is provided in a request parameter <code>c</code>
 * </p>
 * <p>
 * The response is of the form
 * </p>
 * 
 * <pre>
 * {
 *   &quot;server&quot;: &quot;16935@x43543-2.local&quot;,  // the server generating the response
 *   &quot;user&quot;: {
 *     &quot;lastUpdate&quot;: 1253499599589,
 *     &quot;homeServer&quot;: &quot;otherServerId&quot;,
 *     &quot;id&quot;: &quot;ieb&quot;,
 *     &quot;principal&quot;: &quot;principal:ieb&quot;,
 *     &quot;properties&quot;: {
 *       &quot;prop1&quot;: [
 *         &quot;tokenA&quot;,
 *         &quot;tokenB&quot;,
 *         &quot;tokenC&quot;
 *       ],
 *       &quot;prop2&quot;: &quot;tokenA&quot;,
 *       &quot;prop3&quot;: [
 *       ]
 *     },
 *     &quot;principals&quot;: [
 *       &quot;principal:A&quot;,
 *       &quot;principal:B&quot;
 *     ],
 *     &quot;declaredMembership&quot;: [
 *       &quot;group:A&quot;,
 *       &quot;group:B&quot;
 *     ],
 *     &quot;membership&quot;: [
 *       &quot;indirectgroup:A&quot;,
 *       &quot;indirectgroup:B&quot;
 *     ]
 *   }
 * }
 * </pre>
 */
@SlingServlet(generateComponent = true, generateService = true, selectors = { "user" }, extensions = { "json" }, resourceTypes = { "sakai/cluster-users" })
public class ClusterUserServlet extends SlingSafeMethodsServlet {

  
  /**
   * 
   */
  private static final long serialVersionUID = 5013072672247175850L;
  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUserServlet.class);

  @Reference
  private ClusterTrackingService clusterTrackingService;
  private UserManager testingUserManager;

  
  public ClusterUserServlet() {
    
  }
  
  /**
   * Constructor for testing purposes only.
   * @param clusterTrackingService
   */
  protected ClusterUserServlet(ClusterTrackingService clusterTrackingService, UserManager userManager) {
    this.clusterTrackingService = clusterTrackingService;
    this.testingUserManager = userManager;
  }
  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Node node = request.getResource().adaptTo(Node.class);

      
      String trackingCookie = request.getParameter("c");
      ClusterUser clusterUser = clusterTrackingService.getUser(trackingCookie);
      if ( clusterUser == null ) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Cookie is not registered");
        return;
      }
      
      String serverId = clusterTrackingService.getCurrentServerId();
      UserManager userManager = null;
      if ( this.testingUserManager != null ) {
        userManager = testingUserManager;
      } else {
        userManager = AccessControlUtil.getUserManager(node.getSession());
      }
      User user = (User) userManager.getAuthorizable(clusterUser.getUser());
      JSONWriter jsonWriter = new JSONWriter(response.getWriter());
      jsonWriter.setTidy(true);
      jsonWriter.object();
      jsonWriter.key("server").value(serverId); // server
      
      jsonWriter.key("user").object();
      jsonWriter.key("lastUpdate").value(clusterUser.getLastModified());
      jsonWriter.key("homeServer").value(clusterUser.getServerId());
      jsonWriter.key("id").value(user.getID());
      jsonWriter.key("principal").value(user.getPrincipal().getName());
      jsonWriter.key("properties").object();
      for ( Iterator<?> pi = user.getPropertyNames(); pi.hasNext();) {
        String propertyName = (String) pi.next();
        jsonWriter.key(propertyName);
        Value[] propertyValues = user.getProperty(propertyName);
        if ( propertyValues.length == 1 ) {
          jsonWriter.value(propertyValues[0].getString());
        } else {
          jsonWriter.array();
          for ( Value v : propertyValues ) {
            jsonWriter.value(v.getString());
          }
          jsonWriter.endArray();
        }
      }
      jsonWriter.endObject(); // properties
     
      
      jsonWriter.key("principals").array();
      for ( PrincipalIterator pi = user.getPrincipals(); pi.hasNext(); ) {
        jsonWriter.value(pi.nextPrincipal().getName());
      }
      jsonWriter.endArray();
      
      jsonWriter.key("declaredMembership").array();
      for ( Iterator<?> gi = user.declaredMemberOf(); gi.hasNext();) {
        jsonWriter.value(((Authorizable) gi.next()).getID());
      }
      jsonWriter.endArray();
      
      jsonWriter.key("membership").array();
      for ( Iterator<?> gi = user.memberOf(); gi.hasNext(); ) {
        jsonWriter.value(((Authorizable) gi.next()).getID());
      }
      jsonWriter.endArray();
      
      
      jsonWriter.endObject(); // user
      jsonWriter.endObject();
      
      
    } catch (RepositoryException e) {
      LOGGER.error("Failed to get users " + e.getMessage(), e);
      throw new ServletException(e.getMessage());
    } catch (JSONException e) {
      LOGGER.error("Failed to get users " + e.getMessage(), e);
      throw new ServletException(e.getMessage());
    }
  }
}
