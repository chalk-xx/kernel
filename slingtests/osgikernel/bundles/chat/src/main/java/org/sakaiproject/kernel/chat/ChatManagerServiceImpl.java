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
package org.sakaiproject.kernel.chat;

import org.sakaiproject.kernel.api.chat.ChatManagerService;
import org.sakaiproject.kernel.api.memory.Cache;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.sakaiproject.kernel.api.memory.CacheScope;

/**
 * The <code>ChatManagerServiceImpl</code>
 * 
 * @scr.component immediate="true" label="ChatManagerServiceImpl"
 *                description="Implementation of the Chat Manager Service"
 *                name="org.sakaiproject.kernel.api.chat.ChatManagerServiceImpl"
 * @scr.service interface="org.sakaiproject.kernel.api.chat.ChatManagerService"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="service.description" value="Chat Manager Service Implementation"
 * @scr.reference name="CacheManagerService"
 *                interface="org.sakaiproject.kernel.api.memory.CacheManagerService"
 */
public class ChatManagerServiceImpl implements ChatManagerService {

  private static final String CHAT_CACHE = "chat";

  private CacheManagerService cacheManagerService;

  protected void bindCacheManagerService(CacheManagerService cacheManagerService) {
    this.cacheManagerService = cacheManagerService;
  }

  protected void unbindCacheManagerService(CacheManagerService cacheManagerService) {
    this.cacheManagerService = cacheManagerService;
  }

  /**
   * Gets the cache.
   * 
   * @return
   */
  private Cache<Long> getCachedMap() {
    return cacheManagerService.getCache(CHAT_CACHE, CacheScope.CLUSTERREPLICATED);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.chat.ChatManagerService#checkUpdate(java.lang.String,
   *      long)
   */
  public boolean checkUpdate(String userID, long time) {
    Cache<Long> cache = getCachedMap();
    if (cache.containsKey(userID) && !cache.get(userID).equals(time)) {
      // Apparently there is a new chat message.
      return true;
    }

    if (!cache.containsKey(userID)) {
      // Because the user is not in the cache yet there can be some new messages we don't
      // know about. We return true so the user can fetch them (and he get's added.)
      return true;
    }

    // The user is in the cache but has the same timestamp as the provided time.
    // Therefor there are no updates and we return false.
    return false;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.chat.ChatManagerService#setLastUpdate(java.lang.String,
   *      long)
   */
  public void setLastUpdate(String userID, long time) {
    Cache<Long> cache = getCachedMap();
    if (cache.containsKey(userID)) {
      getCachedMap().put(userID, time);
    }
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.chat.ChatManagerService#addUpdate(java.lang.String,
   *      long)
   */
  public void addUpdate(String userID, long time) {
    getCachedMap().put(userID, time);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.chat.ChatManagerService#getLastUpdate(java.lang.String)
   */
  public long getLastUpdate(String userID) {
    return getCachedMap().get(userID);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.chat.ChatManagerService#clear(java.lang.String)
   */
  public void clear(String userID) {
    Cache<Long> cache = getCachedMap();
    if (cache.containsKey(userID)) {
      cache.remove(userID);
    }

  }

}
