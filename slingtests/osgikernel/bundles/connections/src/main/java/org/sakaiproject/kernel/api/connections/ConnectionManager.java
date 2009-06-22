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
package org.sakaiproject.kernel.api.connections;

import org.apache.sling.api.resource.Resource;
import org.sakaiproject.kernel.api.connections.ConnectionConstants.ConnectionOperations;

/**
 * The connection manager manages state changes on connections with friends.
 */
public interface ConnectionManager {

  /**
   * Request a connection be made with a user in the store that contains the resource,
   * this will create a connection node which represents the connection between two users
   * 
   * @param resource a Sling resource (like a JCR node) which represents the path to the contacts node (the base of the connections storage)
   * @param userId
   *          the user to connect to.
   * @param types
   *          the types of this connection (e.g. friend of, professor of, student of)
   * @param requesterUserId [OPTIONAL] leave this null to use the current user OR set to the userId of the user who is making the request
   * @return the path to the connection node
   */
  String request(Resource resource, String userId, String[] types, String requesterUserId) throws ConnectionException;

  /**
   * Handle a connection operation from the current user to another user
   * 
   * @param resource a Sling resource (like a JCR node) which represents the path to the contacts node (the base of the connections storage)
   * @param userId the id of the user sending the invitation.
   * @param operation the operation to perform when connecting (accept, reject, etc.)
   * @param userId the id of the user we are connecting to
   * @param requesterUserId [OPTIONAL] leave this null to use the current user OR set to the userId of the user who is making the request
   * @return the path to the connection node
   * @throws ConnectionException 
   */
  String connect(Resource resource, String userId, ConnectionOperations operation, String requesterUserId) throws ConnectionException;

}
