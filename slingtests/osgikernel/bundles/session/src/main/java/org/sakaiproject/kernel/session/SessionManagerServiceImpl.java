package org.sakaiproject.kernel.session;

import com.google.inject.Inject;

import org.sakaiproject.kernel.api.memory.Cache;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.sakaiproject.kernel.api.memory.CacheScope;
import org.sakaiproject.kernel.api.session.SessionManagerService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class SessionManagerServiceImpl implements SessionManagerService {

  private static final String REQUEST_CACHE = "request";
  private static final String CURRENT_REQUEST = "_r";
  private CacheManagerService cacheManagerService;

  @Inject
  public SessionManagerServiceImpl(CacheManagerService cacheManagerService)
  {
    this.cacheManagerService = cacheManagerService;
  }
  
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

}
