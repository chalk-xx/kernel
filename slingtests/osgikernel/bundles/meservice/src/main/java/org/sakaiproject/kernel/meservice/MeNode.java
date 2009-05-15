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
package org.sakaiproject.kernel.meservice;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

public class MeNode implements Node {

  private String path;

  public MeNode(String path) {
    this.path = path;
  }

  public void addMixin(String arg0) throws NoSuchNodeTypeException, VersionException,
      ConstraintViolationException, LockException, RepositoryException {
    // TODO Auto-generated method stub

  }

  public Node addNode(String arg0) throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Node addNode(String arg0, String arg1) throws ItemExistsException, PathNotFoundException,
      NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException,
      RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean canAddMixin(String arg0) throws NoSuchNodeTypeException, RepositoryException {
    // TODO Auto-generated method stub
    return false;
  }

  public void cancelMerge(Version arg0) throws VersionException, InvalidItemStateException,
      UnsupportedRepositoryOperationException, RepositoryException {
    // TODO Auto-generated method stub

  }

  public Version checkin() throws VersionException, UnsupportedRepositoryOperationException,
      InvalidItemStateException, LockException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public void checkout() throws UnsupportedRepositoryOperationException, LockException,
      RepositoryException {
    // TODO Auto-generated method stub

  }

  public void doneMerge(Version arg0) throws VersionException, InvalidItemStateException,
      UnsupportedRepositoryOperationException, RepositoryException {
    // TODO Auto-generated method stub

  }

  public Version getBaseVersion() throws UnsupportedRepositoryOperationException,
      RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public String getCorrespondingNodePath(String arg0) throws ItemNotFoundException,
      NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public NodeDefinition getDefinition() throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public int getIndex() throws RepositoryException {
    // TODO Auto-generated method stub
    return 0;
  }

  public Lock getLock() throws UnsupportedRepositoryOperationException, LockException,
      AccessDeniedException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public NodeType[] getMixinNodeTypes() throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Node getNode(String arg0) throws PathNotFoundException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public NodeIterator getNodes() throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public NodeIterator getNodes(String arg0) throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public NodeType getPrimaryNodeType() throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public PropertyIterator getProperties() throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public PropertyIterator getProperties(String arg0) throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property getProperty(String arg0) throws PathNotFoundException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public PropertyIterator getReferences() throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException,
      RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean hasNode(String arg0) throws RepositoryException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean hasNodes() throws RepositoryException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean hasProperties() throws RepositoryException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean hasProperty(String arg0) throws RepositoryException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean holdsLock() throws RepositoryException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isCheckedOut() throws RepositoryException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isLocked() throws RepositoryException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isNodeType(String arg0) throws RepositoryException {
    // TODO Auto-generated method stub
    return false;
  }

  public Lock lock(boolean arg0, boolean arg1) throws UnsupportedRepositoryOperationException,
      LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public NodeIterator merge(String arg0, boolean arg1) throws NoSuchWorkspaceException,
      AccessDeniedException, MergeException, LockException, InvalidItemStateException,
      RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public void orderBefore(String arg0, String arg1) throws UnsupportedRepositoryOperationException,
      VersionException, ConstraintViolationException, ItemNotFoundException, LockException,
      RepositoryException {
    // TODO Auto-generated method stub

  }

  public void removeMixin(String arg0) throws NoSuchNodeTypeException, VersionException,
      ConstraintViolationException, LockException, RepositoryException {
    // TODO Auto-generated method stub

  }

  public void restore(String arg0, boolean arg1) throws VersionException, ItemExistsException,
      UnsupportedRepositoryOperationException, LockException, InvalidItemStateException,
      RepositoryException {
    // TODO Auto-generated method stub

  }

  public void restore(Version arg0, boolean arg1) throws VersionException, ItemExistsException,
      UnsupportedRepositoryOperationException, LockException, RepositoryException {
    // TODO Auto-generated method stub

  }

  public void restore(Version arg0, String arg1, boolean arg2) throws PathNotFoundException,
      ItemExistsException, VersionException, ConstraintViolationException,
      UnsupportedRepositoryOperationException, LockException, InvalidItemStateException,
      RepositoryException {
    // TODO Auto-generated method stub

  }

  public void restoreByLabel(String arg0, boolean arg1) throws VersionException,
      ItemExistsException, UnsupportedRepositoryOperationException, LockException,
      InvalidItemStateException, RepositoryException {
    // TODO Auto-generated method stub

  }

  public Property setProperty(String arg0, Value arg1) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, Value[] arg1) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, String[] arg1) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, String arg1) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, InputStream arg1) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, boolean arg1) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, double arg1) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, long arg1) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, Calendar arg1) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, Node arg1) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, Value arg1, int arg2) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, Value[] arg1, int arg2) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, String[] arg1, int arg2) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Property setProperty(String arg0, String arg1, int arg2) throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public void unlock() throws UnsupportedRepositoryOperationException, LockException,
      AccessDeniedException, InvalidItemStateException, RepositoryException {
    // TODO Auto-generated method stub

  }

  public void update(String arg0) throws NoSuchWorkspaceException, AccessDeniedException,
      LockException, InvalidItemStateException, RepositoryException {
    // TODO Auto-generated method stub

  }

  public void accept(ItemVisitor arg0) throws RepositoryException {
    // TODO Auto-generated method stub

  }

  public Item getAncestor(int arg0) throws ItemNotFoundException, AccessDeniedException,
      RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public int getDepth() throws RepositoryException {
    // TODO Auto-generated method stub
    return 0;
  }

  public String getName() throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public String getPath() throws RepositoryException {
    return path;
  }

  public Session getSession() throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isModified() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isNew() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isNode() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isSame(Item arg0) throws RepositoryException {
    // TODO Auto-generated method stub
    return false;
  }

  public void refresh(boolean arg0) throws InvalidItemStateException, RepositoryException {
    // TODO Auto-generated method stub

  }

  public void remove() throws VersionException, LockException, ConstraintViolationException,
      RepositoryException {
    // TODO Auto-generated method stub

  }

  public void save() throws AccessDeniedException, ItemExistsException,
      ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException,
      VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
    // TODO Auto-generated method stub

  }

}
