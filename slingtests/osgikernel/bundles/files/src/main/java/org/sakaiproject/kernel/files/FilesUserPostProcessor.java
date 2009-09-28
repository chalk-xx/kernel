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
package org.sakaiproject.kernel.files;

import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_USER_PATH;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.user.UserPostProcessor;
import org.sakaiproject.kernel.util.JcrUtils;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * This PostProcessor listens to post operations on User objects and processes the
 * changes.
 * 
 * @scr.service interface="org.sakaiproject.kernel.api.user.UserPostProcessor"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.component immediate="true" label="FilesUserPostProcessor" description="Post Processor for User and Group operations and creates the bookmarks folder for a user."
 *                metatype="no"
 * @scr.property name="service.description" value=
 *               "Post Processes User and Group operations and creates the bookmarks folder for a user."
 */
public class FilesUserPostProcessor implements UserPostProcessor {

  public void process(Session session, SlingHttpServletRequest request,
      List<Modification> changes) throws Exception {

    String resourcePath = request.getRequestPathInfo().getResourcePath();
    if (resourcePath.equals(SYSTEM_USER_MANAGER_USER_PATH)) {
      String principalName = null;
      UserManager userManager = AccessControlUtil.getUserManager(session);
      RequestParameter rpid = request
          .getRequestParameter(SlingPostConstants.RP_NODE_NAME);
      if (rpid != null) {
        principalName = rpid.getString();
        Authorizable authorizable = userManager.getAuthorizable(principalName);
        if (authorizable != null) {

          // Create a mybookmarks for this user.
          String path = PersonalUtils.getPrivatePath(authorizable.getID(), "mybookmarks");
          if (!session.itemExists(path)) {

            Node node = JcrUtils.deepGetOrCreateNode(session, path);
            node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
                "sakai/bookmarks");
            node.setProperty("files", new Value[] {});
          }
        }
      }
    }

  }

}
