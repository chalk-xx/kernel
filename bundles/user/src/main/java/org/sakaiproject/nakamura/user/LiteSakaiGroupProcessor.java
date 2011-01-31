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
package org.sakaiproject.nakamura.user;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_GROUP_MANAGERS;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_MANAGED_GROUP;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_MANAGERS_GROUP;

import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.user.LiteAuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class handles whatever processing is needed before the Jackrabbit Group modification
 * can be sent to other AuthorizablePostProcessor services.
 */
public class LiteSakaiGroupProcessor implements
    LiteAuthorizablePostProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(LiteSakaiGroupProcessor.class);
  public static final String PARAM_ADD_TO_MANAGERS_GROUP = SlingPostConstants.RP_PREFIX + "sakai:manager";
  public static final String PARAM_REMOVE_FROM_MANAGERS_GROUP = PARAM_ADD_TO_MANAGERS_GROUP + SlingPostConstants.SUFFIX_DELETE;

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.servlets.post.Modification, java.util.Map)
   */
  public void process(Authorizable authorizable, Session session, Modification change,
      Map<String, Object[]> parameters) throws Exception {
    if (authorizable instanceof Group) {
      Group group = (Group) authorizable;
      if (change.getType() == ModificationType.DELETE) {
        deleteManagersGroup(group, session);
      } else {
        if (change.getType() == ModificationType.CREATE) {
          createManagersGroup(group, session);
        }
        updateManagersGroupMembership(group, session, parameters);
        session.getAuthorizableManager().updateAuthorizable(group);
      }
    }
  }

  /**
   * Generate a private self-managed Jackrabbit Group to hold Sakai group
   * members with the Manager role. Such members have all access
   * rights over the Sakai group itself and may be given special access
   * rights to content.
   * @throws StorageClientException 
   * @throws AccessDeniedException 
   */
  private void createManagersGroup(Group group, Session session) throws AccessDeniedException, StorageClientException {
    AuthorizableManager userManager = session.getAuthorizableManager();
    String managersGroupId = makeUniqueAuthorizableId(group.getId() + "-managers", userManager);
    
    Builder<String, Object> propertyBuilder = ImmutableMap.builder();

    // Create the public self-managed managers group.
    propertyBuilder.put(PROP_GROUP_MANAGERS, StorageClientUtils.toStore(new String[] {managersGroupId}));

    // Add the managers group to its Sakai group.
    group.addMember(managersGroupId);

    // Have the managers group manage the Sakai group.
    addStringToValues(group, PROP_GROUP_MANAGERS, managersGroupId);

    // Set the association between the two groups.
    group.setProperty(PROP_MANAGERS_GROUP, StorageClientUtils.toStore(managersGroupId));
    propertyBuilder.put(PROP_MANAGED_GROUP, StorageClientUtils.toStore(group.getId()));
    userManager.createGroup(managersGroupId, managersGroupId, propertyBuilder.build());
  }

  private void addStringToValues(Authorizable authorizable, String propertyName, String newString) {
    List<String> newValues = new ArrayList<String>();
    if (authorizable.hasProperty(propertyName)) {
      String[] oldValues = StorageClientUtils.toStringArray(authorizable.getProperty(propertyName));
      for (String oldValue : oldValues) {
        if (newString.equals(oldValue)) {
          return;
        } else {
          newValues.add(oldValue);
        }
      }
    }
    newValues.add(newString);
    authorizable.setProperty(propertyName, StorageClientUtils.toStore(newValues.toArray(new String[newValues.size()])));
  }

  /**
   * @param group
   * @param session
   * @return the group that holds the group's Manager members, or null if there is no
   *         managers group or it is inaccessible
   * @throws RepositoryException
   * @throws StorageClientException 
   * @throws AccessDeniedException 
   */
  private Group getManagersGroup(Group group, AuthorizableManager userManager) throws AccessDeniedException, StorageClientException {
    Group managersGroup = null;
    if (group.hasProperty(UserConstants.PROP_MANAGERS_GROUP)) {
      String values[] = StorageClientUtils.toStringArray(group.getProperty(UserConstants.PROP_MANAGERS_GROUP));
      String managersGroupId = values[0];
      managersGroup = (Group) userManager.findAuthorizable(managersGroupId);
    }
    return managersGroup;
  }

  /**
   * Inspired by Sling's AbstractCreateOperation ensureUniquePath.
   * @param startId
   * @param userManager
   * @return
   * @throws RepositoryException
   * @throws StorageClientException 
   * @throws AccessDeniedException 
   */
  private String makeUniqueAuthorizableId(String startId, AuthorizableManager userManager) throws AccessDeniedException, StorageClientException {
    String newAuthorizableId = startId;
    int idx = 0;
    while (userManager.findAuthorizable(newAuthorizableId) != null) {
      if (idx > 100) {
        throw new StorageClientException("Too much contention on authorizable ID " + startId);
      } else {
        newAuthorizableId = startId + "_" + idx++;
      }
    }
    return newAuthorizableId;
  }

  private void updateManagersGroupMembership(Group group, Session session, Map<String, Object[]> parameters) throws StorageClientException, AccessDeniedException {
    AuthorizableManager userManager = session.getAuthorizableManager();
    Group managersGroup = getManagersGroup(group, userManager);
    if (managersGroup != null) {
      boolean needsUpdate = false;
      Object[] addValues = parameters.get(PARAM_ADD_TO_MANAGERS_GROUP);
      if ((addValues != null) && (addValues instanceof String[])) {
        for (String memberId : (String [])addValues) {
          Authorizable authorizable = userManager.findAuthorizable(memberId);
          if (authorizable != null) {
            managersGroup.addMember(authorizable.getId());
            needsUpdate = true;
          } else {
            LOGGER.warn("Could not add {} to managers group {}", memberId, managersGroup.getId());
          }
        }
      }
      Object[] removeValues = parameters.get(PARAM_REMOVE_FROM_MANAGERS_GROUP);
      if ((removeValues != null) && (removeValues instanceof String[])) {
        for (String memberId : (String [])removeValues) {
          Authorizable authorizable = userManager.findAuthorizable(memberId);
          if (authorizable != null) {
            managersGroup.removeMember(authorizable.getId());
            needsUpdate = true;
          } else {
            LOGGER.warn("Could not remove {} from managers group {}", memberId, managersGroup.getId());
          }
        }
      }
      if (needsUpdate) {
        userManager.updateAuthorizable(managersGroup);
      }
    }
  }

  private void deleteManagersGroup(Group group, Session session) throws StorageClientException, AccessDeniedException {
    AuthorizableManager userManager = session.getAuthorizableManager();
    Group managersGroup = getManagersGroup(group, userManager);
    if (managersGroup != null) {
      LOGGER.debug("Deleting managers group {} as part of deleting {}", managersGroup.getId(), group.getId());
      userManager.delete(managersGroup.getId());
    }
  }
}
