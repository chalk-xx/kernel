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
package org.sakaiproject.kernel.message;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

/**
 * Allows the administrator to update the email address on a message store
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sakai/messagestore"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="email"
 */
public class ChangeEmailAddressServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 3813877071190736742L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ChangeEmailAddressServlet.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.message.AbstractMessageServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request,
      org.apache.sling.api.SlingHttpServletResponse response)
      throws javax.servlet.ServletException, java.io.IOException {
    // This is the message store resource.
    Resource baseResource = request.getResource();
    String path = baseResource.getPath();
    String target = path.substring(path.lastIndexOf('/') + 1).split("\\.")[0];
    String currentUser = request.getRemoteUser();
    Session session = baseResource.getResourceResolver().adaptTo(Session.class);
    try {
      UserManager userManager = AccessControlUtil.getUserManager(session);
      User loggedInUser = (User) userManager.getAuthorizable(currentUser);
      if (!loggedInUser.isAdmin()) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
            "Only administrators can change properties of mailboxes");
        return;
      }
    
      String newAddress = request.getParameter("address");
      if (newAddress == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Must supply 'address' parameter");
        return;
      }
    
      Node mailboxNode = (Node) session.getItem(PathUtils.toInternalHashedPath(
          MessageConstants._USER_MESSAGE, target, ""));
      mailboxNode.setProperty(MessageConstants.SAKAI_EMAIL_ADDRESS, newAddress);
      session.save();
      response.setStatus(HttpServletResponse.SC_OK);
    } catch (RepositoryException e) {
      LOGGER.error("Unable to update email address", e);
    } 
  }
  
}
