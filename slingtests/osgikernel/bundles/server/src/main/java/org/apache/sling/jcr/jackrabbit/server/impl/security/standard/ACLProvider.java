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
package org.apache.sling.jcr.jackrabbit.server.impl.security.standard;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.AccessControlList;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.DynamicSecurityManager;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.config.SecurityManagerConfig;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.authorization.AbstractAccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AbstractCompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.AccessControlEntryIterator;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.UnmodifiableAccessControlList;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

/**
 * The ACLProvider generates access control policies out of the items stored in the
 * workspace applying the following rules:
 * <ul>
 * <li>A <code>Node</code> is considered <i>access controlled</i> if an ACL has been
 * explicitely assigned to it by adding the mixin type <code>rep:AccessControllable</code>
 * and adding child node of type <code>rep:acl</code> that forms the acl.</li>
 * <li>a Property is considered 'access controlled' if its parent Node is.</li>
 * <li>An ACL is never assigned to a <code>Property</code> item.</li>
 * <li>A <code>Node</code> that is not access controlled may inherit the ACL. The ACL is
 * inherited from the closest access controlled ancestor.</li>
 * <li>It may be possible that a given <code>Node</code> has no effective ACL, in which
 * case some a default policy is returned that grants READ privilege to any principal and
 * denies all other privileges.</li>
 * <li>an item is considered an <i>ACL item</i> if it is used to define an ACL. ACL items
 * inherit the ACL from node they defined the ACL for.</li>
 * </ul>
 * <p>
 * This a modified version of the ACLProvider with a method to get the EntryCollector,
 * intended to allow extension. This replaces the call to ACLTemplate.collectEntries()
 * </p>
 * 
 * @see AccessControlProvider for additional information.
 */

public class ACLProvider extends AbstractAccessControlProvider implements
    AccessControlConstants {

  /**
   * the default logger
   */
  private static final Logger log = LoggerFactory.getLogger(ACLProvider.class);


  /**
   * the system acl editor.
   */
  private ACLEditor systemEditor;

  /**
   * The node id of the root node
   */
  private NodeId rootNodeId;

  /**
   * Flag indicating whether or not this provider should be create the default ACLs upon
   * initialization.
   */
  private boolean initializedWithDefaults;

  private EntryCollector entryCollector;

  private SessionImpl securitySession;

  private ObservationManager securityObservationMgr;

  // -------------------------------------------------< AccessControlUtils >---
  /**
   * @see AbstractAccessControlProvider#isAcItem(Path)
   */
  public boolean isAcItem(Path absPath) throws RepositoryException {
    Path.Element[] elems = absPath.getElements();
    for (int i = 0; i < elems.length; i++) {
      if (N_POLICY.equals(elems[i].getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Test if the given node is itself a rep:ACL or a rep:ACE node.
   * 
   * @see AbstractAccessControlProvider#isAcItem(ItemImpl)
   */
  public boolean isAcItem(ItemImpl item) throws RepositoryException {
    NodeImpl n = ((item.isNode()) ? (NodeImpl) item : (NodeImpl) item.getParent());
    return n.isNodeType(NT_REP_ACL) || n.isNodeType(NT_REP_ACE);
  }

  // ----------------------------------------------< AccessControlProvider >---
  /**
   * @see AccessControlProvider#init(Session, Map)
   */
  @SuppressWarnings("unchecked")
  public void init(Session systemSession, Map configuration) throws RepositoryException {
    super.init(systemSession, configuration);

    // make sure the workspace of the given systemSession has a
    // minimal protection on the root node.
    NodeImpl root = (NodeImpl) session.getRootNode();
    RepositoryImpl repoImpl = (RepositoryImpl) session.getRepository();
    SecurityManagerConfig smc = repoImpl.getConfig().getSecurityConfig().getSecurityManagerConfig();
    String workspaceName = repoImpl.getConfig().getDefaultWorkspaceName();
    if (smc != null && smc.getWorkspaceName() != null) {
        workspaceName = smc.getWorkspaceName();
    }
    securitySession = (SessionImpl) repoImpl.login(workspaceName);
    securityObservationMgr = securitySession.getWorkspace().getObservationManager();

    rootNodeId = root.getNodeId();
    systemEditor = new ACLEditor(systemSession, this);
    initializedWithDefaults = !configuration.containsKey(PARAM_OMIT_DEFAULT_PERMISSIONS);
    if (initializedWithDefaults && !isAccessControlled(root)) {
      initRootACL(session, systemEditor);
    }

    entryCollector = new EntryCollectorImpl();
  }

  /**
   * @see AccessControlProvider#getEffectivePolicies(Path)
   * @param absPath
   */
  public AccessControlPolicy[] getEffectivePolicies(Path absPath)
      throws ItemNotFoundException, RepositoryException {
    checkInitialized();

    NodeImpl targetNode = (NodeImpl) session.getNode(session.getJCRPath(absPath));
    NodeImpl node = getNode(targetNode);
    List<AccessControlList> acls = new ArrayList<AccessControlList>();

    // collect all ACLs effective at node
    collectAcls(node, acls);
    // if no effective ACLs are present -> add a default, empty acl.
    if (acls.isEmpty()) {
      // no access control information can be retrieved for the specified
      // node, since neither the node nor any of its parents is access
      // controlled -> build a default policy.
      log.warn("No access controlled node present in item hierarchy starting from "
          + targetNode.getPath());
      acls.add(new UnmodifiableAccessControlList(Collections.EMPTY_LIST));
    }
    return (AccessControlList[]) acls.toArray(new AccessControlList[acls.size()]);
  }

  /**
   * @see AccessControlProvider#getEditor(Session)
   */
  public AccessControlEditor getEditor(Session session) {
    checkInitialized();
    return new ACLEditor(session, this);
  }

  /**
   * @see AccessControlProvider#compilePermissions(Set)
   */
  @SuppressWarnings("unchecked")
  public CompiledPermissions compilePermissions(Set principals)
      throws RepositoryException {
    checkInitialized();
    if (isAdminOrSystem(principals)) {
      return getAdminPermissions();
    } else if (isReadOnly(principals)) {
      return getReadOnlyPermissions();
    } else {
      return new AclPermissions(principals,DynamicSecurityManager.getThreadBoundAMContext());
    }
  }

  /**
   * @see AccessControlProvider#canAccessRoot(Set)
   */
  @SuppressWarnings("unchecked")
  public boolean canAccessRoot(Set principals) throws RepositoryException {
    checkInitialized();
    if (isAdminOrSystem(principals)) {
      return true;
    } else {
      CompiledPermissions cp = new AclPermissions(principals, false);
      return cp.grants(PathFactoryImpl.getInstance().getRootPath(), Permission.READ);
    }
  }

  /**
   * Get the configured collector, override this for extension and provide your own
   * implementation.
   * 
   * @return
   */
  protected EntryCollector getEntryCollector() {
    return entryCollector;
  }

  // ------------------------------------------------------------< private >---

  /**
   * Returns the given <code>targetNode</code> unless the node itself stores access
   * control information in which case it's nearest non-ac-parent is searched and
   * returned.
   * 
   * @param targetNode
   *          The node for which AC information needs to be retrieved.
   * @return
   * @throws RepositoryException
   */
  private NodeImpl getNode(NodeImpl targetNode) throws RepositoryException {
    NodeImpl node;
    if (isAcItem(targetNode)) {
      if (targetNode.isNodeType(NT_REP_ACL)) {
        node = (NodeImpl) targetNode.getParent();
      } else {
        node = (NodeImpl) targetNode.getParent().getParent();
      }
    } else {
      node = targetNode;
    }
    return node;
  }

  /**
   * Recursively collects all ACLs that are effective on the specified node.
   * 
   * @param node
   *          the Node to collect the ACLs for, which must NOT be part of the structure
   *          defined by mix:AccessControllable.
   * @param acls
   *          List used to collect the effective acls.
   * @throws RepositoryException
   */
  @SuppressWarnings("unchecked")
  private void collectAcls(NodeImpl node, List acls) throws RepositoryException {
    // if the given node is access-controlled, construct a new ACL and add
    // it to the list
    if (isAccessControlled(node)) {
      // build acl for the access controlled node
      NodeImpl aclNode = node.getNode(N_POLICY);
      AccessControlList acl = systemEditor.getACL(aclNode);
      acls.add(new UnmodifiableAccessControlList(acl));
    }
    // then, recursively look for access controlled parents up the hierarchy.
    if (!rootNodeId.equals(node.getId())) {
      NodeImpl parentNode = (NodeImpl) node.getParent();
      collectAcls(parentNode, acls);
    }
  }

  /**
   * Set-up minimal permissions for the workspace:
   * 
   * <ul>
   * <li>adminstrators principal -> all privileges</li>
   * <li>everybody -> read privilege</li>
   * </ul>
   * 
   * @param session
   *          to the workspace to set-up inital ACL to
   * @param editor for the specified session.
   * @throws RepositoryException
   */
  private static void initRootACL(SessionImpl session, AccessControlEditor editor)
      throws RepositoryException {
    try {
      log.debug("Install initial ACL:...");
      String rootPath = session.getRootNode().getPath();
      AccessControlPolicy[] acls = editor.editAccessControlPolicies(rootPath);
      ACLTemplate acl = (ACLTemplate) acls[0];

      PrincipalManager pMgr = session.getPrincipalManager();
      AccessControlManager acMgr = session.getAccessControlManager();

      log.debug("... Privilege.ALL for administrators.");
      Principal administrators;
      String pName = SecurityConstants.ADMINISTRATORS_NAME;
      if (pMgr.hasPrincipal(pName)) {
        administrators = pMgr.getPrincipal(pName);
      } else {
        log.warn("Administrators principal group is missing.");
        administrators = new PrincipalImpl(pName);
      }
      Privilege[] privs = new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_ALL)};
      acl.addAccessControlEntry(administrators, privs);

      Principal everyone = pMgr.getEveryone();
      log.debug("... Privilege.READ for everyone.");
      privs = new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_READ)};
      acl.addAccessControlEntry(everyone, privs);

      editor.setPolicy(rootPath, acl);
      session.save();

    } catch (RepositoryException e) {
      log.error("Failed to set-up minimal access control for root node of workspace "
          + session.getWorkspace().getName());
      session.getRootNode().refresh(false);
    }
  }

  /**
   * Test if the given node is access controlled. The node is access controlled if it is
   * of nodetype {@link AccessControlConstants#NT_REP_ACCESS_CONTROLLABLE
   * "rep:AccessControllable"} and if it has a child node named
   * {@link AccessControlConstants#N_POLICY "rep:ACL"}.
   * 
   * @param node
   * @return <code>true</code> if the node is access controlled; <code>false</code>
   *         otherwise.
   * @throws RepositoryException
   */
  static boolean isAccessControlled(NodeImpl node) throws RepositoryException {
    return node.isNodeType(NT_REP_ACCESS_CONTROLLABLE) && node.hasNode(N_POLICY);
  }

  // ------------------------------------------------< CompiledPermissions >---
  /**
     *
     */
  private class AclPermissions extends AbstractCompiledPermissions implements
      SynchronousEventListener {

    private class PrincipalChangeListener implements SynchronousEventListener {

      public PrincipalChangeListener() throws RepositoryException {
        int events = Event.PROPERTY_CHANGED;
        securityObservationMgr.addEventListener(this, events, "/", true, null, null, false);
      }
      
      public void onEvent(EventIterator events) {
        while (events.hasNext()) {
          try {
            Event ev = events.nextEvent();
            String path = ev.getPath();
            PropertyImpl p = (PropertyImpl) securitySession.getProperty(path);
            NodeImpl parent = (NodeImpl) p.getParent();
            if (parent.isNodeType("rep:User") && userId != null && userId.equals(parent.getName())) {
              Value[] values = p.getValues();
              List<String> groups = new ArrayList<String>(values.length);
              for (Value value : values) {
                Node n = securitySession.getNodeByUUID(value.getString());
                log.info("Got node:"  + n);
                groups.add(n.getName());
              }
              updatePrincipals(groups);
              break;
            }
          } catch (RepositoryException e) {
            log.warn("Internal error: ", e.getMessage());
          }
        }
      }

      public void close() throws RepositoryException {
        if (securityObservationMgr != null) {
          securityObservationMgr.removeEventListener(this);
        }
      }
    }
    
    private List<String> principalNames;
    private final String jcrReadPrivilegeName;

    /**
     * flag indicating that there is not 'deny READ'. -> simplify
     * {@link #grants(Path, int)} in case of permissions == READ
     */
    private boolean readAllowed = false;
    /**
     * The user ID of the user that the AclPermissions are bound to.
     */
    private String userId;
    /**
     * A reference to the AccessManagerContext to allow us to refresh
     * the list of principals
     */
    private PrincipalChangeListener principalEventListener;

    private AclPermissions(Set<Principal> principals, AMContext amContext) throws RepositoryException {
      this(principals, true);

      this.principalEventListener = new PrincipalChangeListener();
      if ( amContext != null ) {
        userId = amContext.getSession().getUserID();
      }
    }

    private AclPermissions(Set<Principal> principals, boolean listenToEvents)
        throws RepositoryException {
      principalNames = new ArrayList<String>(principals.size());
      for (Iterator<Principal> it = principals.iterator(); it.hasNext();) {
        principalNames.add(it.next().getName());
      }
      jcrReadPrivilegeName = session.getAccessControlManager().privilegeFromName(
          Privilege.JCR_READ).getName();

      if (listenToEvents) {
        /*
         * Determine if there is any 'denyRead' entry (since the default is that everyone
         * can READ everywhere -> makes evaluation for the most common check (can-read)
         * easy.
         */
        readAllowed = isReadAllowed(principalNames);

        /*
         * Make sure this AclPermission recalculates the permissions if any ACL concerning
         * it is modified. interesting events are: - new ACE-entry for any of the
         * principals (NODE_ADDED) - changing ACE-entry for any of the principals
         * (PROPERTY_CHANGED) > new permissions granted/denied > - removed ACE-entry for
         * any of the principals (NODE_REMOVED)
         */
        int events = Event.PROPERTY_CHANGED | Event.NODE_ADDED | Event.NODE_REMOVED;
        String[] ntNames = new String[] {resolver.getJCRName(NT_REP_ACE),
            resolver.getJCRName(NT_REP_ACL)};
        observationMgr.addEventListener(this, events, session.getRootNode().getPath(),
            true, null, ntNames, true);
      }
    }
    
    private void updatePrincipals(List<String> groups) {
      if (userId == null) {
        return;
      }
      for (String group : groups) {
        if (!principalNames.contains(group)) {
          principalNames.add(group);
        }
      }
    }

    /**
     * If this provider defines read-permission for everyone (defined upon init with
     * default values), search if there is any ACE that defines permissions for any of the
     * principals AND denies-READ. Otherwise this shortcut is not possible.
     * 
     * @param principalnames
     */
    @SuppressWarnings("unchecked")
    private boolean isReadAllowed(Collection principalnames) {
      boolean isReadAllowed = false;
      if (initializedWithDefaults) {
        try {
          QueryManager qm = session.getWorkspace().getQueryManager();
          StringBuffer stmt = new StringBuffer("/jcr:root");
          stmt.append("//element(*,");
          stmt.append(resolver.getJCRName(NT_REP_DENY_ACE));
          stmt.append(")[(");

          // where the rep:principalName property exactly matches any of
          // the given principalsNames
          int i = 0;
          Iterator<?> itr = principalnames.iterator();
          while (itr.hasNext()) {
            stmt.append("@").append(resolver.getJCRName(P_PRINCIPAL_NAME)).append(" eq ");
            stmt.append("'").append(itr.next().toString()).append("'");
            if (++i < principalnames.size()) {
              stmt.append(" or ");
            }
          }
          // AND rep:privileges contains the READ privilege
          stmt.append(") and @");
          stmt.append(resolver.getJCRName(P_PRIVILEGES));
          stmt.append(" = '").append(jcrReadPrivilegeName).append("']");
          

          Query q = qm.createQuery(stmt.toString(), Query.XPATH);
//          log.info("Executing Query "+stmt.toString()+" with Session of "+session.getUserID());

          NodeIterator it = q.execute().getNodes();
          isReadAllowed = !it.hasNext();
        } catch (RepositoryException e) {
          log.error(e.toString());
          // unable to determine... -> no shortcut upon grants
        }
      }
      log.debug("+++ Is Read Allowed gave {}", isReadAllowed);
      return isReadAllowed;
    }

    // ------------------------------------< AbstractCompiledPermissions >---
    /**
     * @see AbstractCompiledPermissions#buildResult(Path)
     */
    @SuppressWarnings("unchecked")
    protected Result buildResult(Path absPath) throws RepositoryException {
      boolean existingNode = false;
      NodeImpl node = null;
      String jcrPath = resolver.getJCRPath(absPath);

      if (session.nodeExists(jcrPath)) {
        node = (NodeImpl) session.getNode(jcrPath);
        existingNode = true;
      } else {
        // path points to existing prop or non-existing item (node or prop).
        // -> find the nearest persisted node
        String parentPath = Text.getRelativeParent(jcrPath, 1);
        while (parentPath.length() > 0) {
          if (session.nodeExists(parentPath)) {
            node = (NodeImpl) session.getNode(parentPath);
            break;
          }
          parentPath = Text.getRelativeParent(parentPath, 1);
        }
      }

      if (node == null) {
        // should never get here
        throw new ItemNotFoundException("Item out of hierarchy.");
      }

      boolean isAcItem = isAcItem(absPath);

      ValueFactory valueFactory = session.getValueFactory();
      // retrieve all ACEs at path or at the direct ancestor of path that
      // apply for the principal names.
      AccessControlEntryIterator entries = new Entries(getNode(node), principalNames, userId, valueFactory)
          .iterator();
      // build a list of ACEs that are defined locally at the node
      List<AccessControlEntry> localACEs;
      if (existingNode && isAccessControlled(node)) {
        NodeImpl aclNode = node.getNode(N_POLICY);
        localACEs = Arrays.asList(systemEditor.getACL(aclNode).getAccessControlEntries());
      } else {
        localACEs = Collections.EMPTY_LIST;
      }
      /*
       * Calculate privileges and permissions: Since the ACEs only define privileges on a
       * node and do not allow to add additional restrictions, the permissions can be
       * determined without taking the given target name or target item into account.
       */
      int allows = Permission.NONE;
      int denies = Permission.NONE;

      int allowPrivileges = PrivilegeRegistry.NO_PRIVILEGE;
      int denyPrivileges = PrivilegeRegistry.NO_PRIVILEGE;
      int parentAllows = PrivilegeRegistry.NO_PRIVILEGE;
      int parentDenies = PrivilegeRegistry.NO_PRIVILEGE;

      // loop through all entries (for this node and ancestors for the principal name),unless allows grants everything.
      while (entries.hasNext() && allows != privAll) {
        ACLTemplate.Entry ace = (ACLTemplate.Entry) entries.next();
        // Determine if the ACE is defined on the node at absPath (locally):
        // Except for READ-privileges the permissions must be determined
        // from privileges defined for the parent. Consequently aces
        // defined locally must be treated different than inherited entries.
        // entryBits are the privileges defined at this entry.
        int entryBits = ace.getPrivilegeBits();
        boolean isLocal = localACEs.contains(ace);
        if (!isLocal) {
          if (ace.isAllow()) {
            // remove privileges that have already been denied from the etryBits, and add them to the 
            // allows, this makes the order of the entries significant
            parentAllows |= Permission.diff(entryBits, parentDenies);
            logState("NotLocalAllow ",ace,allows,denies,allowPrivileges,denyPrivileges,parentAllows,parentDenies);
          } else {
            // remove privileges that have already been allowed from the entryBits and add them to the 
            // denies, this makes the order of the entries significant
            parentDenies |= Permission.diff(entryBits, parentAllows);
            logState("NotLocalDeny ",ace,allows,denies,allowPrivileges,denyPrivileges,parentAllows,parentDenies);
          }
        }
        if (ace.isAllow()) {
          // remove privileges that have allready been denied from the entry bits and add them to the allow
          // privileges
          allowPrivileges |= Permission.diff(entryBits, denyPrivileges);
          // apply rules to allowPrivileges merging with parentAllows (see impl of function)
          int permissions = PrivilegeRegistry.calculatePermissions(allowPrivileges, parentAllows, true, isAcItem);
          // remove permissions that have allready been denied from the permissions and add them 
          // to the allows
          allows |= Permission.diff(permissions, denies);
          logState("LocalAllow ",ace,allows,denies,allowPrivileges,denyPrivileges,parentAllows,parentDenies);
        } else {
          // remove privileges that have already been allowed from the entryBits and add them to the deny privileges
          denyPrivileges |= Permission.diff(entryBits, allowPrivileges);
          // apply rules to denyPrivileges merging with parentAllows (see impl of function)
          int permissions = PrivilegeRegistry.calculatePermissions(denyPrivileges, parentDenies, false, isAcItem);
          denies |= Permission.diff(permissions, allows);
          logState("LocalDeny ",ace,allows,denies,allowPrivileges,denyPrivileges,parentAllows,parentDenies);
        }
      }
      //
      return new Result(allows, denies, allowPrivileges, denyPrivileges);
    }


    // --------------------------------------------< CompiledPermissions >---
    /**
     * @see CompiledPermissions#close()
     */
    public void close() {
      try {
        observationMgr.removeEventListener(this);
        principalEventListener.close();
      } catch (RepositoryException e) {
        log.debug("Unable to unregister listener: ", e.getMessage());
      }
      super.close();
    }

    /**
     * 
     * @param absPath
     * @param permissions
     * @return
     * @throws RepositoryException
     * @see CompiledPermissions#grants(Path, int)
     */
    public boolean grants(Path absPath, int permissions) throws RepositoryException {
      if (permissions == Permission.READ && readAllowed && !isAcItem(absPath)) {
        return true;
      } else {
        return super.grants(absPath, permissions);
      }
    }

    // --------------------------------------------------< EventListener >---
    /**
     * @see EventListener#onEvent(EventIterator)
     */
    public synchronized void onEvent(EventIterator events) {
      // only invalidate cache if any of the events affects the
      // nodes defining permissions for principals compiled here.
      boolean clearCache = false;
      while (events.hasNext() && !clearCache) {
        try {
          Event ev = events.nextEvent();
          String path = ev.getPath();
          switch (ev.getType()) {
          case Event.NODE_ADDED:
            // test if the new node is an ACE node that affects
            // the permission of any of the principals listed in
            // principalNames.
            NodeImpl n = (NodeImpl) session.getNode(path); 
            if (n.isNodeType(NT_REP_ACE)) {
              // and reset the readAllowed flag, if the new
              // ACE denies READ.
              if (readAllowed && n.isNodeType(NT_REP_DENY_ACE)) {
                Value[] vs = n.getProperty(P_PRIVILEGES).getValues();
                for (int i = 0; i < vs.length; i++) {
                  if (jcrReadPrivilegeName.equals(vs[i].getString())) {
                    readAllowed = false;
                  }
                }
              }
              clearCache = true;
            }
            break;
          case Event.PROPERTY_REMOVED:
          case Event.NODE_REMOVED:
            // can't find out if the removed ACL/ACE node was
            // relevant for the principals
            readAllowed = isReadAllowed(principalNames);
            clearCache = true;
            break;
          case Event.PROPERTY_ADDED:
          case Event.PROPERTY_CHANGED:
            // test if the added/changed prop belongs to an ACe
            // node and affects the permission of any of the
            // principals listed in principalNames.
            PropertyImpl p = (PropertyImpl) session.getProperty(path);
            NodeImpl parent = (NodeImpl) p.getParent();
            if (parent.isNodeType(NT_REP_ACE)) {
              String principalName = null;
              if (P_PRIVILEGES.equals(p.getQName())) {
                // test if principal-name sibling-prop matches
                principalName = parent.getProperty(P_PRINCIPAL_NAME).getString();
              } else if (P_PRINCIPAL_NAME.equals(p.getQName())) {
                // a new ace or an ace change its principal-name.
                principalName = p.getString();
              }
              if (principalName != null && principalNames.contains(principalName)) {
                readAllowed = isReadAllowed(principalNames);
                clearCache = true;
              }
            }
            break;
          default:
            // illegal event-type: should never occur. ignore
          }
        } catch (RepositoryException e) {
          // should not get here
          log.warn("Internal error: ", e.getMessage());
        }
      }
      if (clearCache) {
        clearCache();
      }
    }
  }

  private class Entries {

    private final Map<String, List<AccessControlEntry>> principalNamesToEntries;
    private final List<ComparableAccessControlEntry> orderedAccessControlEntries;

    /**
     * @param node The Access control node from which the entries are to be taken.
     * @param principalNames a set of static principal names for the user.
     * @param userId userId is the user the entires are for, or null if none is bound.
     * @throws RepositoryException
     */
    @SuppressWarnings("unchecked")
    private Entries(NodeImpl node, Collection<String> principalNames, String userId, ValueFactory valueFactory)
        throws RepositoryException {
      orderedAccessControlEntries = new ArrayList<ComparableAccessControlEntry>();
      principalNamesToEntries = new ListOrderedMap();
      for (Iterator<String> it = principalNames.iterator(); it.hasNext();) {
        principalNamesToEntries.put(it.next(), new ArrayList<AccessControlEntry>());
      }
      collectEntries(node, userId, valueFactory);
      Collections.sort(orderedAccessControlEntries);
      log.debug("ACL Order for {} is {} ", node.getPath(), orderedAccessControlEntries);
    }

    /**
     * @param node the acl node
     * @param userId the userId which may be null
     * @throws RepositoryException
     */
    private void collectEntries(NodeImpl node, String userId, ValueFactory valueFactory) throws RepositoryException {
      // if the given node is access-controlled, construct a new ACL and add
      // it to the list
      if (isAccessControlled(node)) {
        // build acl for the access controlled node
        NodeImpl aclNode = node.getNode(N_POLICY);
        // get the collector and collect entries
        getEntryCollector().collectEntries(aclNode, principalNamesToEntries, orderedAccessControlEntries, userId, valueFactory);
      }
      // then, recursively look for access controlled parents up the hierarchy.
      if (!rootNodeId.equals(node.getId())) {
        NodeImpl parentNode = (NodeImpl) node.getParent();
        collectEntries(parentNode, userId, valueFactory);
      }
    }

    private AccessControlEntryIterator iterator() {
// Sakai removed to ensure that the Entries are processed in the correct order.
//      List<AccessControlEntry> entries = new ArrayList<AccessControlEntry>();
//      for (Iterator<?> it = ((ListOrderedMap) principalNamesToEntries).asList()
//          .iterator(); it.hasNext();) {
//        Object key = it.next();
//        entries.addAll(principalNamesToEntries.get(key));
//      }
      
      /* The order this needs to come out in is by node, then by principal type, then any order */
      
      return new AccessControlEntryIterator(orderedAccessControlEntries);
    }
  }

  
  /**
   * @param string
   * @param ace
   * @param allows
   * @param denies
   * @param allowPrivileges
   * @param denyPrivileges
   * @param parentAllows
   * @param parentDenies
   */
  public static void logState(String message,  ACLTemplate.Entry ace,
      int allows, int denies, int allowPrivileges, int denyPrivileges, int parentAllows,
      int parentDenies) {
    if (log.isDebugEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("allows(").append(bitToString(allows));
      sb.append(")denies(").append(bitToString(denies));
      sb.append(")allowPrivileges(").append(bitToString(allowPrivileges));
      sb.append(")denyPrivileges(").append(bitToString(denyPrivileges));
      sb.append(")parentAllows(").append(bitToString(parentAllows));
      sb.append(")parentDenies(").append(bitToString(parentDenies));
      sb.append(")").append(ace.toString()).append(",").append(message);
      log.debug(sb.toString());
    }
  }

  // the following are private in privilage registry, so duplicated here
  private static final int PrivilegeRegistry_READ = 1;

  private static final int PrivilegeRegistry_WRITE = 30;

  private static final int PrivilegeRegistry_MODIFY_PROPERTIES = 2;

  private static final int PrivilegeRegistry_REMOVE_NODE = 16;

  private static final int PrivilegeRegistry_REMOVE_CHILD_NODES = 8;

  /**
   * @param parentDenies
   * @return
   */
  private static String bitToString(int v) {
    int i = 1;
    StringBuilder sb = new StringBuilder();
    if ( (v&PrivilegeRegistry_READ) == 0 ) {
      sb.append("-");
    } else {
      sb.append("r");
    }
    if ( (v&PrivilegeRegistry_WRITE) == 0 ) {
      sb.append("-");
    } else {
      sb.append("w");
    }
    if ( (v&PrivilegeRegistry_MODIFY_PROPERTIES) == 0 ) {
      sb.append("-");
    } else {
      sb.append("P");
    }
    if ( (v&PrivilegeRegistry_REMOVE_NODE) == 0 ) {
      sb.append("-");
    } else {
      sb.append("R");
    }
    if ( (v&PrivilegeRegistry_REMOVE_CHILD_NODES) == 0 ) {
      sb.append("-");
    } else {
      sb.append("C");
    }    
    return sb.toString();
  }

}
