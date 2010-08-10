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
package org.sakaiproject.nakamura.site;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.site.SiteService.SAKAI_SITE_NAME;
import static org.sakaiproject.nakamura.api.site.SiteService.SITES_CONTAINER_RESOURCE_TYPE;
import static org.sakaiproject.nakamura.api.user.UserConstants.ANON_USERID;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

@Component(immediate = true, description = "Post Processor for User and Group operations, specifically creating a site for users.", metatype = true, label = "SiteAuthorizablePostProcessor")
@Service(value = AuthorizablePostProcessor.class)
@Properties(value = {
    @org.apache.felix.scr.annotations.Property(name = "service.vendor", value = "The Sakai Foundation"),
    @org.apache.felix.scr.annotations.Property(name = "service.description", value = "Post Processes User and Group operations, specifically creating a site for users."),
    @org.apache.felix.scr.annotations.Property(name = "service.ranking", intValue = 100) })
public class SiteAuthorizablePostProcessor implements AuthorizablePostProcessor {

  static final String SITE_TEMPLATE_DEFAULT = "/var/templates/site/template";

  @Property(value = SITE_TEMPLATE_DEFAULT, description = "The path to the site template for sites that get created when users get registered.")
  static final String SITE_TEMPLATE = "org.sakaiproject.nakamura.site.authorizable.processor.template";

  @Reference
  protected transient SiteService siteService;

  private String templatePath;

  @Activate
  public void activate(Map<?, ?> properties) {
    init(properties);
  }

  @Modified
  public void modified(Map<?, ?> properties) {
    init(properties);
  }

  private void init(Map<?, ?> props) {
    templatePath = OsgiUtil.toString(props.get(SITE_TEMPLATE), SITE_TEMPLATE_DEFAULT);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#process(org.apache.jackrabbit.api.security.user.Authorizable,
   *      javax.jcr.Session, org.apache.sling.servlets.post.Modification)
   */
  public void process(Authorizable authorizable, Session session, Modification change)
      throws Exception {
    // We do not process Delete's and anonymous users.
    if (!change.getType().equals(ModificationType.DELETE)
        && !UserConstants.ANON_USERID.equals(authorizable.getID())) {
      // Add a sites node in the home folder.
      // ~/jack/sites
      String home = PersonalUtils.getHomeFolder(authorizable);
      Node sites = JcrUtils.deepGetOrCreateNode(session, home + "/sites");
      // Since AuthorizablePostProcessor's get run when the bundle gets redeployed we make
      // sure we don't do things twice.
      if (sites.isNew()) {
        sites.setProperty(SLING_RESOURCE_TYPE_PROPERTY, SITES_CONTAINER_RESOURCE_TYPE);

        // KERN-916 : Users should have a site.
        if (authorizable instanceof User) {
          // The site will be stored at ~/jack/sites/default
          String sitePath = sites.getPath() + "/" + authorizable.getID();
          siteService.createSite(session, authorizable, sitePath, templatePath, null);

          // The name of the site should be the same name as the userID.
          Node siteNode = session.getNode(sitePath);
          siteNode.setProperty(SAKAI_SITE_NAME, authorizable.getID());

          // Redo the access ACLs.
          // According to KERN-916 these have to be the same as the authprofile ones.
          // We copy them over and then set the correct 'access' property on the site.
          String profilePath = PersonalUtils.getProfilePath(authorizable);
          AccessControlManager acm = AccessControlUtil.getAccessControlManager(session);
          AccessControlPolicy[] policies = acm.getEffectivePolicies(profilePath);
          boolean anonRead = false;
          boolean everyoneRead = false;
          for (AccessControlPolicy policy : policies) {
            // We try to figure out what anon and everyone their read access is.
            if (policy instanceof AccessControlList) {
              AccessControlList list = (AccessControlList) policy;
              AccessControlEntry[] entries = list.getAccessControlEntries();
              for (AccessControlEntry entry : entries) {
                if (!anonRead) {
                  // Can't user Privilege.JCR_READ as that has the jcr namespace in it.
                  anonRead = testACEforGrantedPrivilege(entry, ANON_USERID, "jcr:read");
                }
                if (!everyoneRead) {
                  everyoneRead = testACEforGrantedPrivilege(entry, "everyone", "jcr:read");
                }
              }
              // We're only interested in the first policy.
              // The first policy is the one that applies on the profile path.
              // The second one on the home directory
              // The third one applies on root.
              break;
            }
          }

          // Set correct property.
          String access = "";
          if (anonRead && everyoneRead) {
            // The entire thing is public
            access = "everyone";
          } else if (!anonRead && everyoneRead) {
            // Only logged in users.
            access = "sakaiUsers";
          } else if (!anonRead && !everyoneRead) {
            // Only the newly created user can see it.
            access = "offline";
          }
          siteNode.setProperty("access", access);

          // Now re-apply the authz scheme.
          // We pass in null, as no new groups should be created.
          SiteAuthz authz = new SiteAuthz(siteNode, null);
          authz.applyAuthzChanges();
        }
      }
    }
  }

  /**
   * Checks if an AccessControlEntry grants a principal some privilege.
   * 
   * @param entry
   *          The entry to check.
   * @param principalToTest
   *          The principal that should be checked.
   * @param privilegeToMatch
   *          The privilege that should be looked for.
   * @return true if the principal was granted that privilege, false otherwise.
   * @throws RepositoryException
   */
  protected boolean testACEforGrantedPrivilege(AccessControlEntry entry,
      String principalToTest, String privilegeToMatch) throws RepositoryException {
    if (entry.getPrincipal().getName().equals(principalToTest)
        && AccessControlUtil.isAllow(entry)) {
      for (Privilege privilege : entry.getPrivileges()) {
        if (privilege.getName().equals(privilegeToMatch)) {
          return true;
        }
      }
    }
    return false;
  }

}
