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

import static org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_UUIDS;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.sakaiproject.nakamura.api.files.FileUtils;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;

import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.http.HttpServletResponse;

@Component(immediate = true)
@Service(value = SlingPostOperation.class)
@Properties(value = {
    @Property(name = "sling.post.operation", value = "tag"),
    @Property(name = "service.description", value = "Creates an internal link to a file."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class TagOperation extends AbstractSlingPostOperation {

  @Reference
  protected SlingRepository slingRepository;

  /**
   * 
   */
  private static final long serialVersionUID = -7724827744698056843L;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.servlets.post.AbstractSlingPostOperation#doRun(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {

    // Check if the user has the required minimum privilege.
    String user = request.getRemoteUser();
    if (UserConstants.ANON_USERID.equals(user)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN,
          "Anonymous users can't tag things.");
      return;
    }

    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node tagNode = null;
    Node node = request.getResource().adaptTo(Node.class);

    if (node == null) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
          "A tag operation must be performed on an actual resource");
      return;
    }

    // Check if the uuid is in the request.
    RequestParameter uuidParam = request.getRequestParameter("uuid");
    if (uuidParam == null) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Missing uuid parameter");
      return;
    }

    // Grab the tagNode.
    String uuid = uuidParam.getString();
    try {
      tagNode = session.getNodeByIdentifier(uuid);
      if (!FileUtils.isTag(tagNode)) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
            "Provided UUID doesn't point to a tag.");
      }
    } catch (ItemNotFoundException e1) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Could not locate the tag.");
    } catch (RepositoryException e1) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Could not locate the tag.");
    }

    try {
      // We check if the node already has this tag.
      // If it does, we ignore it..
      if (!hasUuid(node, uuid)) {
        Session adminSession = null;
        try {
          adminSession = slingRepository.loginAdministrative(null);

          // Add the tag on the file.
          FileUtils.addTag(adminSession, node, tagNode);

          // Save our modifications.
          if (adminSession.hasPendingChanges()) {
            adminSession.save();
          }

        } finally {
          adminSession.logout();
        }
      }

    } catch (RepositoryException e) {
      response.setStatus(500, e.getMessage());
    }

  }

  /**
   * Checks if the node already has the uuid in it's properties.
   * 
   * @param node
   * @param uuid
   * @return
   * @throws RepositoryException
   */
  protected boolean hasUuid(Node node, String uuid) throws RepositoryException {
    Value[] uuids = JcrUtils.getValues(node, SAKAI_TAG_UUIDS);
    for (Value v : uuids) {
      if (v.getString().equals(uuid)) {
        return true;
      }
    }
    return false;
  }
}
