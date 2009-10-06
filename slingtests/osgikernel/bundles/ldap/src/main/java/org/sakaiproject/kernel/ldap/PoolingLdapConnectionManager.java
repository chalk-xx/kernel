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
package org.sakaiproject.kernel.ldap;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.sakaiproject.kernel.api.ldap.LdapConnectionBroker;
import org.sakaiproject.kernel.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.kernel.api.ldap.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allocates connected, constrained, bound and optionally secure
 * <code>LDAPConnection</code>s. Uses commons-pool to provide a pool of
 * connections instead of creating a new connection for each request. Originally
 * tried implementing this with
 * <code>om.novell.ldap.connectionpool.PoolManager</code>, but it did not handle
 * recovering connections that had suffered a network error or connections that
 * were never returned but dropped out of scope.
 *
 * @see LdapConnectionManagerConfig
 * @see PooledLDAPConnection
 * @see PooledLDAPConnectionFactory
 * @author John Lewis, Unicon Inc
 */
public class PoolingLdapConnectionManager extends SimpleLdapConnectionManager {

  /** Class-specific logger */
  private static Logger log = LoggerFactory.getLogger(PoolingLdapConnectionManager.class);

  /** LDAP connection pool */
  private ObjectPool pool;

  private PooledLDAPConnectionFactory factory;

  /**
   * How long to block waiting for an available connection before throwing an
   * exception
   */
  private static final int POOL_MAX_WAIT = 60000;

  private LdapConnectionBroker broker;
  private String poolName;

  public PoolingLdapConnectionManager() {

  }

  protected PoolingLdapConnectionManager(LdapConnectionBroker broker, String poolName) {
    this.broker = broker;
    this.pool = pool;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void init() throws LdapException {
    super.init();

    if (pool != null) {
      return;
    }

    if (factory == null) {
      factory = new PooledLDAPConnectionFactory();
    }
    factory.setConnectionManager(this);

    pool = new GenericObjectPool(factory, getConfig().getPoolMaxConns(), // maxActive
        GenericObjectPool.WHEN_EXHAUSTED_BLOCK, // whenExhaustedAction
        POOL_MAX_WAIT, // maxWait (millis)
        getConfig().getPoolMaxConns(), // maxIdle
        true, // testOnBorrow
        false // testOnReturn
    );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LDAPConnection getConnection() throws LdapException {
    log.debug("getConnection(): attempting to borrow connection from pool");
    try {
      LDAPConnection conn = (LDAPConnection) pool.borrowObject();
      log.debug("getConnection(): successfully to borrowed connection from pool");
      return conn;
    } catch (LDAPException e) {
      throw new LdapException(e.getMessage(), e);
    } catch (Exception e) {
      throw new RuntimeException("failed to get pooled connection", e);
    }
  }

  @Override
  public LDAPConnection getBoundConnection(String dn, String pw) throws LdapException {
    log
        .debug(
            "getBoundConnection():dn=[{}] attempting to borrow connection from pool and bind to dn",
            dn);
    LDAPConnection conn = null;
    try {
      conn = (LDAPConnection) pool.borrowObject();
      log.debug("getBoundConnection():dn=[{}] successfully borrowed connection from pool", dn);
      conn.bind(LDAPConnection.LDAP_V3, dn, pw.getBytes("UTF8"));
      log.debug("getBoundConnection():dn=[{}] successfully bound to dn", dn);
      return conn;
    } catch (Exception e) {
      if (conn != null) {
        try {
          log.debug("getBoundConnection():dn=[{}]; error occurred, returning connection to pool",
              dn);
          returnConnection(conn);
        } catch (Exception ee) {
          log.debug("getBoundConnection():dn=[" + dn + "] failed to return connection to pool", ee);
        }
      }
      if (e instanceof LDAPException) {
        throw new LdapException(e.getMessage(), e);
      } else {
        throw new RuntimeException("failed to get pooled connection", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void returnConnection(LDAPConnection conn) {
    if (conn == null) {
      log.debug("returnConnection() received null connection; nothing to do");
      return;
    } else {
      log.debug("returnConnection(): attempting to return connection to the pool");
    }

    try {
      pool.returnObject(conn);
      log.debug("returnConnection(): successfully returned connection to pool");
    } catch (Exception e) {
      throw new RuntimeException("failed to return pooled connection", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
    if (broker != null && poolName != null) {
      broker.destroy(poolName);
    } else {
      try {
        log.debug("destroy(): closing connection pool");
        pool.close();
        log.debug("destroy(): successfully closed connection pool");
      } catch (Exception e) {
        throw new RuntimeException("failed to shutdown connection pool", e);
      }
      log.debug("destroy(): delegating to parent destroy() impl");
    }
  }

  public PooledLDAPConnectionFactory getFactory() {
    return factory;
  }

  public void setFactory(PooledLDAPConnectionFactory factory) {
    this.factory = factory;
  }

  /**
   * Assign a pool implementation. If not specified, one will be constructed by
   * {@link #init()}. If specified,
   * {@link #setFactory(PooledLDAPConnectionFactory)} will have no effect.
   *
   * <p>
   * This method exists almost entirely for testing purposes.
   * </p>
   *
   * @param pool
   *          the pool to cache; accepts <code>null</code>
   */
  protected void setPool(ObjectPool pool) {
    this.pool = pool;
  }

}
