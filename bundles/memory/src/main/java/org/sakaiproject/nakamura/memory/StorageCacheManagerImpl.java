package org.sakaiproject.nakamura.memory;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.lite.CacheHolder;
import org.sakaiproject.nakamura.api.lite.StorageCacheManager;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;

import java.util.Map;

@Component(immediate=true, metatype=true)
@Service(value=StorageCacheManager.class)
public class StorageCacheManagerImpl implements StorageCacheManager {

  private Map<String, CacheHolder> accessControlCache;
  private Map<String, CacheHolder> authorizableCache;
  private Map<String, CacheHolder> contentCache;
  
  @Reference
  private CacheManagerService cacheManagerService;

  @Activate
  public void activate(Map<String, Object> props) {
    Cache<CacheHolder> accesssControlCacheCache = cacheManagerService.getCache("accessControlCache", CacheScope.CLUSTERINVALIDATED);
    Cache<CacheHolder> authorizableCacheCache = cacheManagerService.getCache("authorizableCache", CacheScope.CLUSTERINVALIDATED);
    Cache<CacheHolder> contentCacheCache = cacheManagerService.getCache("contentCache", CacheScope.CLUSTERINVALIDATED);
    accessControlCache = new MapDeligate<String, CacheHolder>(accesssControlCacheCache);
    authorizableCache = new MapDeligate<String, CacheHolder>(authorizableCacheCache);
    contentCache = new MapDeligate<String, CacheHolder>(contentCacheCache);
  }
  
  @Deactivate
  public void deactivate(Map<String, Object> props) {
    
  }
  
  
  public Map<String, CacheHolder> getAccessControlCache() {
    return accessControlCache;
  }

  public Map<String, CacheHolder> getAuthorizableCache() {
    return authorizableCache;
  }

  public Map<String, CacheHolder> getContentCache() {
    return contentCache;
  }

}
