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
package org.sakaiproject.kernel.files.servlets;

import static org.sakaiproject.kernel.api.files.FilesConstants.REQUIRED_MIXIN;
import static org.sakaiproject.kernel.api.files.FilesConstants.RT_SAKAI_LINK;
import static org.sakaiproject.kernel.api.files.FilesConstants.SAKAI_LINK;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceParameter;
import org.sakaiproject.kernel.api.doc.ServiceResponse;
import org.sakaiproject.kernel.api.doc.ServiceSelector;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Create an interal jcr link to a file.
 */
@SlingServlet(methods = { "POST" }, selectors = { "link" }, resourceTypes = { "sling/servlet/default" })
@Properties(value = {
    @Property(name = "service.description", value = "Creates an internal link to a file."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
@ServiceDocumentation(name = "FileCreateLinkServlet", shortDescription = "Create an interal jcr link to a file.", bindings = @ServiceBinding(type = BindingType.TYPE, selectors = @ServiceSelector(name = "link", description = "Create an interal jcr link to a file."), bindings = "sakai/file"), methods = { @ServiceMethod(name = "POST", description = "Create one or more links for a file.", parameters = {
    @ServiceParameter(name = "link", description = "Required: absolute path where you want to create a link for the file. "
        + "This can be multivalued. It has to be the same size as the site parameter though."),
    @ServiceParameter(name = "site", description = "Required: absolute path to a site that should be associated with this file. "
        + "This can be multivalued. It has to be the same size as the link parameter though.") }, response = {
    @ServiceResponse(code = 200, description = "Everything went OK.<br />"
        + "The body will also contain a JSON response that lists all the links and if they were sucesfully created or not."),
    @ServiceResponse(code = 400, description = "Filedata parameter was not provided."),
    @ServiceResponse(code = 500, description = "Failure with HTML explanation.") }) })
public class FileCreateLinkServlet extends SlingAllMethodsServlet {

  public static final Logger log = LoggerFactory.getLogger(FileCreateLinkServlet.class);
  private static final long serialVersionUID = -6206802633585722105L;
  private static final String LINK_PARAM = "link";
  private static final String SITE_PARAM = "site";

  @Reference
  protected transient SlingRepository slingRepository;
  @Reference
  protected transient SiteService siteService;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    String link = request.getParameter(LINK_PARAM);
    String site = request.getParameter(SITE_PARAM);
    Resource resource = request.getResource();
    Session session = request.getResourceResolver().adaptTo(Session.class);

    if ("anon".equals(request.getRemoteUser())) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
          "Anonymous users can't link things.");
      return;
    }
    if (link == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "A link parameter has to be provided.");
      return;
    }
    if (resource == null || resource instanceof NonExistingResource) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The link operation can't be done on NonExisting Resources.");
      return;
    }

    if (site != null) {
      try {
        Node siteNode = (Node) session.getItem(site);
        if (siteNode == null || !siteService.isSite(siteNode)) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST,
              "The site parameter doesn't point to a valid site.");
          return;
        }
      } catch (RepositoryException e) {
        // We assume it went wrong because of a bad parameter.
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "The site parameter doesn't point to a valid site.");
        return;
      }

    }

    try {
      Node node = resource.adaptTo(Node.class);
      boolean hasMixin = JcrUtils.hasMixin(node, REQUIRED_MIXIN);
      if (!hasMixin || site != null) {
        // The required mixin is not on the node.
        // Set it.
        Session adminSession = null;
        try {
          adminSession = slingRepository.loginAdministrative(null);

          // Grab the node via the adminSession
          String path = resource.getPath();
          Node adminNode = (Node) adminSession.getItem(path);
          if (!hasMixin) {
            adminNode.addMixin(REQUIRED_MIXIN);
          }

          // Used in a site.
          if (site != null) {
            JcrUtils.addUniqueValue(adminSession, adminNode, "sakai:sites", site,
                PropertyType.STRING);
          }

          if (adminSession.hasPendingChanges()) {
            adminSession.save();
          }
        } finally {
          adminSession.logout();
        }
      }

      // Now that the file is referenceable, it has a uuid.
      // Use it for the link.
      // Grab the (updated) node via the user's session id.
      node = (Node) session.getItem(node.getPath());

      // Create the link
      Node linkNode = JcrUtils.deepGetOrCreateNode(session, link);
      linkNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
          RT_SAKAI_LINK);
      linkNode.setProperty(SAKAI_LINK, node.getUUID());

      // Save link.
      if (session.hasPendingChanges()) {
        session.save();
      }

    } catch (RepositoryException e) {
      log.warn("Failed to create a link.", e);
    }
  }
}
