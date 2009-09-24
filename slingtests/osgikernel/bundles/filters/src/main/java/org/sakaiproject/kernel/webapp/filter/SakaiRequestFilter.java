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

package org.sakaiproject.kernel.webapp.filter;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.kernel.api.cluster.ClusterTrackingService;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.sakaiproject.kernel.api.memory.CacheScope;
import org.sakaiproject.kernel.api.session.SessionManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

/**
 * The <code>SakaiRequestFilter</code> class is a request level filter, which manages the
 * Sakai Cache and Transaction services..
 */
@Service(value=Filter.class)
@Component(immediate=true, metatype=false)
@Properties(value={@Property(name="service.description", value="Cache and Transaction Support Filter"),
    @Property(name="service.vendor",value="The Sakai Foundation"),
    @Property(name="filter.scope",value="request", propertyPrivate=true),
    @Property(name="filter.order",intValue={10}, propertyPrivate=true)})
public class SakaiRequestFilter implements Filter {
  private static final Logger LOGGER = LoggerFactory.getLogger(SakaiRequestFilter.class);

  private static final boolean debug = LOGGER.isDebugEnabled();

  private boolean timeOn = false;

  @Reference(name="transactionManager", bind="bindTransactionManager", unbind="unbindTransactionManager")
  private TransactionManager transactionManager;

  @Reference(name="cacheManagerService", bind="bindCacheManagerService", unbind="unbindCacheManagerService")
  private CacheManagerService cacheManagerService;
  
  @Reference(name="sessionManagerService", bind="bindSessionManagerService", unbind="unbindSessionManagerService")
  private SessionManagerService sessionManagerService;

  @Reference(name="clusterTrackingService", bind="bindClusterTrackingService", unbind="unbindClusterTrackingService")
  private ClusterTrackingService clusterTrackingService;

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  public void init(FilterConfig config) throws ServletException {

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#destroy()
   */
  public void destroy() {

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
   *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest hrequest = (HttpServletRequest) request;
    HttpServletResponse hresponse = (HttpServletResponse) response;
    clusterTrackingService.trackClusterUser(hrequest, hresponse);
    sessionManagerService.bindRequest(hrequest);
    String requestedSessionID = hrequest.getRequestedSessionId();
    try {
      begin();
      if (timeOn) {
        long start = System.currentTimeMillis();
        try {
          chain.doFilter(request, response);

        } finally {
          long end = System.currentTimeMillis();
          LOGGER.info("Request took " + hrequest.getMethod() + " "
              + hrequest.getPathInfo() + " " + (end - start) + " ms");
        }
      } else {
        chain.doFilter(request, response);
      }
      /*
       * try { if (jcrService.hasActiveSession()) { Session session =
       * jcrService.getSession(); session.save(); } } catch (AccessDeniedException e) {
       * throw new SecurityException(e.getMessage(), e); } catch (Exception e) {
       * LOGGER.warn(e.getMessage(), e); }
       */
      commit();
    } catch (SecurityException se) {
      se.printStackTrace();
      rollback();
      // catch any Security exceptions and send a 401
      hresponse.reset();
      hresponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, se.getMessage());
    } catch (RuntimeException e) {
      rollback();
      throw e;
    } catch (IOException e) {
      rollback();
      throw e;
    } catch (ServletException e) {
      rollback();
      throw e;
    } catch (Throwable t) {
      rollback();
      throw new ServletException(t.getMessage(), t);
    } finally {
      cacheManagerService.unbind(CacheScope.REQUEST);
    }
    if (debug) {
      HttpSession hsession = hrequest.getSession(false);
      if (hsession != null && !hsession.getId().equals(requestedSessionID)) {
        LOGGER.debug("New Session Created with ID " + hsession.getId());
      }
    }

  }

  /**
   * @throws SystemException
   * @throws NotSupportedException
   * 
   */
  protected void begin() throws NotSupportedException, SystemException {
    transactionManager.begin();
  }

  /**
   * @throws SystemException
   * @throws SecurityException
   * 
   */
  protected void commit() throws SecurityException, SystemException {
    try {
      if (Status.STATUS_NO_TRANSACTION != transactionManager.getStatus()) {
        transactionManager.commit();
      }
    } catch (RollbackException e) {
      if (debug) {
        LOGGER.debug(e.getMessage(), e);
      }
    } catch (IllegalStateException e) {
      if (debug) {
        LOGGER.debug(e.getMessage(), e);
      }
    } catch (HeuristicMixedException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (HeuristicRollbackException e) {
      LOGGER.warn(e.getMessage(), e);
    }
  }

  /**
   *
   */
  protected void rollback() {
    try {
      transactionManager.rollback();
    } catch (IllegalStateException e) {
      if (debug) {
        LOGGER.debug(e.getMessage(), e);
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  /**
   * @param transactionManager
   */
  protected void bindTransactionManager(TransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  /**
   * @param transactionManager
   */
  protected void unbindTransactionManager(TransactionManager transactionManager) {
    this.transactionManager = null;
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

  /**
   * @param sessionManagerService
   */
  protected void bindSessionManagerService(SessionManagerService sessionManagerService) {
    this.sessionManagerService = sessionManagerService;
  }

  /**
   * @param sessionManagerService
   */
  protected void unbindSessionManagerService(SessionManagerService sessionManagerService) {
    this.sessionManagerService = null;
  }

  
  /**
   * @param clusterTrackingService
   */
  protected void bindClusterTrackingService(ClusterTrackingService clusterTrackingService) {
    this.clusterTrackingService = clusterTrackingService;
  }

  /**
   * @param clusterTrackingService
   */
  protected void unbindClusterTrackingService(ClusterTrackingService clusterTrackingService) {
    this.clusterTrackingService = null;
  }

}
