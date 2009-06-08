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
import org.apache.jackrabbit.core.NodeImpl;

import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Implementations of this class collect together ACE's based on instance of the object
 * and its configuration. This may take account of the node, user, session, patch
 * environment etc. This is an extension API that will allow extension of the
 * ACLProvider with different EntryCollectors.
 */
public interface EntryCollector {

  /**
   * Collect ACE's for the configured context.
   * 
   * @param aclNode
   * @param principalNamesToEntries
   * @throws RepositoryException
   */
  void collectEntries(NodeImpl aclNode,
      Map<String, List<AccessControlEntry>> principalNamesToEntries)
      throws RepositoryException;

}
