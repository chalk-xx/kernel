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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionBroker;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionLivenessValidator;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.nakamura.api.ldap.LdapException;

/**
 * Simple implementation of an {@link LdapConnectionBroker}. Maintains an
 * associative pairing of connection factories and names. As connections are
 * requested by name, the associated factory is used to create a pooled
 * connection.
 */
@Component(metatype = true)
@Service
public class PoolingLdapConnectionBroker implements LdapConnectionBroker {
  private Hashtable<String, LdapConnectionManager> factories;
  private LdapConnectionManagerConfig defaultConfig;

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

  @Reference(referenceInterface = LdapConnectionLivenessValidator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, bind = "bindLivenessValidator", unbind = "unbindLivenessValidator")
  private List<LdapConnectionLivenessValidator> livenessValidators = new LinkedList<LdapConnectionLivenessValidator>();

  protected void bindLivenessValidator(LdapConnectionLivenessValidator validator) {
    livenessValidators.add(validator);
  }

  protected void unbindLivenessValidator(LdapConnectionLivenessValidator validator) {
    livenessValidators.remove(validator);
  }

  /**
   * Default constructor for normal usage.
   */
  public PoolingLdapConnectionBroker() {
    factories = new Hashtable<String, LdapConnectionManager>();
  }

  /**
   * Activate/initialize the instance. Normally called by OSGi.
   *
   * @param ctx
   */
  @SuppressWarnings("unchecked")
  @Activate
  protected void activate(ComponentContext ctx) {
    Dictionary properties = ctx.getProperties();
    update(properties);
  }

  /**
   * Deactivate/finalize the instance. Normally called by OSGi.
   *
   * @param ctx
   */
  @Deactivate
  protected void deactivate(ComponentContext ctx) {
    for (String mgr : factories.keySet()) {
      destroy(mgr);
    }
    factories = null;
    defaultConfig = null;
  }

  public List<LdapConnectionLivenessValidator> getLivenessValidators() {
    return livenessValidators;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.ldap.LdapConnectionBroker#create(java.lang.String)
   */
  public LdapConnectionManager create(String name) throws LdapException {
    LdapConnectionManager mgr = create(name, defaultConfig);
    return mgr;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.ldap.LdapConnectionBroker#create(java.lang.String,
   *      org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig)
   */
  public LdapConnectionManager create(String name, LdapConnectionManagerConfig config)
      throws LdapException {
    if (config == null) {
      throw new IllegalArgumentException(
          "A configuration must be provided. To use the default config, use create(String).");
    }

    // create a new connection manager, set the config and initialize it.
    PoolingLdapConnectionManager mgr = newPoolingLdapConnectionManager(name, config);

    // put the new connection manager in the store and set it to be
    // available outside of this block.
    factories.put(name, mgr);

    return mgr;
  }

  protected PoolingLdapConnectionManager newPoolingLdapConnectionManager(String poolName,
      LdapConnectionManagerConfig config) throws LdapException {
    PoolingLdapConnectionManager mgr = new PoolingLdapConnectionManager(config, this);
    mgr.init();
    return mgr;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.ldap.LdapConnectionBroker#destroy(java.lang.String)
   */
  public void destroy(String name) {
    if (factories.containsKey(name)) {
      LdapConnectionManager mgr = factories.remove(name);
      mgr.destroy();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.ldap.LdapConnectionBroker#exists(java.lang.String)
   */
  public boolean exists(String name) {
    boolean exists = factories.containsKey(name);
    return exists;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.ldap.LdapConnectionBroker#getConnection(java.lang.String)
   */
  public LDAPConnection getConnection(String name) throws LdapException {
    // get a connection manager from the local store. if not found, create a
    // new one and store it locally for reuse.
    LdapConnectionManager mgr = null;
    if (!factories.containsKey(name)) {
      mgr = create(name);
    } else {
      mgr = factories.get(name);
    }

    // get a connection from the manager and return it
    LDAPConnection conn = mgr.getConnection();
    return conn;
    // } else {
    // throw new LdapException("No factory found for [" + name +
    // "].  Be sure to call create(String) before calling getConnection(String).");
    // }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.ldap.LdapConnectionManager#getBoundConnection(String, String)
   */
  public LDAPConnection getBoundConnection(String name, String dn, String password)
      throws LdapException {
    // get a connection manager from the local store. if not found, create a
    // new one and store it locally for reuse.
    LdapConnectionManager mgr = null;
    if (!factories.containsKey(name)) {
      mgr = create(name);
    } else {
      mgr = factories.get(name);
    }

    // get a connection from the manager and return it
    LDAPConnection conn = mgr.getBoundConnection(dn, password);
    return conn;
    // } else {
    // throw new LdapException("No factory found for [" + name +
    // "].  Be sure to call create(String) before calling getBoundConnection(String, char[]).");
    // }
  }

  public LdapConnectionManagerConfig getDefaultConfig() {
    return defaultConfig.copy();
  }

  @SuppressWarnings("unchecked")
  public void update(Dictionary props) {
    if (props == null) props = new Properties();

    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();

    config.setAutoBind(OsgiUtil.toBoolean(props.get(AUTO_BIND), DEFAULT_AUTO_BIND));
    config.setFollowReferrals(OsgiUtil.toBoolean(props.get(FOLLOW_REFERRALS), DEFAULT_FOLLOW_REFERRALS));
    config.setKeystoreLocation(OsgiUtil.toString(props.get(KEYSTORE_LOCATION), DEFAULT_KEYSTORE_LOCATION));
    config.setKeystorePassword(OsgiUtil.toString(props.get(KEYSTORE_PASSWORD), DEFAULT_KEYSTORE_PASSWORD));
    config.setSecureConnection(OsgiUtil.toBoolean(props.get(SECURE_CONNECTION), DEFAULT_SECURE_CONNECTION));
    config.setLdapHost(OsgiUtil.toString(props.get(HOST), DEFAULT_HOST));
    config.setLdapPort(OsgiUtil.toInteger(props.get(PORT), DEFAULT_PORT));
    config.setLdapUser(OsgiUtil.toString(props.get(USER), DEFAULT_USER));
    config.setLdapPassword(OsgiUtil.toString(props.get(PASSWORD), DEFAULT_PASSWORD));
    config.setOperationTimeout(OsgiUtil.toInteger(props.get(OPERATION_TIMEOUT), DEFAULT_OPERATION_TIMEOUT));
    config.setPooling(OsgiUtil.toBoolean(props.get(POOLING), DEFAULT_POOLING));
    config.setPoolMaxConns(OsgiUtil.toInteger(props.get(POOLING_MAX_CONNS), DEFAULT_POOLING_MAX_CONNS));
    config.setTLS(OsgiUtil.toBoolean(props.get(TLS), DEFAULT_TLS));

    // set the default configuration
    defaultConfig = config;
  }
}
