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
package org.sakaiproject.nakamura.sitetemplate;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = { "POST" }, resourceTypes = { "sakai/sites" }, selectors = { "template" }, extensions = "json", generateComponent = true, generateService = true)
public class CreateSiteServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 6687687185254684084L;
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateSiteServlet.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    Node templateNode = null;
    String sitePath = null;
    JSONObject siteJSON = null;
    ResourceResolver resolver = request.getResourceResolver();
    Session session = resolver.adaptTo(Session.class);

    try {
      String templatePath = request.getRequestParameter("template").getString();
      templateNode = (Node) session.getItem(templatePath);

      RequestParameter pathParam = request.getRequestParameter("path");
      sitePath = pathParam.getString("UTF-8");
      RequestParameter siteParam = request.getRequestParameter("site");
      siteJSON = new JSONObject(siteParam.getString("UTF-8"));

    } catch (JSONException e) {
      LOGGER.error("Could not convert JSON", e);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The provided json was invalid.");
      return;
    } catch (RepositoryException e) {
      LOGGER.error("Could not convert JSON", e);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The path to the template is invalid.");
      return;
    }

    try {
      TemplateBuilder builder = new TemplateBuilder(templateNode, siteJSON, resolver);

      // Create the groups.
      createGroups(builder, session);

      // Create the site structure.
      Node siteNode = JcrUtils.deepGetOrCreateNode(session, sitePath);
      createSiteStructure(builder, siteNode);

      // Save everything.
      if (session.hasPendingChanges()) {
        session.save();
      }

    } catch (Exception e) {
      LOGGER.error("Could not create site via templating engine.", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }
  }

  /**
   * Creates the structure for the site.
   * 
   * @param builder
   * @param siteNode
   * @throws RepositoryException
   */
  private void createSiteStructure(TemplateBuilder builder, Node siteNode)
      throws RepositoryException {
    Map<String, Object> structure = builder.getSiteMap();
    handleNode(structure, siteNode);
  }

  /**
   * Converts a map into a node structure.
   * 
   * @param structure
   *          A Map that represents the node structure. The values can be of the following
   *          types: Value, Value[], Map<String, Object>
   * @param node
   *          The node where the map should be applied on.
   * @throws RepositoryException
   */
  @SuppressWarnings("unchecked")
  private void handleNode(Map<String, Object> structure, Node node)
      throws RepositoryException {
    for (Entry<String, Object> entry : structure.entrySet()) {

      // Handle the properties ..
      if (entry.getValue() instanceof Value) {
        node.setProperty(entry.getKey(), (Value) entry.getValue());
      } else if (entry.getValue() instanceof Value[]) {
        node.setProperty(entry.getKey(), (Value[]) entry.getValue());
      }

      // Handle the child nodes.
      else if (entry.getValue() instanceof Map<?, ?>) {
        Map<String, Object> map = (Map<String, Object>) entry.getValue();
        Node childNode = node.addNode(entry.getKey());
        handleNode(map, childNode);
      }

      // Handle ACEs
      // We could check getKey().equals("rep:policy")
      else if (entry.getValue() instanceof List<?>) {
        List<ACE> lst = (List<ACE>) entry.getValue();
        for (ACE ace : lst) {
          AccessControlUtil.replaceAccessControlEntry(node.getSession(), node.getPath(),
              ace.getPrincipal(), ace.getGrantedPrivileges(), ace.getDeniedPrivileges(),
              null);
        }
      }

    }
  }

  /**
   * Creates the groups in the system.
   * 
   * @param builder
   * @param session
   * @throws RepositoryException
   */
  private void createGroups(TemplateBuilder builder, Session session)
      throws RepositoryException {
    UserManager um = AccessControlUtil.getUserManager(session);
    Map<Principal, Map<String, Object>> groups = builder.getGroups();
    for (Principal groupPrincipal : groups.keySet()) {
      // Create the authorizable.
      Group group = um.createGroup(groupPrincipal);

      // Set any additional properties
      Map<String, Object> map = groups.get(groupPrincipal);
      for (Entry<String, Object> entry : map.entrySet()) {
        if (entry.getValue() instanceof Value) {
          group.setProperty(entry.getKey(), (Value) entry.getValue());
        } else if (entry.getValue() instanceof Value[]) {
          group.setProperty(entry.getKey(), (Value[]) entry.getValue());
        }
      }
    }

  }

}
