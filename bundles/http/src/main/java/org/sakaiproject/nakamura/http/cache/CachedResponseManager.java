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

package org.sakaiproject.nakamura.http.cache;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CachedResponseManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(CachedResponseManager.class);
  private int cacheAge;
  private String key;
  private Cache<CachedResponse> cache;
  private CachedResponse cachedResponse;

  public CachedResponseManager(SlingHttpServletRequest srequest, int cacheAge, Cache<CachedResponse> cache) {
    this.cacheAge = cacheAge;
    this.key = hashKey(srequest.getPathInfo()+"?"+srequest.getQueryString());
    this.cache = cache;
    this.cachedResponse = load();
  }

  private String hashKey(String key) {
    return key;
  }

  /**
   * @return true if the CacheResponse is current and valid.
   */
  public boolean isValid() {
    return cachedResponse != null;
  }

  public void save(OperationResponseCapture responseOperation) {
    try {
      cache.put(key, new CachedResponse(responseOperation, cacheAge));
    } catch (IOException e) {
      LOGGER.error("Failed to save response in cache ",e);
    }
  }

  private CachedResponse load() {
    CachedResponse cachedResponse = null;
    cachedResponse = cache.get(key);
    if ( cachedResponse != null && !cachedResponse.isValid() ) {
      cachedResponse = null;
      cache.remove(key);
    }
    return cachedResponse;
  }

  public void send(SlingHttpServletResponse sresponse) throws IOException {
    cachedResponse.replay(sresponse);
  }
  
  @Override
  public String toString() {
    return key+"  "+cachedResponse.toString();
  }

}
