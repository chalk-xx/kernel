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
package org.sakaiproject.nakamura.files.servlets;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.files.FileUtils;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "MyManagedContentServlet", shortDescription = "Returns information about the current active user.", description = "Presents information about current user in JSON format.", bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/system/me"), methods = @ServiceMethod(name = "GET", description = "Get information about current user.", response = {
    @ServiceResponse(code = 200, description = "Request for information was successful. <br />"),
    @ServiceResponse(code = 401, description = "Unauthorized: credentials provided were not acceptable to return information for."),
    @ServiceResponse(code = 500, description = "Unable to return list of user's managed content.") }))
@SlingServlet(paths = { "/var/search/pool/me/manager" }, generateComponent = true, generateService = true, methods = { "GET" })
public class MyManagedContentServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -3786472219389695181L;
  private static final Logger LOG = LoggerFactory.getLogger(MyManagedContentServlet.class);

  private static final int searchLimit = 100;

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
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(javax.jcr.Session.class);
      Session session =
        StorageClientUtils.adaptToSession(jcrSession);
      List<Content> contentResults = new ArrayList<Content>();
      AuthorizableManager am = session.getAuthorizableManager();
      ContentManager cm = session.getContentManager();
      Authorizable currentUser = am.findAuthorizable(session.getUserId());
      Iterator<Group> allGroupsIter = currentUser.memberOf(am);
      while(allGroupsIter.hasNext()) {
        Group group = allGroupsIter.next();
        if (!group.getId().equals(Group.EVERYONE) && group.hasProperty(UserConstants.PROP_MANAGED_GROUP)) {
          contentResults.addAll(findManagedContent(cm, (String)group.getProperty(UserConstants.PROP_MANAGED_GROUP), searchLimit));
        }
      }
      contentResults.addAll(findManagedContent(cm, currentUser.getId(), searchLimit));
      PrintWriter w = response.getWriter();
      ExtendedJSONWriter writer = new ExtendedJSONWriter(w);
      writer.object();
      int contentCount = 0;
      writer.key("items");
      writer.value(255);

      writer.key("results");
      writer.array();
      if (contentResults != null) {
        for (Content content : contentResults) {
          FileUtils.writeFileNode(content, session, writer);
          contentCount++;
        }
      }
      writer.endArray();
      writer.key("total");
      writer.value(contentCount);

      writer.endObject();
    } catch (JSONException e) {
      LOG.error("Failed to create proper JSON response in /var/search/pool/me/manager", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to create proper JSON response.");
    } catch (StorageClientException e) {
      LOG.error("Failed to get a user his profile node in /var/search/pool/me/manager", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Sparse storage client error.");
    } catch (AccessDeniedException e) {
      LOG.error("Failed to get a user his profile node in /var/search/pool/me/manager", e);
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "Access denied error.");
    }

  }

  private Collection<? extends Content> findManagedContent(ContentManager cm, String id, int maxItems) throws StorageClientException, AccessDeniedException {
    List<Content> contentSearchResults = Lists.newArrayList();
    for (Content searchResult : cm.find(ImmutableMap.of("sling:resourceType", (Object)"sakai/pooled-content", "sakai:pooled-content-manager", id))) {
      if (contentSearchResults.size() < maxItems) {
        contentSearchResults.add(searchResult);
      } else {
        break;
      }
    }
    return contentSearchResults;
  }

}
