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

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.kernel.api.site.SiteException;
import org.sakaiproject.kernel.api.site.SiteService;
import org.sakaiproject.kernel.api.site.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
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
 * @scr.reference name="eventAdmin" interface="org.osgi.service.event.EventAdmin"
 */
public class SiteServiceImpl implements SiteService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SiteServiceImpl.class);

  /**
   * @scr.reference
   */
  private SlingRepository slingRepository;

  /**
   * The default site template, used when none has been defined.
   */
  public static final String DEFAULT_SITE = "/sites/default.html";

  /**
   * The maximum size of any list before we truncate. The user is warned.
   */
  private static final int MAXLISTSIZE = 10000;

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
            && SiteService.SITE_RESOURCE_TYPE.equals(n.getProperty(
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
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
      Session session = slingRepository.loginAdministrative(null);
      String user = site.getSession().getUserID();
      UserManager userManager = AccessControlUtil.getUserManager(session);
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
        startJoinWorkflow(site, targetGroup);
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
      if (!isSite(site)) {
        throw new SiteException(HttpServletResponse.SC_BAD_REQUEST, site.getPath()
            + " is not a site");
      }
      UserManager userManager = AccessControlUtil.getUserManager(site.getSession());
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
            + " was not a member of " + requestedGroup);
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
  public void startJoinWorkflow(Node site, Group targetGroup) throws SiteException {
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
   * @return true if the group is a member of this site
   */
  public boolean isMember(Node site, Authorizable targetGroup) {
    /*
     * What groups are associated with this site, and is the target group a member of one
     * of those groups.
     */
    // low cost check
    try {
      UserManager userManager = AccessControlUtil.getUserManager(site.getSession());
      if (site.hasProperty(SiteService.AUTHORIZABLE)) {
        Value[] values = getPropertyValues(site, SiteService.AUTHORIZABLE);
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

  private Value[] getPropertyValues(Node site, String propName) throws PathNotFoundException, RepositoryException {
    Property property = site.getProperty(propName);
    if (property.getDefinition().isMultiple()) {
      return property.getValues();
    } else {
      return new Value[] { property.getValue() };
    }
  }

  /**
   * @param site
   * @return true if the site is joinable
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
   * @return true if the authz group is joinable
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

  protected void bindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  protected void unbindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.site.SiteService#getSiteTemplate(javax.jcr.Node)
   */
  public String getSiteTemplate(Node site) {
    try {
      if (site.hasProperty(SiteService.SAKAI_SITE_TEMPLATE)) {
        return site.getProperty(SiteService.SAKAI_SITE_TEMPLATE).getString();
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return DEFAULT_SITE;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.site.SiteService#getDefaultSiteTemplate(javax.jcr.Node)
   */
  public String getDefaultSiteTemplate(Node site) {
    // we should probably test that this node exists, but since this is a hard config,
    // then its probably not worth doing it.
    return DEFAULT_SITE;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.site.SiteService#getGroups(javax.jcr.Node, int, int,
   *      org.sakaiproject.kernel.api.site.Sort[])
   */
  public Iterator<Group> getGroups(Node site, int start, int nitems, Sort[] sort)
      throws SiteException {
    MembershipTree membership = getMembershipTree(site);
    if (sort != null && sort.length > 0) {
      Comparator<GroupKey> comparitor = buildCompoundComparitor(sort);
      List<GroupKey> sortedList = Lists.sortedCopy(membership.getGroups().keySet(),
          comparitor);
      Iterator<GroupKey> sortedIterator = sortedList.listIterator(start);
      return unwrapGroups(Iterators.limit(sortedIterator, nitems));
    }

    // no sort requested.

    List<GroupKey> finalList = Lists.immutableList(membership.getGroups().keySet());
    Iterator<GroupKey> unsortedIterator = finalList.listIterator(start);
    return unwrapGroups(Iterators.limit(unsortedIterator, nitems));
  }
  
  public Iterator<Group> unwrapGroups(final Iterator<GroupKey> underlying)
  {
    return new Iterator<Group>() {

      public boolean hasNext() {
        return underlying.hasNext();
      }

      public Group next() {
        return underlying.next().getGroup();
      }

      public void remove() {
        underlying.remove();
      }
    };
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.site.SiteService#getMembers(javax.jcr.Node, int,
   *      int, org.sakaiproject.kernel.api.site.Sort[])
   */
  public Iterator<User> getMembers(Node site, int start, int nitems, Sort[] sort) {
    MembershipTree membership = getMembershipTree(site);
    if (sort != null && sort.length > 0) {
      Comparator<UserKey> comparitor = buildCompoundComparitor(sort);
      List<UserKey> sortedList = Lists
          .sortedCopy(membership.getUsers().keySet(), comparitor);
      Iterator<UserKey> sortedIterator = sortedList.listIterator(start);
      return unwrapUsers(Iterators.limit(sortedIterator, nitems));
    }

    // no sort requested.
    List<UserKey> finalList = Lists.immutableList(membership.getUsers().keySet());
    Iterator<UserKey> unsortedIterator = finalList.listIterator(start);
    return unwrapUsers(Iterators.limit(unsortedIterator, nitems));
  }

  public Iterator<User> unwrapUsers(final Iterator<UserKey> underlying)
  {
    return new Iterator<User>() {

      public boolean hasNext() {
        return underlying.hasNext();
      }

      public User next() {
        return underlying.next().getUser();
      }

      public void remove() {
        underlying.remove();
      }
    };
  }

  public int getMemberCount(Node site) {
    return getMembershipTree(site).getUsers().size();
  }

  /**
   * Builds a membership tree of users and groups for the site, because of the tree like
   * nature of membership we may want to think of a more efficient way of performing this
   * operation, however, there are no queries to perform this and it may be better to have
   * this in memory. We are taking a google like approach to this operation, limiting the
   * set to a practical size. If this become a real problem, then we might want to put the
   * members of a site into a JPA mapped table, although that will require a tree cache
   * invalidation scheme to keep upto date. Membership is derived not always declared.
   * 
   * 
   * @param site
   *          the site
   * @return a membership tree
   */
  private MembershipTree getMembershipTree(Node site) {
    Map<GroupKey, Membership> groups = Maps.newLinkedHashMap();
    Map<UserKey, Membership> users = Maps.newLinkedHashMap();
    try {
      UserManager userManager = AccessControlUtil.getUserManager(site.getSession());
      if (site.hasProperty(SiteService.AUTHORIZABLE)) {
        Value[] values = getPropertyValues(site, SiteService.AUTHORIZABLE);
        for (Value v : values) {
          String groupId = v.getString();
          Authorizable a = userManager.getAuthorizable(groupId);
          if (a instanceof Group) {
            if (!groups.containsKey(a)) {
              groups.put(new GroupKey((Group) a), new Membership(null, a));
              populateMembers((Group) a, groups, users);
            }
          } else if (a instanceof User) {
            if (!users.containsKey(a)) {
              users.put(new UserKey((User) a), new Membership(null, a));
            }
          }
        }

        // might want to cache the unsorted lists at this point, although they are already
        // cached by JCR.

      }
    } catch (RepositoryException ex) {
      LOGGER.warn("Failed to build membership Tree for {} ", site, ex);
    }
    return new MembershipTree(groups, users);
  }

  /**
   * Build a compound set of comparators for performing sorts.
   * 
   * @param sort
   *          the sort array to base the compound set
   * @return the first comparator in the set.
   */
  @SuppressWarnings("unchecked")
  private <T extends AuthorizableKey> Comparator<T> buildCompoundComparitor(Sort[] sort) {
    if (sort.length == 0) {
      return null;
    }
    final Comparator<T>[] comparitors = new Comparator[sort.length];
    int i = 0;
    for (final Sort s : sort) {

      final int next = i + 1;
      comparitors[i++] = new Comparator<T>() {

        /**
         * Compare the objects
         */
        public int compare(T o1, T o2) {
          try {
            Object c1 = o1.getAuthorizable().getID();
            Object c2 = o2.getAuthorizable().getID();
            switch (s.getField()) {
            case firstName:
            case id:
            case lastName:
            }
            switch (s.getOrder()) {
            case asc:
              if (c1 instanceof Comparable) {
                Comparable cc1 = (Comparable<?>) c1;
                int i = cc1.compareTo(c2);
                if (i == 0) {
                  i = compareNext(o1, o2);
                }
                return i;
              }
              return 0;
            case desc:
              if (c2 instanceof Comparable) {
                Comparable cc2 = (Comparable<?>) c2;
                int i = cc2.compareTo(c1);
                if (i == 0) {
                  i = compareNext(o1, o2);
                }
                return i;
              }
              return 0;
            }
          } catch (RepositoryException e) {
          }
          return 0;
        }

        /**
         * Chain to the next comparator in the ordering list.
         * 
         * @param o1
         *          the first object to compare.
         * @param o2
         *          the second object to compare.
         * @return the result of the next comparator in the chain or 0 if this is the last
         *         one.
         */
        private int compareNext(T o1, T o2) {
          if (next < comparitors.length) {
            return comparitors[next].compare(o1, o2);
          }
          return 0;
        }
      };
    }
    return comparitors[0];
  }

  /**
   * Recursively build a list of groups for the group avoiding duplicates or infinite
   * recursion.
   * 
   * @param group
   *          the group for which we want to know all members.
   * @param groups
   *          the groups associated with the site.
   * @param users
   *          the users associated with the sites, extracted from groups
   * @throws RepositoryException
   */
  @SuppressWarnings("unchecked")
  private void populateMembers(Group group, Map<GroupKey, Membership> groups,
      Map<UserKey, Membership> users) throws RepositoryException {
    for (Iterator<Authorizable> igm = group.getDeclaredMembers(); igm.hasNext();) {
      Authorizable a = igm.next();
      if (!groups.containsKey(a)) {
        if (a instanceof Group) {
          groups.put(new GroupKey((Group) a), new Membership(group, a));
          populateMembers((Group) a, groups, users);
        } else {
          users.put(new UserKey((User) a), new Membership(group, a));
        }
      }
      if (users.size() > MAXLISTSIZE || groups.size() > MAXLISTSIZE) {
        LOGGER
            .warn("Large site listing, please consider using dynamic membership rather than explicit members groups parent Group {} "
                + group.getID());
        return;
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @throws RepositoryException
   * @throws IllegalStateException
   * @throws ValueFormatException
   * @see org.sakaiproject.kernel.api.site.SiteService#getMembership(org.apache.jackrabbit.api.security.user.User)
   */
  @SuppressWarnings("unchecked")
  public Map<String, List<Group>> getMembership(Session session, String user)
      throws SiteException {
    try {
      Map<String, List<Group>> sites = Maps.newHashMap();
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable a = userManager.getAuthorizable(user);
      if (a instanceof User) {
        User u = (User) a;
        for (Iterator<Group> igroup = u.memberOf(); igroup.hasNext();) {
          Group group = igroup.next();
          if (group.hasProperty(SiteService.SITES)) {
            Value[] siteReferences = group.getProperty(SiteService.SITES);
            for (Value v : siteReferences) {
              List<Group> g = sites.get(v.getString());
              if (g == null) {
                g = Lists.newArrayList();
                sites.put(v.getString(), g);
              }
              g.add(group);
            }
          }
        }
      }
      return sites;
    } catch (RepositoryException e) {
      throw new SiteException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getMessage());
    }
  }

  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }
  
  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

}
