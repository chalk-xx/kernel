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

/**
 * The connection manager manages state changes on connections with friends.
 */
public interface ConnectionManager {

  /**
   * Request a connection be made with a user in the store that contains the resource
   * 
   * @param store a {@link Resource} contained within the connection store.
   * @param userId
   *          the user to connect to.
   * @param type
   *          the type to associate with the user.
   * @return a path to the resource representing the connection.
   */
  String invite(Resource store, String userId, String type) throws ConnectionException;

  
  /**
   * Accept a connection in the store from the user.
   * @param store a {@link Resource} contained within the connection store.
   * @param userId the id of the user sending the invitation.
   * @throws ConnectionException 
   */
  void accept(Resource store, String userId) throws ConnectionException;
  
  /**
   * Reject an invitation in the store for the user. 
   * @param store a {@link Resource} contained within the connection store.
   * @param userId the id of the user sending the invitation.
   */
  void reject(Resource store, String userId) throws ConnectionException;
  
  /**
   * Ignore an invitation from a user in the store identified by store.
   * @param store a {@link Resource} contained within the connection store.
   * @param userId the id of the user sending the invitation.
   */
  void ignore(Resource store, String userId) throws ConnectionException;
  
  /**
   * Block this and future invitations from the user
   * @param store a {@link Resource} contained within the connection store.
   * @param userId the id of the user sending the invitation.
   */
  void block(Resource store, String userId) throws ConnectionException;
  
  /**
   * Cancel an invitation.
   * @param store a {@link Resource} contained within the connection store.
   * @param userId the id of the user sending the invitation.
   */
  void cancel(Resource store, String userId) throws ConnectionException;
  
  /**
   * @param store a {@link Resource} contained within the connection store.
   * @param userId the id of the user sending the invitation.
   */
  void remove(Resource store, String userId) throws ConnectionException;
}
