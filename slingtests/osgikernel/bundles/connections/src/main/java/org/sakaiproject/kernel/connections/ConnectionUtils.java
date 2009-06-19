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

package org.sakaiproject.kernel.connections;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.util.JcrUtils;

/**
 * Simple utils which help us when working with connections
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ConnectionUtils {

  /**
   * Gets the first node which matches a given type starting at the current resource
   * 
   * @param store
   *          a resource within the store.
   * @param resourceType
   *          the type to stop at when a node is found
   * @return the first Node that matches the resourceType OR null if none found
   * @throws RepositoryException
   */
  public static Node getStoreNode(Resource store, String resourceType) throws RepositoryException {
    if (store == null || resourceType == null) {
      throw new IllegalArgumentException("store and resourceType must be set");
    }
    Session session = store.getResourceResolver().adaptTo(Session.class);
    String path = store.getPath();
    Node node = JcrUtils.getFirstExistingNode(session, path);
    return findNode(resourceType, node);
  }

  /**
   * Gets the first node which matches a given type starting at the current node
   * 
   * @param node
   *          a node within the store.
   * @param resourceType
   *          the type to stop at when a node is found
   * @return the first Node that matches the resourceType OR null if none found
   * @throws RepositoryException
   */
  public static Node getStoreNode(Node node, String resourceType) throws RepositoryException {
    if (node == null || resourceType == null) {
      throw new IllegalArgumentException("store and resourceType must be set");
    }
    Session session = node.getSession();
    String path = node.getPath();
    Node n = JcrUtils.getFirstExistingNode(session, path);
    return findNode(resourceType, n);
  }

  /**
   * @param resourceType the type to limit the search by
   * @param node the node to start searching from
   * @return the first Node that matches the resourceType OR null if none found
   * @throws RepositoryException
   */
  private static Node findNode(String resourceType, Node node)
      throws RepositoryException {
    while (!"/".equals(node.getPath())) {
      if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
          && resourceType.equals(node.getProperty(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
        return node;
      }
      node = node.getParent();
    }
    return null;
  }

}
