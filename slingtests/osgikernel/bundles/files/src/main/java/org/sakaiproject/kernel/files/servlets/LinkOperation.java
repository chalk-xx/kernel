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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostOperation;
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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

/**
 * Create an interal jcr link to a file.
 */
@Component(immediate = true)
@Service(value = SlingPostOperation.class)
@Properties(value = {
    @Property(name = "sling.post.operation", value = "link"),
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
public class LinkOperation extends AbstractSlingPostOperation {

  public static final Logger log = LoggerFactory.getLogger(LinkOperation.class);
  private static final long serialVersionUID = -6206802633585722105L;
  private static final String LINK_PARAM = "link";
  private static final String SITE_PARAM = "site";

  @Reference
  protected transient SlingRepository slingRepository;
  @Reference
  protected transient SiteService siteService;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.servlets.post.AbstractSlingPostOperation#doRun(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {

    String link = request.getParameter(LINK_PARAM);
    String site = request.getParameter(SITE_PARAM);
    Resource resource = request.getResource();
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node node = (Node) session.getItem(resource.getPath());
    if ("anon".equals(request.getRemoteUser())) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN,
          "Anonymous users can't link things.");
      return;
    }
    if (resource == null || ResourceUtil.isNonExistingResource(resource)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
          "A tag operation must be performed on an actual resource");
      return;
    }
    if (link == null) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
          "A link parameter has to be provided.");
      return;
    }
    if (node == null || resource == null || resource instanceof NonExistingResource) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
          "The link operation can't be done on NonExisting Resources.");
      return;
    }

    if (site != null) {
      try {
        Node siteNode = (Node) session.getNodeByUUID(site);
        if (siteNode == null || !siteService.isSite(siteNode)) {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
              "The site parameter doesn't point to a valid site.");
          return;
        }
      } catch (RepositoryException e) {
        // We assume it went wrong because of a bad parameter.
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
            "The site parameter doesn't point to a valid site.");
        return;
      }

    }

    try {
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

      // Grab the content of node.
      Node contentNode = node;
      if (node.hasNode(JcrConstants.JCR_CONTENT)) {
        contentNode = node.getNode(JcrConstants.JCR_CONTENT);
      }

      // Create the link
      Node linkNode = JcrUtils.deepGetOrCreateNode(session, link, "nt:linkedFile");
      linkNode.setProperty(JcrConstants.JCR_CONTENT, contentNode);
      linkNode.addMixin(REQUIRED_MIXIN);
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
