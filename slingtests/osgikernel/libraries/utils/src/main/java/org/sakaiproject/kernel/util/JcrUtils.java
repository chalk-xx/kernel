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
package org.sakaiproject.kernel.util;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Utilities to make simple JCR operations easier and avoid duplication.
 */
public class JcrUtils {

  /**
   * Deep creates a path.
   * 
   * @param session
   * @param path
   * @param nodeType
   * @return
   * @throws RepositoryException
   */
  public static Node deepGetOrCreateNode(Session session, String path, String nodeType)
      throws RepositoryException {
    if (path == null || !path.startsWith("/")) {
      throw new IllegalArgumentException("path must be an absolute path.");
    }
    // get the starting node
    String startingNodePath = path;
    Node startingNode = null;
    while (startingNode == null) {
      if (startingNodePath.equals("/")) {
        startingNode = session.getRootNode();
      } else if (session.itemExists(startingNodePath)) {
        startingNode = (Node) session.getItem(startingNodePath);
      } else {
        int pos = startingNodePath.lastIndexOf('/');
        if (pos > 0) {
          startingNodePath = startingNodePath.substring(0, pos);
        } else {
          startingNodePath = "/";
        }
      }
    }
    // is the searched node already existing?
    if (startingNodePath.length() == path.length()) {
      return startingNode;
    }
    // create nodes
    int from = (startingNodePath.length() == 1 ? 1 : startingNodePath.length() + 1);
    Node node = startingNode;
    while (from > 0) {
      final int to = path.indexOf('/', from);
      final String name = to < 0 ? path.substring(from) : path.substring(from, to);
      boolean leafNode = to < 0;
      // although the node should not exist (according to the first test
      // above)
      // we do a sanety check.
      if (node.hasNode(name)) {
        node = node.getNode(name);
      } else {
        if (leafNode && nodeType != null) {
          node = node.addNode(name, nodeType);
        } else {
          node = node.addNode(name);
        }
      }
      from = to + 1;
    }
    return node;
  }

  /**
   * Deep creates a path.
   * 
   * @param session
   * @param path
   * @return
   * @throws RepositoryException
   */
  public static Node deepGetOrCreateNode(Session session, String path) throws RepositoryException {
    return deepGetOrCreateNode(session, path, null);
  }
  
  
  /**
   * @throws RepositoryException
   * 
   */
  public static Node getFirstExistingNode(Session session, String absRealPath)
      throws RepositoryException {
    Item item = null;
    try {
      item = session.getItem(absRealPath);
    } catch (PathNotFoundException ex) {
    }
    String parentPath = absRealPath;
    while (item == null && !"/".equals(parentPath)) {
      parentPath = PathUtils.getParentReference(parentPath);
      try {
        item = session.getItem(parentPath);
      } catch (PathNotFoundException ex) {
      }
    }
    if (item == null) {
      return null;
    }
    // convert first item to a node.
    if (!item.isNode()) {
      item = item.getParent();
    }

    return (Node) item;
  }

}
