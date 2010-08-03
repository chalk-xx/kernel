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
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

@Component(immediate = true, description = "Post Processor for User and Group operations, specifically creating a site for users.", metatype = true, label = "SiteAuthorizablePostProcessor")
@Service(value = AuthorizablePostProcessor.class)
@Properties(value = {
    @org.apache.felix.scr.annotations.Property(name = "service.vendor", value = "The Sakai Foundation"),
    @org.apache.felix.scr.annotations.Property(name = "service.description", value = "Post Processes User and Group operations, specifically creating a site for users.") })
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
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#getSequence()
   */
  public int getSequence() {
    // Should get executed AFTER the PersonalAuthorizablePostProcessor!
    return 100;
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
          Node siteNode = session.getNode(sitePath);
          // The name is
          siteNode.setProperty(SAKAI_SITE_NAME, authorizable.getID());
        }
      }
    }
  }

}
