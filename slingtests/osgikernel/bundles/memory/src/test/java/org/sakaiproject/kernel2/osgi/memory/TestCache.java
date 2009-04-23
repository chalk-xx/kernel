package org.sakaiproject.kernel2.osgi.memory;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.api.memory.Cache;
import org.sakaiproject.kernel.api.memory.CacheScope;
import org.sakaiproject.kernel.api.memory.ThreadBound;
import org.sakaiproject.kernel.memory.CacheManagerServiceImpl;

import java.io.IOException;

public class TestCache {

  private CacheManagerServiceImpl cacheManagerService;
  
  @Before
  public void setUp() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
    cacheManagerService = new CacheManagerServiceImpl();
  }

  private void exerciseCache(String cacheName, CacheScope scope)
  {
    Cache<String> cache = cacheManagerService.getCache(cacheName, scope);
    cache.put("fish", "cat");
    assertTrue("Expected element to be in cache", cache.containsKey("fish"));
    Cache<String> sameCache = cacheManagerService.getCache(cacheName, scope);
    assertEquals("Expected cache to work", "cat", sameCache.get("fish"));
    sameCache.put("fish", "differentcat");
    assertEquals("Expected cache value to propogate", "differentcat", cache.get("fish"));
    sameCache.remove("fish");
    sameCache.remove("another");
    assertNull("Expected item to be removed from cache", cache.get("fish"));
    cache.put("foo", "bar");
    cache.clear();
    assertNull("Expected cache to be empty", cache.get("foo"));
    cacheManagerService.unbind(scope);
  }
  
  @Test
  public void testCacheStorage() {
    for (CacheScope scope : CacheScope.values())
    {
      exerciseCache("TestCache", scope);
    }
  }

  @Test
  public void testNullCacheNames() {
    for (CacheScope scope : CacheScope.values())
    {
      exerciseCache(null, scope);
    }
  }
  
  @Test
  public void testCacheWithChildKeys() {
    for (CacheScope scope : CacheScope.values())
    {
      String cacheName = "SomeTestCache";
      Cache<String> cache = cacheManagerService.getCache(cacheName, scope);
      cache.put("fish", "cat");
      assertTrue("Expected element to be in cache", cache.containsKey("fish"));
      cache.put("fish/child", "childcat");
      cache.put("fish/child/child", "childcatchild");
      Cache<String> sameCache = cacheManagerService.getCache(cacheName, scope);
      sameCache.removeChildren("fish/child/child");
      assertNull("Expected key to be removed", cache.get("fish/child/child"));
      sameCache.removeChildren("fish");
      assertNull("Expected key to be removed", cache.get("fish"));
      assertNull("Expected key to be removed", cache.get("fish/child"));
    }    
  }
  
  @Test
  public void testThreadUnbinding()
  {
    ThreadBound testItem = createMock(ThreadBound.class);
    testItem.unbind();
    testItem.unbind();
    replay(testItem);
    Cache<ThreadBound> threadBoundCache = cacheManagerService.getCache("testCache", CacheScope.THREAD);
    threadBoundCache.put("testItem", testItem);
    threadBoundCache.remove("testItem");
    threadBoundCache.put("testItem", testItem);
    threadBoundCache.clear();
    verify(testItem);
  }

}
