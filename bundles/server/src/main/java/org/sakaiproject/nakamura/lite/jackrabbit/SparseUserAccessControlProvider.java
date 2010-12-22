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

import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractAccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.spi.Path;

import java.security.Principal;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlPolicy;

public class SparseUserAccessControlProvider extends AbstractAccessControlProvider {

  public AccessControlPolicy[] getEffectivePolicies(Path absPath)
      throws ItemNotFoundException, RepositoryException {
    return null;
  }

  public AccessControlEditor getEditor(Session session) throws RepositoryException {
    return null;
  }

  public CompiledPermissions compilePermissions(Set<Principal> principals)
      throws RepositoryException {
    return new CompiledPermissions() {

      public boolean grants(Path absPath, int permissions) throws RepositoryException {
        return true;
      }

      public int getPrivileges(Path absPath) throws RepositoryException {
        return 127;
      }

      public void close() {
      }

      public boolean canReadAll() throws RepositoryException {
        return true;
      }
    };
  }

  public boolean canAccessRoot(Set<Principal> principals) throws RepositoryException {
    return true;
  }

  public boolean isAcItem(Path absPath) throws RepositoryException {
    return false;
  }

  public boolean isAcItem(ItemImpl item) throws RepositoryException {
    return false;
  }

}
