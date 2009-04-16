package org.sakaiproject.kernel.session;

import org.sakaiproject.kernel.api.memory.Cache;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.sakaiproject.kernel.api.memory.CacheScope;
import org.sakaiproject.kernel.api.session.SessionManagerService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * The <code>SessionManagerServiceImpl</code>
 *
 * @scr.component immediate="true" label="SessionManagerServiceImpl" description="Implementation of the Session Manager Service"
 *          name="org.sakaiproject.kernel.api.session.SessionManagerService"
 * @scr.service interface="org.sakaiproject.kernel.api.session.SessionManagerService"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="service.description"
 *      value="Session Manager Service Implementation"
 * @scr.reference name="cacheManagerService"
 *                interface="org.sakaiproject.kernel.api.memory.CacheManagerService"
 *                bind="bindCacheManagerService" unbind="unbindCacheManagerService"
 */
public class SessionManagerServiceImpl implements SessionManagerService {

  private static final String REQUEST_CACHE = "request";
  private static final String CURRENT_REQUEST = "_r";
  private CacheManagerService cacheManagerService;
  
  protected Cache<HttpServletRequest> getRequestScope()
  {
    return cacheManagerService.getCache(REQUEST_CACHE, CacheScope.REQUEST); 
  }
  
  public void bindRequest(HttpServletRequest request) {
    getRequestScope().put(CURRENT_REQUEST, request);
  }

  public HttpServletRequest getCurrentRequest() {
    return getRequestScope().get(CURRENT_REQUEST);
  }

  public HttpSession getCurrentSession() {
    return getCurrentRequest().getSession();
  }

  public String getCurrentUserId() {
    return getCurrentRequest().getRemoteUser();
  }

  /**
   * @param cacheManagerService
   */
  protected void bindCacheManagerService(CacheManagerService cacheManagerService) {
    this.cacheManagerService = cacheManagerService;
  }

  /**
   * @param cacheManagerService
   */
  protected void unbindCacheManagerService(CacheManagerService cacheManagerService) {
    this.cacheManagerService = null;
  }

}
