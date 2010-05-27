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
package org.sakaiproject.nakamura.sitetemplate;

import static org.sakaiproject.nakamura.api.sitetemplate.SiteConstants.AUTHORIZABLES_SITE_NODENAME;
import static org.sakaiproject.nakamura.api.sitetemplate.SiteConstants.AUTHORIZABLES_SITE_NODENAME_SINGLE;
import static org.sakaiproject.nakamura.api.sitetemplate.SiteConstants.AUTHORIZABLES_SITE_IS_MAINTAINER;
import static org.sakaiproject.nakamura.api.sitetemplate.SiteConstants.AUTHORIZABLES_SITE_PRINCIPAL_NAME;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.framework.Constants;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.site.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(immediate = true, label = "%siteService.impl.label", description = "%siteService.impl.desc")
@Property(name = Constants.SERVICE_RANKING, intValue = 100)
@Service
public class SiteServiceTemplateImpl extends SiteServiceImpl {

  public static final Logger LOGGER = LoggerFactory
      .getLogger(SiteServiceTemplateImpl.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.sitetemplate.SiteServiceImpl#getGroups(javax.jcr.Node,
   *      int, int, org.sakaiproject.nakamura.api.site.Sort[])
   */
  @Override
  public Iterator<Group> getGroups(Node site, int start, int nitems, Sort[] sort) {
    MembershipTree membership = getMembershipTree(site, true);
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

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.sitetemplate.SiteServiceImpl#getMemberCount(javax.jcr.Node)
   */
  @Override
  public int getMemberCount(Node site) {
    return getMembershipTree(site, false).getUsers().size();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.sitetemplate.SiteServiceImpl#getMembers(javax.jcr.Node,
   *      int, int, org.sakaiproject.nakamura.api.site.Sort[])
   */
  @Override
  public AbstractCollection<User> getMembers(Node site, int start, int nitems, Sort[] sort) {
    // Get all the members
    MembershipTree membership = getMembershipTree(site, true);

    // If sorting is requested, sort the entire list.
    if (sort != null && sort.length > 0) {
      Comparator<UserKey> comparitor = buildCompoundComparitor(sort);
      List<UserKey> sortedList = Lists.sortedCopy(membership.getUsers().keySet(),
          comparitor);
      Iterator<UserKey> sortedIterator = sortedList.listIterator(start);
      return returnCollection(sortedIterator, nitems, sortedList.size());
    }

    // no sort requested.
    List<UserKey> finalList = Lists.immutableList(membership.getUsers().keySet());
    Iterator<UserKey> unsortedIterator = finalList.listIterator(start);
    return returnCollection(unsortedIterator, nitems, finalList.size());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.sitetemplate.SiteServiceImpl#isMember(javax.jcr.Node,
   *      org.apache.jackrabbit.api.security.user.Authorizable)
   */
  @Override
  public boolean isMember(Node site, Authorizable target) {
    try {
      List<Authorizable> authorizables = getSiteAuthorizables(site, false);
      return isMember(target, authorizables);
    } catch (Exception e) {
      LOGGER.warn("Could not check if the authorizable {} is a member of this site.",
          target);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.sitetemplate.SiteServiceImpl#isUserSiteMaintainer(javax.jcr.Node)
   */
  @Override
  public boolean isUserSiteMaintainer(Node site) throws RepositoryException {
    // TODO Maybe just check JCR_ALL or JCR_WRITE permission on the site node?
    Session session = site.getSession();
    UserManager um = AccessControlUtil.getUserManager(session);
    Authorizable target = um.getAuthorizable(session.getUserID());
    List<Authorizable> authorizables = getSiteAuthorizables(site, true);
    return isMember(target, authorizables);
  }

  /**
   * Builds up an entire membership tree of the site.
   * 
   * @param site
   *          The node that represents a site (the top level node)
   * @param lookupProfile
   *          Whether or not we should look up the profile information
   *          (firstname/lastname)
   * @return The entire membership of a site.
   */
  protected MembershipTree getMembershipTree(Node site, boolean lookupProfile) {
    Map<GroupKey, Membership> groups = Maps.newLinkedHashMap();
    Map<UserKey, Membership> users = Maps.newLinkedHashMap();

    try {
      Session session = site.getSession();

      // Get all the authorizables
      List<Authorizable> authorizables = getSiteAuthorizables(site, false);

      // Loop over all the authorizables and expand the groups so that each user can be
      // added.
      for (Authorizable a : authorizables) {
        if (a instanceof Group) {
          GroupKey gKey = new GroupKey((Group) a);
          if (!groups.containsKey(gKey)) {
            // This group has not been added yet.
            groups.put(gKey, new Membership(null, a));
            // Add all the members of this group to the membership tree.
            // This is done recursively, so a subgroup get's added as well.
            populateMembers((Group) a, groups, users, lookupProfile, session);
          }
        } else if (a instanceof User) {
          // Add this individual User to the list.
          UserKey userKey = new UserKey((User) a);
          if (!users.containsKey(userKey)) {
            // Check if we really need to lookup the profile info.
            if (lookupProfile) {
              String profilePath = PersonalUtils.getProfilePath(a);
              try {
                Node profileNode = (Node) session.getItem(profilePath);
                userKey.setProfileNode(profileNode);
              } catch (PathNotFoundException e) {
                LOGGER.warn("User {} does not have a profile at {} ", a.getID(),
                    profilePath);
              }
            }
            users.put(userKey, new Membership(null, a));
          }
        }
      }
    } catch (RepositoryException e) {
      // dont change this warn into {} form, doing so will prevent the exception being
      // displayed.
      LOGGER.warn("Failed to build membership Tree for  site [" + site + "] ", e);
    }

    return new MembershipTree(groups, users);
  }

  /**
   * Retrieves a list of {@link Authorizable authorizables} for a certain site.
   * 
   * @param site
   *          The node that represents the site (top level node)
   * @param justMaintainers
   *          true only retrieves those authorizables that are marked as site maintainers.
   * @return
   * @throws RepositoryException
   */
  protected List<Authorizable> getSiteAuthorizables(Node site, boolean justMaintainers)
      throws RepositoryException {
    Session session = site.getSession();
    UserManager um = AccessControlUtil.getUserManager(session);
    Node authorizableNodes = site.getNode(AUTHORIZABLES_SITE_NODENAME);
    NodeIterator authorizableIt = authorizableNodes
        .getNodes(AUTHORIZABLES_SITE_NODENAME_SINGLE + "*");
    List<Authorizable> authorizables = new ArrayList<Authorizable>();
    while (authorizableIt.hasNext()) {
      // Each one of these nodes will either represent a Group or a User
      Node auNode = authorizableIt.nextNode();
      String auName = auNode.getProperty(AUTHORIZABLES_SITE_PRINCIPAL_NAME).getString();

      // If we only need the maintainers we look at the property on the node to see if
      // this authorizable represents a maintainer.
      if (justMaintainers) {
        if (!auNode.hasProperty(AUTHORIZABLES_SITE_IS_MAINTAINER)) {
          continue;
        }
        if (!auNode.getProperty(AUTHORIZABLES_SITE_IS_MAINTAINER).getBoolean()) {
          continue;
        }
      }

      // Get the authorizable
      Authorizable a = um.getAuthorizable(auName);

      authorizables.add(a);
    }

    return authorizables;
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
   * @param lookupProfile
   *          A boolean that states whether or not the profile should be looked up. If it
   *          is looked up then the latest information regarding firstName, lastName will
   *          be used
   * @param session
   *          the session to grab the profile node for users.
   * @throws RepositoryException
   */
  private void populateMembers(Group group, Map<GroupKey, Membership> groups,
      Map<UserKey, Membership> users, boolean lookupProfile, Session session)
      throws RepositoryException {
    for (Iterator<Authorizable> igm = group.getDeclaredMembers(); igm.hasNext();) {
      Authorizable a = igm.next();
      if (!groups.containsKey(a)) {
        if (a instanceof Group) {
          groups.put(new GroupKey((Group) a), new Membership(group, a));
          populateMembers((Group) a, groups, users, lookupProfile, session);
        } else {
          // This is a user
          addUser(a, group, users, lookupProfile, session);
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
   * Adds a user to the map.
   * 
   * @param user
   *          The user that should be added.
   * @param group
   *          An optional group that the user is part of. This can be null.
   * @param users
   *          The map to add the user to.
   * @param lookupProfile
   *          A boolean that states whether or not the profile should be looked up. If it
   *          is looked up then the latest information regarding firstName, lastName will
   *          be used.
   * @param session
   *          A session to look up the profile.
   * @throws RepositoryException
   */
  protected void addUser(Authorizable user, Group group, Map<UserKey, Membership> users,
      boolean lookupProfile, Session session) throws RepositoryException {
    Node profileNode = null;

    // Check if we really need to look up the profile.
    if (lookupProfile) {
      String profilePath = PersonalUtils.getProfilePath(user);
      try {
        profileNode = (Node) session.getItem(profilePath);
      } catch (PathNotFoundException e) {
        LOGGER.warn("User {} does not have a profile at {} ", user.getID(), profilePath);
      }
    }
    users.put(new UserKey((User) user, profileNode), new Membership(group, user));
  }

  /**
   * Checks if the target authorizable is in the list of authorizables (or member of one
   * of the Group authorizables).
   * 
   * @param target
   *          The authorizable to test.
   * @param authorizables
   *          The List of authorizables to test the target on.
   * @return Will return true if the authorizable is in or member of the list, false
   *         otherwise.
   * @throws RepositoryException
   */
  private boolean isMember(Authorizable target, List<Authorizable> authorizables)
      throws RepositoryException {
    // We loop over the list of authorizables twice.
    // Once to check if the target is in the list.
    // If it hasn't been found by then, we loop over it again and check if the target is
    // a member of one the group authorizables.
    for (Authorizable a : authorizables) {
      // Authorizables implement the necessary equals method to check for equality.
      if (a.equals(target)) {
        return true;
      }
    }

    // The authorizable hasn't been found yet, we check each group authorizable for
    // membership of the target.
    // We don't have to worry about subgroups as these will be checked for us.
    // This check is obviously more expensive.
    for (Authorizable a : authorizables) {
      if (a instanceof Group) {
        if (((Group) a).isMember(target)) {
          return true;
        }
      }
    }
    return false;
  }
}
