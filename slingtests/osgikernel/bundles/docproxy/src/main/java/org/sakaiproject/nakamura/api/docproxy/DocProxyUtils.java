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
package org.sakaiproject.nakamura.api.docproxy;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.RT_EXTERNAL_REPOSITORY;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * A couple of utility classes for Document Proxying.
 */
public class DocProxyUtils {

  /**
   * Checks wether or not a node is a document proxy node.
   * 
   * @param node
   *          The node to check.
   * @return true = the node is a doc proxy node, false it is not.
   */
  public static boolean isDocProxyNode(Node node) {
    try {
      return (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY) && node.getProperty(
          SLING_RESOURCE_TYPE_PROPERTY).getString().equals(RT_EXTERNAL_REPOSITORY));
    } catch (RepositoryException e) {
      return false;
    }
  }
}
