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
package org.sakaiproject.kernel.site;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.kernel.api.site.SiteException;
import org.sakaiproject.kernel.api.site.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceImpl</code> provides a Site Service implementatoin.
 * 
 * @scr.component immediate="true" label="SiteService"
 *                description="Sakai Site Service implementation"
 * @scr.service interface="org.sakaiproject.kernel.api.site.SiteService"
 * @scr.property name="service.description"
 *               value="Provides a site service to manage sites."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @src.reference name="userManager" bind="bindUserManager" unbind="unbindUserManager"
 * @src.reference name="eventAdmin" bind="bindEventAdmin" unbind="unbindEventAdmin"
 */
public class SiteServiceImpl implements SiteService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SiteServiceImpl.class);

  /**
   * The default site template, used when none has been defined.
   */
  public static final String DEFAULT_SITE = "/sites/default.html";

  /**
   * The user manager implementation.
   */
  private UserManager userManager;

  /**
   * The OSGi Event Admin Service.
   */
  private EventAdmin eventAdmin;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.site.SiteService#isSite(javax.jcr.Item)
   */
  public boolean isSite(Item site) {
    try {
      if (site instanceof Node) {
        Node n = (Node) site;
        if (n.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            && SiteService.SITE_RESOURCE_TYPE.equals(n
                .getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))) {
          return true;
        }
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.site.SiteService#joinSite(javax.jcr.Node,
   *      java.lang.String, java.lang.String)
   */
  public void joinSite(Node site, String requestedGroup) throws SiteException {
    try {
      Session session = site.getSession();
      String user = session.getUserID();
      Authorizable userAuthorizable = userManager.getAuthorizable(user);
      if (isMember(site, userAuthorizable)) {
        throw new SiteException(HttpServletResponse.SC_CONFLICT,
            "The Current user is already a member of the site.");
      }
      /*
       * Is the site joinable
       */
      Joinable siteJoin = getJoinable(site);

      /*
       * is the group associated ?
       */
      Authorizable authorizable = userManager.getAuthorizable(requestedGroup);
      if (!(authorizable instanceof Group)) {
        throw new SiteException(HttpServletResponse.SC_BAD_REQUEST,
            "The target group must be specified in the " + SiteService.PARAM_GROUP
                + " post parameter");
      }

      Group targetGroup = (Group) authorizable;
      /*
       * Is the group joinable.
       */
      Joinable groupJoin = getJoinable(targetGroup);

      if (!isMember(site, targetGroup)) {
        throw new SiteException(
            HttpServletResponse.SC_BAD_REQUEST,
            "The target group "
                + targetGroup.getPrincipal().getName()
                + " is not a member of the site, so we cant join the site in the target group.");
      }
      /*
       * The target group is a member of the site, so we should be able to join that
       * group.
       */
      if (Joinable.no.equals(groupJoin)) {
        throw new SiteException(HttpServletResponse.SC_CONFLICT,
            "The group is not joinable.");
      } else if (Joinable.no.equals(siteJoin)) {
        throw new SiteException(HttpServletResponse.SC_CONFLICT,
            "The site is not joinable.");
      }

      if (Joinable.yes.equals(groupJoin) && Joinable.yes.equals(siteJoin)) {
        targetGroup.addMember(userAuthorizable);
        postEvent(SiteEvent.joinedSite, site, targetGroup);

      } else {
        startJoinWorkfow(site, targetGroup);
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new SiteException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.site.SiteService#unjoinSite(javax.jcr.Node,
   *      java.lang.String, java.lang.String)
   */
  public void unjoinSite(Node site, String requestedGroup) throws SiteException {
    try {
      if (isSite(site)) {
        throw new SiteException(HttpServletResponse.SC_BAD_REQUEST, site.getPath()
            + " is not a site");
      }
      Authorizable authorizable = userManager.getAuthorizable(requestedGroup);
      if (!(authorizable instanceof Group)) {
        throw new SiteException(HttpServletResponse.SC_BAD_REQUEST,
            "The target group must be specified in the " + SiteService.PARAM_GROUP
                + " post parameter");
      }
      Group targetGroup = (Group) authorizable;
      if (!isMember(site, targetGroup)) {
        throw new SiteException(HttpServletResponse.SC_CONFLICT, targetGroup
            + " is not associated with " + site.getPath());
      }

      Session session = site.getSession();
      String user = session.getUserID();
      Authorizable userAuthorizable = userManager.getAuthorizable(user);
      if (!(userAuthorizable instanceof User)) {

        throw new SiteException(HttpServletResponse.SC_CONFLICT,
            "Not a user that is known to the system: " + user);
      }

      if (!targetGroup.removeMember(userAuthorizable)) {
        throw new SiteException(HttpServletResponse.SC_CONFLICT, "User " + user
            + " was no a member of " + requestedGroup);
      }
      postEvent(SiteEvent.unjoinedSite, site, targetGroup);

    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new SiteException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getMessage());
    }

  }

  /**
   * @param site
   * @param targetGroup
   * @param user
   * @throws SiteException
   * @throws RepositoryException
   */
  public void startJoinWorkfow(Node site, Group targetGroup) throws SiteException {
    postEvent(SiteEvent.startJoinWorkflow, site, targetGroup);
  }

  /**
   * @param startJoinWorkflow
   * @param site
   * @param targetGroup
   * @throws SiteException
   */
  private void postEvent(SiteEvent event, Node site, Group targetGroup)
      throws SiteException {

    try {
      eventAdmin.postEvent(SiteEventUtil.newSiteEvent(event, site, targetGroup));
    } catch (RepositoryException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      throw new SiteException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex
          .getMessage());
    }

  }

  /**
   * @param site
   * @param targetGroup
   * @return
   */
  public boolean isMember(Node site, Authorizable targetGroup) {
    /*
     * What groups are associated with this site, and is the target group a member of one
     * of those groups.
     */
    // low cost check
    try {
      if (site.hasProperty(SiteService.AUTHORIZABLE)) {
        Value[] values = site.getProperty(SiteService.AUTHORIZABLE).getValues();
        for (Value v : values) {
          String groupName = v.getString();
          if (groupName.equals(targetGroup.getPrincipal().getName())) {
            return true;
          }
        }
        // expensive more complete check
        for (Value v : values) {
          String groupName = v.getString();
          Authorizable siteAuthorizable = userManager.getAuthorizable(groupName);
          if (siteAuthorizable instanceof Group) {
            Group siteGroup = (Group) siteAuthorizable;
            if (siteGroup.isMember(targetGroup)) {
              return true;
            }

          }
        }
      }
    } catch (RepositoryException ex) {
      LOGGER.warn(ex.getMessage(), ex);
    }
    return false;
  }

  /**
   * @param site
   * @return
   * @throws RepositoryException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
  public Joinable getJoinable(Node site) {
    try {
      if (site.hasProperty(SiteService.JOINABLE)) {
        try {
          return Joinable.valueOf(site.getProperty(SiteService.JOINABLE).getString());
        } catch (IllegalArgumentException e) {
        }
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return Joinable.no;
  }

  /**
   * @param site
   * @return
   * @throws RepositoryException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
  public Joinable getJoinable(Authorizable authorizable) {
    try {
      if (authorizable instanceof Group && authorizable.hasProperty(SiteService.JOINABLE)) {
        try {
          Value[] joinable = authorizable.getProperty(SiteService.JOINABLE);
          if (joinable != null && joinable.length > 0)
            return Joinable.valueOf(joinable[0].getString());
        } catch (IllegalArgumentException e) {
        }
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return Joinable.no;
  }

  public void bindUserManager(UserManager userManager) {
    this.userManager = userManager;
  }

  public void unbindUserManager(UserManager userManager) {
    this.userManager = null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.site.SiteService#getSiteTemplate(javax.jcr.Node)
   */
  public String getSiteTemplate(Node site) throws SiteException {
    try {
      if (site.hasProperty(SiteService.SAKAI_SITE_TEMPLATE)) {
        return site.getProperty(SiteService.SAKAI_SITE_TEMPLATE).getString();
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return DEFAULT_SITE;
  }

}
