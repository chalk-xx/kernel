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
package org.sakaiproject.kernel.api.chat;

public interface ChatManagerService {

  /**
   * Checks if a userID has a new chat message by checking the cache. If the user is in
   * the cache it will check the timestamp along with it. Note: This will NOT add any
   * values to the cache, it just checks for updates!
   * 
   * @param userID
   *          The user ID to check on.
   * @param time
   *          The milliseconds when checked last time.
   * @return
   */
  public boolean checkUpdate(String userID, long time);

  /**
   * Sets a new time for a user, this will only get set when the user is already in the
   * cache.
   * 
   * @param userID
   *          The ID of the user you wish to add.
   * @param time
   *          The timestamp (milliseconds)
   */
  public void setLastUpdate(String userID, long time);

  /**
   * Adds a time for a user. Even if he/she is not in the cache already.
   * 
   * @param userID
   *          The ID of the user you wish to add.
   * @param time
   *          The timestamp (milliseconds)
   */
  public void addUpdate(String userID, long time);

  /**
   * Gets the last time a user had his chat messages updated.
   * 
   * @param userID
   *          The ID of the user you wish to add.
   * @return
   */
  public long getLastUpdate(String userID);

  /**
   * Removes a user out of the cache.
   * 
   * @param userID
   */
  public void clear(String userID);
}
