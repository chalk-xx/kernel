/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.user.servlet;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jackrabbit.usermanager.impl.post.AbstractAuthorizablePostServlet;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * Base class for servlets manipulating groups
 */
public abstract class AbstractSakaiGroupPostServlet extends
        AbstractAuthorizablePostServlet {
    private static final long serialVersionUID = 1159063041816944076L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSakaiGroupPostServlet.class);

    /**
     * Update the group membership based on the ":member" request parameters. If
     * the ":member" value ends with @Delete it is removed from the group
     * membership, otherwise it is added to the group membership.
     * @param session 
     * 
     * @param request
     * @param authorizable
     * @throws RepositoryException
     */
    protected void updateGroupMembership(Session session, SlingHttpServletRequest request,
            Authorizable authorizable, List<Modification> changes)
            throws RepositoryException {
        if (authorizable.isGroup()) {
            Group group = ((Group) authorizable);
            String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
                + group.getID();

            ResourceResolver resolver = request.getResourceResolver();
            Resource baseResource = request.getResource();
            boolean changed = false;
            
            UserManager userManager = AccessControlUtil.getUserManager(session);
            
            int addIndex = 0;
            int removeIndex = 0;
            List<Modification> userChanges = new ArrayList<Modification>();

            String[] membersToDelete = request.getParameterValues(SlingPostConstants.RP_PREFIX
                + "member" + SlingPostConstants.SUFFIX_DELETE);
            String[] membersToAdd = request.getParameterValues(SlingPostConstants.RP_PREFIX
                + "member");
            try {
              // first remove any members posted as ":member@Delete"
              if (membersToDelete != null) {
                  for (removeIndex=0; removeIndex<membersToDelete.length; removeIndex++) {
                      if (removeMember(group, membersToDelete[removeIndex], baseResource, userManager, resolver)) {
                        String[] usernameParts = membersToDelete[removeIndex].split("\\/");
                        Authorizable au = userManager.getAuthorizable(usernameParts[usernameParts.length - 1]);
                        Modification mod;
                        if (au.isGroup()) {
                          mod = GroupModification.onGroupLeave(groupPath + "/members", group, (Group)au);
                        }
                        else {
                          mod = UserModification.onGroupLeave(groupPath + "/members", group, (User)au);
                        }
                        userChanges.add(mod);
                        changed = true;
                      }
                  }
              }

              // second add any members posted as ":member"
              if (membersToAdd != null) {
                for (addIndex=0; addIndex<membersToAdd.length; addIndex++) {
                    if (addMember(group, membersToAdd[addIndex], baseResource, userManager, resolver)) {
                      String[] usernameParts = membersToAdd[addIndex].split("\\/");
                      Authorizable au = userManager.getAuthorizable(usernameParts[usernameParts.length - 1]);
                      Modification mod;
                      if (au.isGroup()) {
                        mod = GroupModification.onGroupLeave(groupPath + "/members", group, (Group)au);
                      }
                      else {
                        mod = UserModification.onGroupLeave(groupPath + "/members", group, (User)au);
                      }
                      userChanges.add(mod);
                      changed = true;
                    }
                }
              }

              changes.addAll(userChanges);

            } catch (RepositoryException re) {
              LOGGER.debug("Group membership modification failed, rolling back");
              for (int unaddIndex = addIndex - 1; unaddIndex >= 0; unaddIndex--) {
                removeMember(group, membersToAdd[unaddIndex], baseResource, userManager, resolver);
              }
              for (int unremoveIndex = removeIndex - 1; unremoveIndex >= 0; unremoveIndex--) {
                addMember(group, membersToDelete[unremoveIndex], baseResource, userManager, resolver);
              }
              throw re;
            }

            if (changed) {
                // add an entry to the changes list to record the membership
                // change
                changes.add(Modification.onModified(groupPath + "/members"));
            }
        }
    }

    private boolean removeMember(Group group, String member, Resource baseResource,
        UserManager userManager, ResourceResolver resolver) throws RepositoryException {
      Authorizable memberAuthorizable = getAuthorizable(baseResource, member,userManager,resolver);
      if (memberAuthorizable != null) {
          if (!group.removeMember(memberAuthorizable)) {
            throw new RepositoryException("Unable to remove user " + member + " from group " + group.getID());
          }
          return true;
      }
      return false;
    }

    private boolean addMember(Group group, String member, Resource baseResource,
        UserManager userManager, ResourceResolver resolver) throws RepositoryException {
      Authorizable memberAuthorizable = getAuthorizable(baseResource, member,userManager,resolver);
      if (memberAuthorizable != null) {
          if (!group.addMember(memberAuthorizable)) {
            throw new RepositoryException("Unable to add user " + member + " to group " + group.getID());
          }
          return true;
      }
      return false;
    }

    /**
     * Gets the member, assuming its a principal name, failing that it assumes it a path to the resource.
     * @param member the token pointing to the member, either a name or a uri
     * @param userManager the user manager for this request.
     * @param resolver the resource resolver for this request.
     * @return the authorizable, or null if no authorizable was found.
     */
    private Authorizable getAuthorizable(Resource baseResource, String member, UserManager userManager,
        ResourceResolver resolver) {
      Authorizable memberAuthorizable = null;
      try {
        String memberId = PathUtils.lastElement(member);
        memberAuthorizable = userManager.getAuthorizable(memberId);
      } catch (RepositoryException e) {
        // if we can't find the members then it may be resolvable as a resource.
        LOGGER.warn("Failed to find member "+member,e);
      }
      return memberAuthorizable;
    }

    /**
     * @param session the session used to create the group
     * @param request the request
     * @param group the group
     * @param principalChange a list of principals who are allowed to admin the group.
     * @param changes changes made
     * @throws RepositoryException 
     */
    protected void updateOwnership(Session session, SlingHttpServletRequest request,
        Group group, String[] principalChange, List<Modification> changes) throws RepositoryException {
      Set<String> adminPrincipals = new HashSet<String>();
      if (group.hasProperty(UserConstants.ADMIN_PRINCIPALS_PROPERTY)) {
        Value[] adminPrincipalsProperties = group
            .getProperty(UserConstants.ADMIN_PRINCIPALS_PROPERTY);
        for (Value adminPricipal : adminPrincipalsProperties) {
          adminPrincipals.add(adminPricipal.toString());
        }
      }
      boolean changed = false;
      for (String principal : principalChange) {
        if (principal.startsWith("delete@")) {
          String principalToDelete = principal.substring("delete@".length());
          if (adminPrincipals.contains(principalToDelete)) {
            adminPrincipals.remove(principalToDelete);
            changed = true;
          }

        } else {

          if (!adminPrincipals.contains(principal)) {
            adminPrincipals.add(principal);
            changed = true;
          }
        }
      }
      if (changed) {
        ValueFactory valueFactory = session.getValueFactory();
        Value[] newAdminPrincipals = new Value[adminPrincipals.size()];
        int i = 0;
        for (String adminPrincipalName : adminPrincipals) {
          newAdminPrincipals[i++] = valueFactory.createValue(adminPrincipalName);
        }
        group.setProperty(UserConstants.ADMIN_PRINCIPALS_PROPERTY,
            newAdminPrincipals);
      }
    }

}
