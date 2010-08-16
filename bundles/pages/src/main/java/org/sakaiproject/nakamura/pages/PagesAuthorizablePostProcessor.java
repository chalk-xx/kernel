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
package org.sakaiproject.nakamura.pages;

import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.framework.Constants.SERVICE_VENDOR;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

/**
 * Initializes structured pages for new users and groups.
 */
@Component(immediate=true, metatype=true,
    description="Initializes structured pages for new users and groups",
    label="PagesAuthorizablePostProcessor")
@Service
@Properties(value = {
    @Property(name=SERVICE_VENDOR, value="The Sakai Foundation"),
    @Property(name=SERVICE_DESCRIPTION, value="Initializes structured pages for new users and groups."),
    @Property(name=SERVICE_RANKING, intValue=100) })
public class PagesAuthorizablePostProcessor implements AuthorizablePostProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(PagesAuthorizablePostProcessor.class);

  @Property(value="/var/templates/pages/systemuser", description="The default template for a user's pages", label="Default User Template")
  public static final String DEFAULT_USER_PAGES_TEMPLATE = "default.user.template";
  private String defaultUserPagesTemplate;

  @Property(value="/var/templates/pages/systemgroup", description="The default template for a group's pages", label="Default Group Template")
  public static final String DEFAULT_GROUP_PAGES_TEMPLATE = "default.group.template";
  private String defaultGroupPagesTemplate;

  public void process(Authorizable authorizable, Session session, Modification change) {
    if (ModificationType.CREATE.equals(change.getType())) {
      String home = PersonalUtils.getHomeFolder(authorizable);
      String pagesPath = home + "/pages";
      try {
        if (!session.nodeExists(pagesPath)) {
          String templatePath = null;
          if (authorizable.isGroup()) {
            // Create the new group's pages.
            templatePath = defaultGroupPagesTemplate;
          } else {
            // Create the new user's pages.
            templatePath = defaultUserPagesTemplate;
          }
          Workspace workspace = session.getWorkspace();
          workspace.copy(templatePath, pagesPath);
          LOGGER.info("Copied template from {} to {}", templatePath, pagesPath);
          initializeAccess(authorizable, session, pagesPath);
        }
      } catch (RepositoryException e) {
        LOGGER.error("Could not create default pages for " + authorizable, e);
      }
    }
  }

  //----------- OSGi integration ----------------------------

  @Activate
  protected void activate(Map<?, ?> properties) {
    init(properties);
  }

  @Modified
  protected void modified(Map<?, ?> properties) {
    init(properties);
  }

  //----------- Internal ----------------------------

  private void init(Map<?, ?> properties) {
    defaultUserPagesTemplate = OsgiUtil.toString(properties.get(DEFAULT_USER_PAGES_TEMPLATE), "");
    defaultGroupPagesTemplate = OsgiUtil.toString(properties.get(DEFAULT_GROUP_PAGES_TEMPLATE), "");
  }

  private void initializeAccess(Authorizable authorizable, Session session, String pagesPath) throws RepositoryException {
    // Start with a clean slate for each Principal.
    final String[] allPrivs = {"jcr:all"};
    final String[] readPrivs = {"jcr:read"};

    PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);

    if (authorizable.isGroup()) {
      // By default, group managers have complete access to the group's pages and
      // other group members only have read access.
      Principal membersPrincipal = authorizable.getPrincipal();
      Principal managersPrincipal = null;
      if (authorizable.hasProperty(UserConstants.PROP_MANAGERS_GROUP)) {
        String managersGroupId = authorizable.getProperty(UserConstants.PROP_MANAGERS_GROUP)[0].getString();
        managersPrincipal = principalManager.getPrincipal(managersGroupId);
        if (managersPrincipal != null) {
          AccessControlUtil.replaceAccessControlEntry(session, pagesPath, managersPrincipal,
              allPrivs, new String[] {}, allPrivs, null);
        } else {
          LOGGER.info("No managers group found for new group {}", authorizable.getID());
        }
      }
      AccessControlUtil.replaceAccessControlEntry(session, pagesPath, membersPrincipal,
          readPrivs, new String[] {}, allPrivs, null);
    } else {
      // Give new users complete control over their own pages.
      Principal userPrincipal = authorizable.getPrincipal();
      AccessControlUtil.replaceAccessControlEntry(session, pagesPath, userPrincipal,
          allPrivs, new String[] {}, allPrivs, null);
    }

    // By default, no one except the user can see the new user's pages, and no one
    // outside the group can see the new group's pages. We need to call out
    // "everyone" and "anonymous" separately since they might have been given
    // separate grants up the inheritance tree.
    Principal everyonePrincipal = principalManager.getPrincipal("everyone");
    AccessControlUtil.replaceAccessControlEntry(session, pagesPath, everyonePrincipal,
        new String[] {}, allPrivs, allPrivs, null);
    Principal anonymousPrincipal = principalManager.getPrincipal("anonymous");
    AccessControlUtil.replaceAccessControlEntry(session, pagesPath, anonymousPrincipal,
        new String[] {}, allPrivs, allPrivs, null);
  }
}
