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

import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AbstractAccessControlManager;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

public class SparseAccessManager extends AbstractAccessControlManager implements AccessManager {
  private PrivilegeRegistry privilegeRegistry;
  private int privAll;

  public void init(AMContext context) throws AccessDeniedException, Exception {
    privilegeRegistry = new PrivilegeRegistry(context.getNamePathResolver());
    privAll = PrivilegeRegistry.getBits(new Privilege[] {privilegeFromName(Privilege.JCR_ALL)});
  }

  public void init(AMContext context, AccessControlProvider acProvider, WorkspaceAccessManager wspAccessMgr) throws AccessDeniedException, Exception {
    init(context);
  }

  public void close() throws Exception {
    //no-op
  }

  public void checkPermission(ItemId id, int permissions) throws AccessDeniedException, ItemNotFoundException, RepositoryException {
    //no-op
  }

  public void checkPermission(Path absPath, int permissions) throws AccessDeniedException, RepositoryException {
    //no-op
  }

  public boolean isGranted(ItemId id, int permissions) throws ItemNotFoundException, RepositoryException {
    return true;
  }

  public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
    return true;
  }

  public boolean isGranted(Path parentPath, Name childName, int permissions) throws RepositoryException {
    return true;
  }

  public boolean canRead(Path itemPath) throws RepositoryException {
    return true;
  }

  public boolean canAccess(String workspaceName) throws RepositoryException {
    return true;
  }

  @Override
  protected void checkInitialized() throws IllegalStateException {
    // no-op
  }

  @Override
  protected void checkPermission(String absPath, int permission) throws AccessDeniedException, PathNotFoundException, RepositoryException {
    // no-op
  }

  @Override
  protected PrivilegeRegistry getPrivilegeRegistry() throws RepositoryException {
    return privilegeRegistry;
  }

  @Override
  protected void checkValidNodePath(String absPath) throws PathNotFoundException, RepositoryException {
    // no-op
  }

  public boolean hasPrivileges(String absPath, Set<Principal> principals, Privilege[] privileges) throws PathNotFoundException, AccessDeniedException, RepositoryException {
    return true;
  }

  public Privilege[] getPrivileges(String absPath, Set<Principal> principals) throws PathNotFoundException, AccessDeniedException, RepositoryException {
    return privilegeRegistry.getPrivileges(privAll);
  }

  public boolean hasPrivileges(String absPath, Privilege[] privileges) throws PathNotFoundException, RepositoryException {
    return true;
  }

  public Privilege[] getPrivileges(String absPath) throws PathNotFoundException, RepositoryException {
    return privilegeRegistry.getPrivileges(privAll);
  }

  @Override
  public AccessControlPolicy[] getPolicies(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
    return getEffectivePolicies(absPath);
  }

  public AccessControlPolicy[] getEffectivePolicies(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
    return new AccessControlPolicy[]{ new JackrabbitAccessControlList() {
      public AccessControlEntry[] getAccessControlEntries() throws RepositoryException {
        return new AccessControlEntry[] { new JackrabbitAccessControlEntry() {
          public Principal getPrincipal() {
            return EveryonePrincipal.getInstance();
          }

          public Privilege[] getPrivileges() {
            return privilegeRegistry.getPrivileges(privAll);
          }

          public boolean isAllow() {
            return true;
          }

          public String[] getRestrictionNames() {
            return new String[0];
          }

          public Value getRestriction(String restrictionName) {
            return null;
          }
        }};
      }

      public boolean addAccessControlEntry(Principal principal, Privilege[] privileges) throws AccessControlException, RepositoryException {
        return true;
      }

      public void removeAccessControlEntry(AccessControlEntry ace) throws AccessControlException, RepositoryException {
        // no-op
      }

      public String[] getRestrictionNames() {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
      }

      public int getRestrictionType(String restrictionName) {
        return 0;
      }

      public boolean isEmpty() {
        return false;
      }

      public int size() {
        return 1;
      }

      public boolean addEntry(Principal principal, Privilege[] privileges, boolean isAllow) throws AccessControlException, RepositoryException {
        return true;
      }

      public boolean addEntry(Principal principal, Privilege[] privileges, boolean isAllow, Map<String, Value> restrictions) throws AccessControlException, RepositoryException {
        return true;
      }

      public void orderBefore(AccessControlEntry srcEntry, AccessControlEntry destEntry) throws AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        // no-op
      }

      public String getPath() {
        return null;
      }
    }};
  }

  @Override
  public void setPolicy(String absPath, AccessControlPolicy policy)
    throws PathNotFoundException, AccessControlException, AccessDeniedException, RepositoryException {
    // no-op
  }

  @Override
  public void removePolicy(String absPath, AccessControlPolicy policy)
    throws PathNotFoundException, AccessControlException, AccessDeniedException, RepositoryException {
    // no-op
  }
}
