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
package org.sakaiproject.nakamura.user.lite.servlet;

import com.google.common.collect.ImmutableSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.api.user.UserConstants.Joinable;
import org.sakaiproject.nakamura.user.lite.resource.LiteAuthorizableResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;



/**
 * Base class for servlets manipulating groups
 */
@Component(immediate=true, metatype=true,componentAbstract=true)
public abstract class LiteAbstractSakaiGroupPostServlet extends
    LiteAbstractAuthorizablePostServlet {
  private static final long serialVersionUID = 1159063041816944076L;

  /**
   * The JCR Repository we access to resolve resources
   *
   */
  @Reference
  protected transient Repository repository;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(LiteAbstractSakaiGroupPostServlet.class);

  /**
   * Update the group membership based on the ":member" request parameters. If the
   * ":member" value ends with @Delete it is removed from the group membership, otherwise
   * it is added to the group membership.
   *
   * @param request
   * @param authorizable
   * @param toSave
   * @throws StorageClientException
   * @throws AccessDeniedException
   * @throws RepositoryException
   */
  protected void updateGroupMembership(SlingHttpServletRequest request, Session session,
      Authorizable authorizable, List<Modification> changes, Map<String, Object> toSave) throws AccessDeniedException, StorageClientException  {
    updateGroupMembership(request, session, authorizable,
        SlingPostConstants.RP_PREFIX + "member", changes, toSave);
  }

  protected void updateGroupMembership(SlingHttpServletRequest request, Session session,
      Authorizable authorizable, String paramName, List<Modification> changes, Map<String, Object> toSave) throws AccessDeniedException, StorageClientException  {
    if (authorizable instanceof Group) {
      Group group = ((Group) authorizable);
      String groupPath = LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
          + group.getId();

      boolean changed = false;

      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      Joinable groupJoin = getJoinable(group);

      // first remove any members posted as ":member@Delete"
      String[] membersToDelete = request.getParameterValues(paramName + SlingPostConstants.SUFFIX_DELETE);
      if (membersToDelete != null) {
        toSave.put(group.getId(), group);
        LOGGER.info("Members to delete {} ",membersToDelete);
        for (String member : membersToDelete) {
          String memberId = getAuthIdFromParameter(member);
          group.removeMember(memberId);
          if(!User.ADMIN_USER.equals(session.getUserId()) && !User.ANON_USER.equals(session.getUserId())
              && Joinable.yes.equals(groupJoin)
              && memberId.equals(session.getUserId())) {
            Session adminSession = getSession();
            try{
              AuthorizableManager adminAuthorizableManager = adminSession.getAuthorizableManager();
              adminAuthorizableManager.updateAuthorizable(group);
            }finally{
                ungetSession(adminSession);
            }
          }
          changed = true;
        }
      }

      // second add any members posted as ":member"
      String[] membersToAdd = request.getParameterValues(paramName);
      if (membersToAdd != null) {
        LOGGER.info("Members to add {} ",membersToAdd);
        Group peerGroup = getPeerGroupOf(group, authorizableManager, toSave);
        List<Authorizable> membersToRemoveFromPeer = new ArrayList<Authorizable>();
        for (String member : membersToAdd) {
          String memberId = getAuthIdFromParameter(member);
          Authorizable memberAuthorizable = (Authorizable) toSave.get(memberId);
          if (memberAuthorizable == null ) {
            memberAuthorizable = authorizableManager.findAuthorizable(memberId);
          }
          if (memberAuthorizable != null) {
            if(!User.ADMIN_USER.equals(session.getUserId()) && !UserConstants.ANON_USERID.equals(session.getUserId())
                && Joinable.yes.equals(groupJoin)
                && memberAuthorizable.getId().equals(session.getUserId())){
              LOGGER.debug("Is Joinable {} {} ",groupJoin,session.getUserId());
              //we can grab admin session since group allows all users to join
              Session adminSession = getSession();
              try{
                AuthorizableManager adminAuthorizableManager = adminSession.getAuthorizableManager();
                Group adminAuthGroup = (Group) adminAuthorizableManager.findAuthorizable(group.getId());
                if(adminAuthGroup != null){
                  adminAuthGroup.addMember(memberAuthorizable.getId());
                  adminAuthorizableManager.updateAuthorizable(adminAuthGroup);
                  changed = true;
                }
              }finally{
                  ungetSession(adminSession);
              }
            }else{
              LOGGER.info("Group {} is not Joinable: User {} adding {}  ",new Object[]{group.getId(), session.getUserId(), memberAuthorizable.getId(),});
              //group is restricted, so use the current user's authorization
              //to add the member to the group:

              group.addMember(memberAuthorizable.getId());
              if ( LOGGER.isInfoEnabled() ) {
                LOGGER.info("{} Membership now {} {} {}", new Object[]{ group.getId(),Arrays.toString(group.getMembers()), Arrays.toString(group.getMembersAdded()), Arrays.toString(group.getMembersRemoved())});
              }
              toSave.put(group.getId(), group);
              Group gt = (Group) toSave.get(group.getId());
              if ( LOGGER.isInfoEnabled() ) {
                LOGGER.info("{} Membership now {} {} {}", new Object[]{ group.getId(),Arrays.toString(gt.getMembers()), Arrays.toString(gt.getMembersAdded()), Arrays.toString(gt.getMembersRemoved())});
              }
              changed = true;
            }
            if (peerGroup != null && peerGroup.getId() != group.getId()) {
              Set<String> members = ImmutableSet.of(peerGroup.getMembers());
              if (members.contains(memberAuthorizable.getId())) {
                membersToRemoveFromPeer.add(memberAuthorizable);
              }
            }
          } else {
            LOGGER.warn("member not found {} ", memberId);
          }
        }
        if ((peerGroup != null) && (membersToRemoveFromPeer.size() > 0)) {
          for (Authorizable member : membersToRemoveFromPeer) {
            if ( LOGGER.isInfoEnabled() ) {
              LOGGER.info("Removing Member {} from {} ",member.getId(), peerGroup.getId());
            }
            peerGroup.removeMember(member.getId());
          }
          toSave.put(peerGroup.getId(), peerGroup);
          if ( LOGGER.isInfoEnabled() ) {
            LOGGER.info("{} Just Updated Peer Group Membership now {} {} {}", new Object[]{peerGroup.getId(), Arrays.toString(peerGroup.getMembers()), Arrays.toString(peerGroup.getMembersAdded()), Arrays.toString(peerGroup.getMembersRemoved())});
          }
        }

      }

      if (changed) {
        // add an entry to the changes list to record the membership
        // change
        changes.add(Modification.onModified(groupPath + "/members"));
      }
    }
  }

  private String getAuthIdFromParameter(String member) {
    //we might be sent a parameter that looks like a full path
    //we only want the id at the end
    return member.substring(member.lastIndexOf("/") + 1);
  }

  private Group getPeerGroupOf(Group group, AuthorizableManager authorizableManager, Map<String, Object> toSave) throws AccessDeniedException, StorageClientException  {
    Group peerGroup = null;
    if (group.hasProperty(UserConstants.PROP_MANAGERS_GROUP)) {
      String managersGroupId = (String) group.getProperty(UserConstants.PROP_MANAGERS_GROUP);
      if ( group.getId().equals(managersGroupId)) {
        return group;
      }
      peerGroup = (Group) toSave.get(managersGroupId);
      if ( peerGroup == null ) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("For {} Not in toSave List loading Managers Group from store {} ",group.getId(),managersGroupId);
        }
        peerGroup = (Group) authorizableManager.findAuthorizable(managersGroupId);
      } else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("For {} got Managers Group from save list {} ",group.getId(),managersGroupId);
        }
      }
    } else if (group.hasProperty(UserConstants.PROP_MANAGED_GROUP)) {
      String managedGroupId = (String) group.getProperty(UserConstants.PROP_MANAGED_GROUP);
      if ( group.getId().equals(managedGroupId)) {
        return group;
      }
      peerGroup = (Group) toSave.get(managedGroupId);
      if ( peerGroup == null ) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("For {} Not in toSave List loading Managed Group from store {} ",group.getId(),managedGroupId);
        }
        peerGroup = (Group) authorizableManager.findAuthorizable(managedGroupId);
      } else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("For {} got Managed Group from save list {} ",group.getId(),managedGroupId);
        }
      }
    }
    return peerGroup;
  }



  /**
   * @param request
   *          the request
   * @param group
   *          the group
   * @param managers
   *          a list of principals who are allowed to admin the group.
   * @param changes
   *          changes made
   * @param toSave
   * @throws RepositoryException
   */
  protected void updateOwnership(SlingHttpServletRequest request, Group group,
      String[] managers, List<Modification> changes, Map<String, Object> toSave) {

    handleAuthorizablesOnProperty(request, group, UserConstants.PROP_GROUP_MANAGERS,
        SlingPostConstants.RP_PREFIX + "manager", managers, toSave);
    handleAuthorizablesOnProperty(request, group, UserConstants.PROP_GROUP_VIEWERS,
        SlingPostConstants.RP_PREFIX + "viewer", null, toSave);

  }

  /**
   * @param request
   *          The request that contains the authorizables.
   * @param group
   *          The group that should be modified.
   * @param propertyName
   *          The name of the property on the group where the authorizable IDs should be
   *          added/deleted.
   * @param paramName
   *          The name of the parameter that contains the authorizable IDs. ie: :manager
   *          or :viewer. If :manager is specified, :manager@Delete will be used for
   *          deletes.
   * @param extraPropertyValues
   *          An array of authorizable IDs that should be added as well.
   * @param toSave
   * @throws RepositoryException
   */
  protected void handleAuthorizablesOnProperty(SlingHttpServletRequest request,
      Group group, String propertyName, String paramName,
      String[] extraPropertyValues, Map<String, Object> toSave)  {
    Set<String> propertyValueSet = new HashSet<String>();

    if (group.hasProperty(propertyName)) {
      Object o = group.getProperty(propertyName);

      String[] existingProperties;
      if (o instanceof String) {
        existingProperties = new String[] { (String) o };
      } else {
        existingProperties = (String[]) o;
      }

      for (String property : existingProperties) {
        propertyValueSet.add(property);
      }
    }

    boolean changed = false;

    // Remove all the managers that are in the :manager@Delete request parameter.
    String[] propertiesToDelete = request.getParameterValues(paramName
        + SlingPostConstants.SUFFIX_DELETE);
    if (propertiesToDelete != null) {
      for (String propertyToDelete : propertiesToDelete) {
        propertyValueSet.remove(propertyToDelete);
        changed = true;
      }
    }

    // Add the new ones (if any)
    String[] proeprtiesToAdd = request.getParameterValues(paramName);
    if (proeprtiesToAdd != null) {
      for (String propertyToAdd : proeprtiesToAdd) {
        propertyValueSet.add(propertyToAdd);
        changed = true;
      }
    }

    // Add the extra ones (if any.)
    if (extraPropertyValues != null) {
      for (String propertyValue : extraPropertyValues) {
        propertyValueSet.add(propertyValue);
        changed = true;
      }
    }

    // Write the property.
    if (changed) {
      group.setProperty(propertyName, propertyValueSet.toArray(new String[propertyValueSet.size()]));
      if ( LOGGER.isDebugEnabled() ) {
        LOGGER.debug("Adding to save Queue {} {}",group.getId(),group.getSafeProperties());
      }
      toSave.put(group.getId(), group);
    }
  }

  /** Returns the JCR repository used by this service. */
  protected Repository getRepository() {
    return repository;
  }

  /**
   * Returns an administrative session to the default workspace.
   * @throws AccessDeniedException
   * @throws StorageClientException
   * @throws ClientPoolException
   */
  private Session getSession() throws ClientPoolException, StorageClientException, AccessDeniedException  {
    return getRepository().loginAdministrative();
  }

  /**
   * Return the administrative session and close it.
   */
  private void ungetSession(final Session session) {
    if (session != null) {
      try {
        session.logout();
      } catch (Throwable t) {
        LOGGER.error("Unable to log out of session: " + t.getMessage(), t);
      }
    }
  }

  /**
   * @return true if the authz group is joinable
   * @throws RepositoryException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
  public Joinable getJoinable(Authorizable authorizable) {
      if (authorizable instanceof Group && authorizable.hasProperty(UserConstants.PROP_JOINABLE_GROUP)) {
        try {
          String joinable = (String) authorizable.getProperty(UserConstants.PROP_JOINABLE_GROUP);
          LOGGER.info("Joinable Property on {} {} ", authorizable, joinable);
          if (joinable != null) {
            return Joinable.valueOf(joinable);
          }
        } catch (IllegalArgumentException e) {
          LOGGER.info(e.getMessage(),e);
        }
      } else {
        LOGGER.info("No Joinable Property on {} ", authorizable);
      }
    return Joinable.no;
  }

}
