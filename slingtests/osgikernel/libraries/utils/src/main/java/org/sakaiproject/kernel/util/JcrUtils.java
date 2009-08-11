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

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashSet;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Utilities to make simple JCR operations easier and avoid duplication.
 */
public class JcrUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(JcrUtils.class);
  
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
  public static Node deepGetOrCreateNode(Session session, String path)
      throws RepositoryException {
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

  /**
   * @param logger
   * @param node
   * @throws JSONException
   * @throws RepositoryException
   */
  public static void logItem(Logger logger, Node node) throws RepositoryException,
      JSONException {
    StringWriter sw = new StringWriter();
    JsonItemWriter dumpWriter = new JsonItemWriter(new HashSet<String>());
    dumpWriter.dump(node, sw, 5, true);
    logger.info("Node is {} ", sw.toString());
  }

  /**
   * @param node
   * @param propertyName
   * @return
   * @throws RepositoryException
   */
  public static Value[] getValues(Node node, String propertyName)
      throws RepositoryException {
    if (node.hasProperty(propertyName)) {
      Property property = node.getProperty(propertyName);
      PropertyDefinition pd = property.getDefinition();
      if (pd.isMultiple()) {
        return property.getValues();
      } else {
        return new Value[] {property.getValue()};
      }
    }
    return new Value[] {};
  }
  
  public static NodeInputStream getInputStreamForNode(Node node) {
    try {
      Node content = node.isNodeType("nt:file") ? node.getNode("jcr:content") : node;
      Property data;

      if (content.hasProperty("jcr:data")) {
        data = content.getProperty("jcr:data");
      } else {
        try {
          Item item = content.getPrimaryItem();
          while (item.isNode()) {
            item = ((Node) item).getPrimaryItem();
          }
          data = ((Property) item);
        } catch (ItemNotFoundException infe) {
          data = null;
        }
      }

      if (data != null) {
        long length = data.getLength();
        InputStream stream = data.getStream();
        return new NodeInputStream(node, stream, length);
      }
    } catch (RepositoryException re) {
      LOGGER.error("getInputStream: Cannot get InputStream for " + node, re);
    }
    return null;
  }

}
