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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.api.user.UserConstants.Joinable;
import org.sakaiproject.nakamura.user.lite.resource.LiteAuthorizableResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



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
  protected void updateGroupMembership(SlingHttpServletRequest request,
      Authorizable authorizable, List<Modification> changes, Set<Object> toSave) throws AccessDeniedException, StorageClientException  {
    updateGroupMembership(request, authorizable,
        SlingPostConstants.RP_PREFIX + "member", changes, toSave);
  }

  protected void updateGroupMembership(SlingHttpServletRequest request,
      Authorizable authorizable, String paramName, List<Modification> changes, Set<Object> toSave) throws AccessDeniedException, StorageClientException  {
    if (authorizable instanceof Group) {
      Group group = ((Group) authorizable);
      String groupPath = LiteAuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
          + group.getId();

      ResourceResolver resolver = request.getResourceResolver();
      boolean changed = false;
      
      Session session = StorageClientUtils.adaptToSession(resolver.adaptTo(javax.jcr.Session.class));
      AuthorizableManager authorizableManager = session.getAuthorizableManager();

      // first remove any members posted as ":member@Delete"
      String[] membersToDelete = request.getParameterValues(paramName + SlingPostConstants.SUFFIX_DELETE);
      if (membersToDelete != null) {
        for (String member : membersToDelete) {
          group.removeMember(member);
          changed = true;
        }
      }
      
      if ( changed ) {
        // we must update since we annother session may need this object.
        authorizableManager.updateAuthorizable(group);
      }
      Joinable groupJoin = getJoinable(group);

      // second add any members posted as ":member"
      String[] membersToAdd = request.getParameterValues(paramName);
      if (membersToAdd != null) {
        Group peerGroup = getPeerGroupOf(group, authorizableManager);
        List<Authorizable> membersToRemoveFromPeer = new ArrayList<Authorizable>();
        for (String member : membersToAdd) {
          Authorizable memberAuthorizable = authorizableManager.findAuthorizable(member);
          if (memberAuthorizable != null) {
            if(!UserConstants.ANON_USERID.equals(resolver.getUserID())
                && Joinable.yes.equals(groupJoin)
                && memberAuthorizable.getId().equals(resolver.getUserID())){
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
              //group is restricted, so use the current user's authorization
              //to add the member to the group:
              group.addMember(memberAuthorizable.getId());
              toSave.add(group);
              changed = true;
            }
            if (peerGroup != null) {
              Set<String> members = ImmutableSet.of(peerGroup.getMembers());
              if (members.contains(memberAuthorizable.getId())) {
                membersToRemoveFromPeer.add(memberAuthorizable);
              }
            }
          }
        }
        if (peerGroup != null) {
          for (Authorizable member : membersToRemoveFromPeer) {
            peerGroup.removeMember(member.getId());
          }
          toSave.add(peerGroup);
        }
      }

      if (changed) {
        // add an entry to the changes list to record the membership
        // change
        changes.add(Modification.onModified(groupPath + "/members"));
      }
    }
  }

  private Group getPeerGroupOf(Group group, AuthorizableManager authorizableManager) throws AccessDeniedException, StorageClientException  {
    Group peerGroup = null;
    if (group.hasProperty(UserConstants.PROP_MANAGERS_GROUP)) {
      String managersGroupId = StorageClientUtils.toString(group.getProperty(UserConstants.PROP_MANAGERS_GROUP));
      peerGroup = (Group) authorizableManager.findAuthorizable(managersGroupId);
    } else {
      if (group.hasProperty(UserConstants.PROP_MANAGED_GROUP)) {
        String managedGroupId = StorageClientUtils.toString(group.getProperty(UserConstants.PROP_MANAGED_GROUP));
        peerGroup = (Group) authorizableManager.findAuthorizable(managedGroupId);
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
      String[] managers, List<Modification> changes, Set<Object> toSave) {

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
   * @param propAuthorizables
   *          The name of the property on the group where the authorizable IDs should be
   *          added/deleted.
   * @param paramName
   *          The name of the parameter that contains the authorizable IDs. ie: :manager
   *          or :viewer. If :manager is specified, :manager@Delete will be used for
   *          deletes.
   * @param extraPrincipalsToAdd
   *          An array of authorizable IDs that should be added as well.
   * @param toSave 
   * @throws RepositoryException
   */
  protected void handleAuthorizablesOnProperty(SlingHttpServletRequest request,
      Group group, String propAuthorizables, String paramName,
      String[] extraPrincipalsToAdd, Set<Object> toSave)  {
    Set<String> principals = new HashSet<String>();
    if (group.hasProperty(propAuthorizables)) {
      String[] existingPrincipals = StorageClientUtils.toStringArray(group.getProperty(propAuthorizables));
      for (String principal : existingPrincipals) {
        principals.add(principal);
      }
    }

    boolean changed = false;

    // Remove all the managers that are in the :manager@Delete request parameter.
    String[] principalsToDelete = request.getParameterValues(paramName
        + SlingPostConstants.SUFFIX_DELETE);
    if (principalsToDelete != null) {
      for (String principal : principalsToDelete) {
        principals.remove(principal);
        changed = true;
      }
    }

    // Add the new ones (if any)
    String[] principalsToAdd = request.getParameterValues(paramName);
    if (principalsToAdd != null) {
      for (String principal : principalsToAdd) {
        principals.add(principal);
        changed = true;
      }
    }

    // Add the extra ones (if any.)
    if (extraPrincipalsToAdd != null) {
      for (String principal : extraPrincipalsToAdd) {
        principals.add(principal);
        changed = true;
      }
    }

    // Write the property.
    if (changed) {
      group.setProperty(propAuthorizables, StorageClientUtils.toStore(principals));
      toSave.add(group);
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
          String joinable = StorageClientUtils.toString(authorizable.getProperty(UserConstants.PROP_JOINABLE_GROUP));
          if (joinable != null)
            return Joinable.valueOf(joinable);
        } catch (IllegalArgumentException e) {
        }
      }
    return Joinable.no;
  }

}
