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
package org.apache.sling.jcr.jackrabbit.server.impl.security.standard;

import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.sling.jcr.jackrabbit.server.impl.security.standard.ACLTemplate.Entry;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * This EntryCollector implementation uses a principal manager to check each potential
 * principal against a dynamic principal manager to see if the ACE should be included in
 * the resolved entry.
 */
public class EntryCollectorImpl implements EntryCollector {

  /**
   * Construct this type of EntryCollector with a principal manager.
   */
  public EntryCollectorImpl() {
  }

  /**
   * Separately collect the entries defined for the principals with the specified names
   * and return a map consisting of principal name key and a list of ACEs as value.
   * 
   * @param aclNode
   * @param princToEntries
   *          Map of key = principalName and value = ArrayList to be filled with ACEs
   *          matching the principal names.
   * @throws RepositoryException
   */
  public void collectEntries(NodeImpl aclNode,
      Map<String, List<AccessControlEntry>> princToEntries) throws RepositoryException {
    SessionImpl sImpl = (SessionImpl) aclNode.getSession();
    PrincipalManager principalMgr = sImpl.getPrincipalManager();
    AccessControlManager acMgr = sImpl.getAccessControlManager();

    NodeIterator itr = aclNode.getNodes();
    while (itr.hasNext()) {
      NodeImpl aceNode = (NodeImpl) itr.nextNode();
      String principalName = aceNode.getProperty(AccessControlConstants.P_PRINCIPAL_NAME)
          .getString();
      // only process aceNode if 'principalName' is contained in the given set
      // or the dynamicPrincialManager says the user has the principal.

      if (hasPrincipal(principalName, aclNode, princToEntries)) {
        Principal princ = principalMgr.getPrincipal(principalName);

        Value[] privValues = aceNode.getProperty(AccessControlConstants.P_PRIVILEGES)
            .getValues();
        Privilege[] privs = new Privilege[privValues.length];
        for (int i = 0; i < privValues.length; i++) {
          privs[i] = acMgr.privilegeFromName(privValues[i].getString());
        }
        // create a new ACEImpl (omitting validation check)
        Entry ace = new Entry(princ, privs, aceNode
            .isNodeType(AccessControlConstants.NT_REP_GRANT_ACE));
        // add it to the proper list (e.g. separated by principals)
        List<AccessControlEntry> l = princToEntries.get(principalName);
        if (l == null) {
          l = new ArrayList<AccessControlEntry>();
          l.add(ace);
          princToEntries.put(principalName, l);
        } else {
          l.add(ace);
        }
      }
    }
  }

  /**
   * Does the user have this principal, the standard implementation just checks to see if it
   * was added to the map.
   * @param session 
   * 
   * @param principalName the name to check
   * @param aclNode the aclNode being constructed
   * @param princToEntries the processed ACE map.
   * @return true if the user has the principal.
   */
  protected boolean hasPrincipal(String principalName, NodeImpl aclNode,
      Map<String, List<AccessControlEntry>> princToEntries) {
    return princToEntries.containsKey(principalName);
  }

}
