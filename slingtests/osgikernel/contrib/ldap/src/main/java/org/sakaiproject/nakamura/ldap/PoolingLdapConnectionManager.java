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
package org.sakaiproject.nakamura.ldap;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionLivenessValidator;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.nakamura.api.ldap.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

/**
 * Allocates connected, constrained, bound and optionally secure
 * <code>LDAPConnection</code>s. Uses commons-pool to provide a pool of connections
 * instead of creating a new connection for each request. Originally tried implementing
 * this with <code>om.novell.ldap.connectionpool.PoolManager</code>, but it did not handle
 * recovering connections that had suffered a network error or connections that were never
 * returned but dropped out of scope.
 * 
 * @author John Lewis, Unicon Inc [development for Sakai 2]
 * @author <a href="mailto:carl@hallwaytech.com">Carl Hall, Hallway Technologies [changes
 *         for OSGi]</a>
 * @see LdapConnectionManagerConfig
 * @see PooledLDAPConnection
 * @see PooledLDAPConnectionFactory
 */
@Component(policy = ConfigurationPolicy.IGNORE)
@Service
public class PoolingLdapConnectionManager extends SimpleLdapConnectionManager implements
    ManagedServiceFactory {

  private HashMap<String, PoolingLdapConnectionManager> managers = new HashMap<String, PoolingLdapConnectionManager>();

  public static final String PID = "org.sakaiproject.nakamura.ldap.LdapConnectionManager";
  @Property(value = PID)
  static final String SERVICE_PID = Constants.SERVICE_PID;

  public static final String FACTORY_PID = PID + ".factory";
  @Property(value = FACTORY_PID)
  static final String SERVICE_FACTORY_PID = "service.factoryPid";

  static final boolean DEFAULT_AUTO_BIND = false;
  @Property(boolValue = DEFAULT_AUTO_BIND)
  static final String AUTO_BIND = "sakai.ldap.autobind";

  static final boolean DEFAULT_FOLLOW_REFERRALS = false;
  @Property(boolValue = DEFAULT_FOLLOW_REFERRALS)
  static final String FOLLOW_REFERRALS = "sakai.ldap.referrals.follow";

  static final String DEFAULT_KEYSTORE_LOCATION = "";
  @Property(value = DEFAULT_KEYSTORE_LOCATION)
  static final String KEYSTORE_LOCATION = "sakai.ldap.keystore.location";

  static final String DEFAULT_KEYSTORE_PASSWORD = "";
  @Property(value = DEFAULT_KEYSTORE_PASSWORD)
  static final String KEYSTORE_PASSWORD = "sakai.ldap.keystore.password";

  static final String DEFAULT_HOST = "localhost";
  @Property(value = DEFAULT_HOST)
  static final String HOST = "sakai.ldap.host";

  static final int DEFAULT_PORT = LDAPConnection.DEFAULT_PORT;
  @Property(intValue = DEFAULT_PORT)
  static final String PORT = "sakai.ldap.port";

  static final String DEFAULT_USER = "";
  @Property(value = DEFAULT_USER)
  static final String USER = "sakai.ldap.user";

  static final String DEFAULT_PASSWORD = "";
  @Property(value = DEFAULT_PASSWORD)
  static final String PASSWORD = "sakai.ldap.password";

  static final boolean DEFAULT_SECURE_CONNECTION = false;
  @Property(boolValue = DEFAULT_SECURE_CONNECTION)
  static final String SECURE_CONNECTION = "sakai.ldap.connection.secure";

  static final int DEFAULT_OPERATION_TIMEOUT = 5000;
  @Property(intValue = DEFAULT_OPERATION_TIMEOUT)
  static final String OPERATION_TIMEOUT = "sakai.ldap.operation.timeout";

  static final boolean DEFAULT_POOLING = true;
  @Property(boolValue = DEFAULT_POOLING)
  static final String POOLING = "sakai.ldap.pooling";

  static final int DEFAULT_POOLING_MAX_CONNS = 10;
  @Property(intValue = DEFAULT_POOLING_MAX_CONNS)
  static final String POOLING_MAX_CONNS = "sakai.ldap.pooling.maxConns";

  static final boolean DEFAULT_TLS = false;
  @Property(boolValue = DEFAULT_TLS)
  static final String TLS = "sakai.ldap.tls";
  /** Class-specific logger */
  private static Logger log = LoggerFactory.getLogger(PoolingLdapConnectionManager.class);

  /** LDAP connection pool */
  private ObjectPool pool;

  private PooledLDAPConnectionFactory factory;

  /** How long to block waiting for an available connection before throwing an exception */
  private static final int POOL_MAX_WAIT = 60000;

  public PoolingLdapConnectionManager() {
    // default constructor in parent class
    super();
  }

  // ---------- SCR References
  @Reference(referenceInterface = LdapConnectionLivenessValidator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "bindLivenessValidator", unbind = "unbindLivenessValidator")
  private List<LdapConnectionLivenessValidator> livenessValidators = new LinkedList<LdapConnectionLivenessValidator>();

  protected void bindLivenessValidator(LdapConnectionLivenessValidator validator) {
    livenessValidators.add(validator);
  }

  protected void unbindLivenessValidator(LdapConnectionLivenessValidator validator) {
    livenessValidators.remove(validator);
  }

  public PoolingLdapConnectionManager newInstance(LdapConnectionManagerConfig config) {
    PoolingLdapConnectionManager mgr = new PoolingLdapConnectionManager();
    mgr.init(config);
    return mgr;
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
  public LDAPConnection getBoundConnection(String dn, String pass) throws LdapException {
    log.debug(
        "getBoundConnection():dn=[{}] attempting to borrow connection from pool and bind to dn",
        dn);
    LDAPConnection conn = null;
    try {
      conn = (LDAPConnection) pool.borrowObject();
      log.debug(
          "getBoundConnection():dn=[{}] successfully borrowed connection from pool", dn);
      conn.bind(LDAPConnection.LDAP_V3, dn, pass.getBytes("UTF8"));
      log.debug("getBoundConnection():dn=[{}] successfully bound to dn", dn);
      return conn;
    } catch (Exception e) {
      if (conn != null) {
        try {
          log.debug(
              "getBoundConnection():dn=[{}]; error occurred, returning connection to pool",
              dn);
          returnConnection(conn);
        } catch (Exception ee) {
          log.debug("getBoundConnection():dn=[" + dn
              + "] failed to return connection to pool", ee);
        }
      }
      if (e instanceof LDAPException) {
        throw new LdapException(e.getMessage(), e);
      } else {
        throw new RuntimeException("failed to get pooled connection", e);
      }
    }
  }

  /** {@inheritDoc} */
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

  // ---------- ManagedServiceFactory
  public String getName() {
    return getClass().getName();
  }

  public void deleted(String pid) {
    managers.remove(pid);
  }

  @SuppressWarnings("rawtypes")
  public void updated(String pid, Dictionary properties) throws ConfigurationException {
    if (managers.containsKey(pid)) {
      PoolingLdapConnectionManager mgr = managers.get(pid);
      mgr.init(config);
    } else {
      PoolingLdapConnectionManager mgr = new PoolingLdapConnectionManager();
      mgr.init(config);
      managers.put(pid, mgr);
    }
  }

  // ---------- SCR integration
  /**
   * Activate/initialize the instance. Normally called by OSGi.
   * 
   * @param ctx
   */
  @Activate
  protected void activate(ComponentContext ctx) {
    Dictionary<?, ?> properties = ctx.getProperties();

    // set the default configuration
    LdapConnectionManagerConfig config = createConfig(properties);
    init(config);
  }

  @Deactivate
  protected void deactivate(ComponentContext context) {
    try {
      log.debug("deactivate(): closing connection pool");
      pool.close();
      log.debug("destroy(): successfully closed connection pool");
    } catch (Exception e) {
      throw new RuntimeException("failed to shutdown connection pool", e);
    } finally {
      pool = null;
      factory = null;
    }
    log.debug("destroy(): delegating to parent destroy() impl");
  }

  @Modified
  protected void update(Dictionary<?, ?> dict) {
    LdapConnectionManagerConfig config = createConfig(dict);
    init(config);
  }

  public void init(LdapConnectionManagerConfig config) {
    super.init(config);

    if (pool != null) {
      try {
        pool.close();
      } catch (Exception e) {
        // ignore
      }
      pool = null;
    }

   factory = newPooledLDAPConnectionFactory(this, livenessValidators);
    
    pool = newConnectionPool(factory, getConfig().getPoolMaxConns(), // maxActive
        GenericObjectPool.WHEN_EXHAUSTED_BLOCK, // whenExhaustedAction
        POOL_MAX_WAIT, // maxWait (millis)
        getConfig().getPoolMaxConns(), // maxIdle
        true, // testOnBorrow
        false // testOnReturn
      );
  }
  
  PooledLDAPConnectionFactory newPooledLDAPConnectionFactory(
      PoolingLdapConnectionManager manager,
      List<LdapConnectionLivenessValidator> livenessValidators) {
    return new PooledLDAPConnectionFactory(manager, livenessValidators);
  }
  
  ObjectPool newConnectionPool(PoolableObjectFactory factory, int maxConns,
      byte whenExhausted, int maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn) {
    GenericObjectPool pool = new GenericObjectPool(factory,
        maxConns, // maxActive
        whenExhausted, // whenExhaustedAction
        maxWait, // maxWait (millis)
        maxIdle, // maxIdle
        testOnBorrow, // testOnBorrow
        testOnReturn // testOnReturn
    );
    return pool;
  }
  
  LdapConnectionManagerConfig createConfig(Dictionary<?, ?> dict) {
    if (dict == null)
      dict = new Hashtable<String, String>();

    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();

    config.setAutoBind(OsgiUtil.toBoolean(dict.get(AUTO_BIND), DEFAULT_AUTO_BIND));
    config.setFollowReferrals(OsgiUtil.toBoolean(dict.get(FOLLOW_REFERRALS),
        DEFAULT_FOLLOW_REFERRALS));
    config.setKeystoreLocation(OsgiUtil.toString(dict.get(KEYSTORE_LOCATION),
        DEFAULT_KEYSTORE_LOCATION));
    config.setKeystorePassword(OsgiUtil.toString(dict.get(KEYSTORE_PASSWORD),
        DEFAULT_KEYSTORE_PASSWORD));
    config.setSecureConnection(OsgiUtil.toBoolean(dict.get(SECURE_CONNECTION),
        DEFAULT_SECURE_CONNECTION));
    config.setLdapHost(OsgiUtil.toString(dict.get(HOST), DEFAULT_HOST));
    config.setLdapPort(OsgiUtil.toInteger(dict.get(PORT), DEFAULT_PORT));
    config.setLdapUser(OsgiUtil.toString(dict.get(USER), DEFAULT_USER));
    config.setLdapPassword(OsgiUtil.toString(dict.get(PASSWORD), DEFAULT_PASSWORD));
    config.setOperationTimeout(OsgiUtil.toInteger(dict.get(OPERATION_TIMEOUT),
        DEFAULT_OPERATION_TIMEOUT));
    config.setPooling(OsgiUtil.toBoolean(dict.get(POOLING), DEFAULT_POOLING));
    config.setPoolMaxConns(OsgiUtil.toInteger(dict.get(POOLING_MAX_CONNS),
        DEFAULT_POOLING_MAX_CONNS));
    config.setTLS(OsgiUtil.toBoolean(dict.get(TLS), DEFAULT_TLS));

    return config;
  }
}
