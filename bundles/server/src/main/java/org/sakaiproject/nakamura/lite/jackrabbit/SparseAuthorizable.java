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
package org.sakaiproject.nakamura.lite.jackrabbit;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.util.Iterables;
import org.sakaiproject.nakamura.api.lite.util.PreemptiveIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

public class SparseAuthorizable implements Authorizable {

  private static final Logger LOGGER = LoggerFactory.getLogger(SparseAuthorizable.class);
  protected org.sakaiproject.nakamura.api.lite.authorizable.Authorizable sparseAuthorizable;
  AuthorizableManager authorizableManager;
  Principal principal;
  ValueFactory valueFactory;
  AccessControlManager accessControlManager;

  public SparseAuthorizable(
      org.sakaiproject.nakamura.api.lite.authorizable.Authorizable authorizable,
      AuthorizableManager authorizableManager, AccessControlManager accessControlManager,
      ValueFactory valueFactory) {
    this.valueFactory = valueFactory;
    this.sparseAuthorizable = authorizable;
    this.authorizableManager = authorizableManager;
    this.accessControlManager = accessControlManager;
  }

  public String getID() throws RepositoryException {
    return sparseAuthorizable.getId();
  }

  public boolean isGroup() {
    return false;
  }

  public Principal getPrincipal() throws RepositoryException {
    return principal;
  }

  public Iterator<Group> declaredMemberOf() throws RepositoryException {
    final Iterator<String> memberIterator = Iterables.of(
        sparseAuthorizable.getPrincipals()).iterator();
    return new PreemptiveIterator<Group>() {

      private SparseGroup group;


      protected boolean internalHasNext() {
        while (memberIterator.hasNext()) {
          try {
            String id = memberIterator.next();
            org.sakaiproject.nakamura.api.lite.authorizable.Authorizable a = authorizableManager
                .findAuthorizable(id);
            if (a instanceof org.sakaiproject.nakamura.api.lite.authorizable.Group) {
              group = new SparseGroup(
                  (org.sakaiproject.nakamura.api.lite.authorizable.Group) a,
                  authorizableManager, accessControlManager, valueFactory);
              return true;
            }
          } catch (AccessDeniedException e) {
            LOGGER.debug(e.getMessage(), e);
          } catch (StorageClientException e) {
            LOGGER.debug(e.getMessage(), e);
          }

        }
        return false;
      }

      protected Group internalNext() {
        return group;
      }

    };
  }

  public Iterator<Group> memberOf() throws RepositoryException {
    final List<String> memberIds = new ArrayList<String>();
    Collections.addAll(memberIds, sparseAuthorizable.getPrincipals());
    return new PreemptiveIterator<Group>() {

      private int p;
      private SparseGroup group;

      protected boolean internalHasNext() {
        while (p < memberIds.size()) {
          String id = memberIds.get(p);
          p++;
          try {
            org.sakaiproject.nakamura.api.lite.authorizable.Authorizable a = authorizableManager
                .findAuthorizable(id);
            if (a instanceof org.sakaiproject.nakamura.api.lite.authorizable.Group) {
              group = new SparseGroup(
                  (org.sakaiproject.nakamura.api.lite.authorizable.Group) a,
                  authorizableManager, accessControlManager, valueFactory);
              for (String pid : a.getPrincipals()) {
                if (!memberIds.contains(pid)) {
                  memberIds.add(pid);
                }
              }
              return true;
            }
          } catch (AccessDeniedException e) {
            LOGGER.debug(e.getMessage(), e);
          } catch (StorageClientException e) {
            LOGGER.debug(e.getMessage(), e);
          }
        }
        return false;
      }

      protected Group internalNext() {
        return group;
      }

    };
  }

  public void remove() throws RepositoryException {
    try {
      authorizableManager.delete(sparseAuthorizable.getId());
    } catch (AccessDeniedException e) {
      throw new javax.jcr.AccessDeniedException(e.getMessage(), e);
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    }
  }

  public Iterator<String> getPropertyNames() throws RepositoryException {
    return sparseAuthorizable.getSafeProperties().keySet().iterator();
  }

  public boolean hasProperty(String name) throws RepositoryException {
    return sparseAuthorizable.hasProperty(name);
  }

  public void setProperty(String name, Value value) throws RepositoryException {
    String sv = value.getString();
    List<String> values = new ArrayList<String>();
    values.add(sv);
    sparseAuthorizable.setProperty(name, value.getString());
    save();
    try {
      // FIXME: ACL via properties was always wrong should be via an ACL call.
      if ("rep:group-managers".equals(name)) {
        setManagers(values);
      } else if ("rep:group-viewers".equals(name)) {
        setViewers(values);

      }
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new javax.jcr.AccessDeniedException(e.getMessage(), e);
    }
  }

  public void setProperty(String name, Value[] value) throws RepositoryException {
    List<String> values = new ArrayList<String>();
    for (Value v : value) {
      String sv = v.getString();
      values.add(sv);
    }
    sparseAuthorizable.setProperty(name, values.toArray(new String[values.size()]));
    save();
    try {
      // FIXME: ACL via properties was always wrong.
      if ("rep:group-managers".equals(name)) {
        setManagers(values);
      } else if ("rep:group-viewers".equals(name)) {
        setViewers(values);
      }
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new javax.jcr.AccessDeniedException(e.getMessage(), e);
    }

  }

  public Value[] getProperty(String name) throws RepositoryException {
    Object s = sparseAuthorizable.getProperty(name);
    if (s != null) {
      if ( s instanceof Object[] ) {
        Object[] parts = (Object[]) s;
        Value[] v = new Value[parts.length];
        for (int i = 0; i < parts.length; i++) {
          v[i] = valueFactory.createValue(String.valueOf(parts[i]));
        }
        return v;        
      } else {
        Value[] v = new Value[1];
        v[0] = valueFactory.createValue(String.valueOf(s));
      }
    }
    return null;
  }

  public boolean removeProperty(String name) throws RepositoryException {
    sparseAuthorizable.removeProperty(name);
    save();
    try {
      // FIXME: ACL via properties was always wrong.
      if ("rep:group-managers".equals(name)) {
        clearManagers(sparseAuthorizable.hasProperty("rep:group-viewers"));
      } else if ("rep:group-viewers".equals(name)) {
        clearViewers();
      }
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new javax.jcr.AccessDeniedException(e.getMessage(), e);
    }
    return true;
  }

  protected void save() throws RepositoryException {
    // dumb impl at the moment, would be better if there was a hook on the
    // JCR session save.
    try {
      authorizableManager.updateAuthorizable(sparseAuthorizable);
      sparseAuthorizable = authorizableManager.findAuthorizable(sparseAuthorizable
          .getId());
    } catch (AccessDeniedException e) {
      throw new javax.jcr.AccessDeniedException(e.getMessage(), e);
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (Exception e) {
      LOGGER.error("Failed to save authorizable", e);
    }
  }

  private void clearViewers() throws StorageClientException, AccessDeniedException {
    Map<String, Object> acl = accessControlManager.getAcl(Security.ZONE_AUTHORIZABLES,
        sparseAuthorizable.getId());
    List<AclModification> modifications = new ArrayList<AclModification>();
    // remove all keys that restrict read and add a everyone read
    AclModification.filterAcl(acl, false, Permissions.CAN_READ, false, modifications);
    AclModification.addAcl(true, Permissions.CAN_READ,
        org.sakaiproject.nakamura.api.lite.authorizable.Group.EVERYONE, modifications);
    AclModification.addAcl(true, Permissions.CAN_READ,
        org.sakaiproject.nakamura.api.lite.authorizable.User.ANON_USER, modifications);
    accessControlManager.setAcl(Security.ZONE_AUTHORIZABLES, sparseAuthorizable.getId(),
        modifications.toArray(new AclModification[modifications.size()]));
  }

  private void clearManagers(boolean privateView) throws StorageClientException,
      AccessDeniedException {
    Map<String, Object> acl = accessControlManager.getAcl(Security.ZONE_AUTHORIZABLES,
        sparseAuthorizable.getId());
    List<AclModification> modifications = new ArrayList<AclModification>();
    // remove all keys that restrict read and add a everyone read,
    AclModification.filterAcl(acl, true, Permissions.CAN_MANAGE, false, modifications);
    if (!privateView) {
      AclModification.addAcl(true, Permissions.CAN_READ,
          org.sakaiproject.nakamura.api.lite.authorizable.Group.EVERYONE, modifications);
      AclModification.addAcl(true, Permissions.CAN_READ,
          org.sakaiproject.nakamura.api.lite.authorizable.User.ANON_USER, modifications);
    }
    if (modifications.size() > 0) {
      accessControlManager.setAcl(Security.ZONE_AUTHORIZABLES,
          sparseAuthorizable.getId(),
          modifications.toArray(new AclModification[modifications.size()]));
    }
  }

  private void setViewers(List<String> values) throws StorageClientException,
      AccessDeniedException {
    Map<String, Object> acl = accessControlManager.getAcl(Security.ZONE_AUTHORIZABLES,
        sparseAuthorizable.getId());
    List<AclModification> modifications = new ArrayList<AclModification>();
    // remove any other read grant
    AclModification.filterAcl(acl, false, Permissions.CAN_READ, false, modifications);
    // add the viewers in
    for (String viewer : values) {
      AclModification.addAcl(true, Permissions.CAN_READ, viewer, modifications);
    }
    // deny everyone, there is a default read assumption on all users and
    // groups.
    AclModification.addAcl(false, Permissions.CAN_READ,
        org.sakaiproject.nakamura.api.lite.authorizable.Group.EVERYONE, modifications);
    AclModification.addAcl(false, Permissions.CAN_READ,
        org.sakaiproject.nakamura.api.lite.authorizable.User.ANON_USER, modifications);
    accessControlManager.setAcl(Security.ZONE_AUTHORIZABLES, sparseAuthorizable.getId(),
        modifications.toArray(new AclModification[modifications.size()]));
  }

  private void setManagers(List<String> values) throws StorageClientException,
      AccessDeniedException {
    Map<String, Object> acl = accessControlManager.getAcl(Security.ZONE_AUTHORIZABLES,
        sparseAuthorizable.getId());
    List<AclModification> modifications = new ArrayList<AclModification>();
    // remove any other read grant
    AclModification.filterAcl(acl, false, Permissions.CAN_MANAGE, false, modifications);
    // add the viewers in
    for (String viewer : values) {
      AclModification.addAcl(true, Permissions.CAN_MANAGE, viewer, modifications);
    }
    accessControlManager.setAcl(Security.ZONE_AUTHORIZABLES, sparseAuthorizable.getId(),
        modifications.toArray(new AclModification[modifications.size()]));
  }

}
