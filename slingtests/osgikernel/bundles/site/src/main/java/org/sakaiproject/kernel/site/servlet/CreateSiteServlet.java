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
package org.sakaiproject.kernel.site.servlet;

import static org.sakaiproject.kernel.util.ACLUtils.ADD_CHILD_NODES_GRANTED;
import static org.sakaiproject.kernel.util.ACLUtils.MODIFY_PROPERTIES_GRANTED;
import static org.sakaiproject.kernel.util.ACLUtils.REMOVE_CHILD_NODES_GRANTED;
import static org.sakaiproject.kernel.util.ACLUtils.REMOVE_NODE_GRANTED;
import static org.sakaiproject.kernel.util.ACLUtils.WRITE_GRANTED;
import static org.sakaiproject.kernel.util.ACLUtils.addEntry;

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>CreateSiteServlet</code> creates new sites. . /site/container.createsite
 * /site/container/site.createsite If the node is of type of sakai/sites, then create the
 * site based on a request property If the note is not of type sakai/sites, and exists
 * make it a sakai/site
 * 
 * @scr.component immediate="true" label="CreateSiteServlet"
 *                description="Create site servlet"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="SUpports creation of sites, either from existing folders, or new folders."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sling/servlet/default"
 *               values.1="sakai/sites"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="createsite"
 * @scr.reference name="SlingRepository"
 *                interface="org.apache.sling.jcr.api.SlingRepository"
 */

public class CreateSiteServlet extends AbstractSiteServlet {

  /**
   *
   */
  private static final long serialVersionUID = -7996020354919244147L;

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateSiteServlet.class);

  private SlingRepository slingRepository;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      UserManager userManager = AccessControlUtil.getUserManager(session);
      PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);

      String resourceType = request.getResource().getResourceType();
      String sitePath = request.getRequestPathInfo().getResourcePath();
      if ("sakai/sites".equals(resourceType)) {
        RequestParameter relativePathParam = request.getRequestParameter(":sitepath");
        if (relativePathParam == null) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + ":sitepath" + " must be set to a relative path ");
          return;
        }
        String relativePath = relativePathParam.getString();
        if (StringUtils.isEmpty(relativePath)) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parameter "
              + ":sitepath" + " must be set to a relative path ");
          return;
        }

        if (sitePath.startsWith("/")) {
          sitePath = sitePath + relativePath;
        } else {
          sitePath = sitePath + "/" + relativePath;
        }
      }

      Node firstRealNode = JcrUtils.getFirstExistingNode(session, sitePath);
      // iterate upto the root looking for a site marker.
      Node siteMarker = firstRealNode;
      Set<String> principals = new HashSet<String>();
      Authorizable currentUser = userManager.getAuthorizable(request.getRemoteUser());
      PrincipalIterator principalIterator = principalManager
          .getGroupMembership(currentUser.getPrincipal());
      boolean granted = false;
      while (!"/".equals(siteMarker.getPath())) {
        if (siteMarker.hasProperty("sakai:sitegroupcreate")) {
          Property p = siteMarker.getProperty("sakai:sitegroupcreate");

          Value[] authorizableIds = p.getValues();
          for (Value authorizable : authorizableIds) {
            Authorizable grantedAuthorizable = userManager.getAuthorizable(authorizable
                .getString());
            String grantedAuthorizableName = grantedAuthorizable.getPrincipal().getName();
            if (principals.contains(grantedAuthorizableName)) {
              granted = true;
              break;
            }
            while (principalIterator.hasNext()) {
              Principal principal = principalIterator.nextPrincipal();
              if (principal.getName().equals(grantedAuthorizableName)) {
                granted = true;
                break;
              }
              principals.add(principal.getName());
            }
          }
        }
        siteMarker = siteMarker.getParent();
      }
      Session createSession = session;
      if (granted) {
        createSession = slingRepository.loginAdministrative(null);
      }

      try {

        Node siteNode = JcrUtils.deepGetOrCreateNode(createSession, sitePath);
        siteNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            "sakai/site");
        // setup the ACL's on the node.
        addEntry(siteNode.getPath(), currentUser, createSession, WRITE_GRANTED,
            REMOVE_CHILD_NODES_GRANTED, MODIFY_PROPERTIES_GRANTED,
            ADD_CHILD_NODES_GRANTED, REMOVE_NODE_GRANTED);

        try {
          JcrUtils.logItem(LOGGER, siteNode);
        } catch (JSONException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        if (createSession.hasPendingChanges()) {
          createSession.save();
        }
      } finally {
        if (granted) {
          createSession.logout();
        }
      }
    } catch (RepositoryException ex) {
      throw new ServletException(ex.getMessage(), ex);
    }

  }

  /**
   * @param slingRepository
   */
  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  /**
   * @param slingRepository
   */
  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }
}
