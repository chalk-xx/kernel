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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionBroker;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionLivenessValidator;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.nakamura.api.ldap.LdapException;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

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
  private LdapConnectionManagerConfig defaults;

  @Property(boolValue = false)
  protected static final String AUTO_BIND = "sakai.ldap.autobind";

  @Property(boolValue = false)
  protected static final String FOLLOW_REFERRALS = "sakai.ldap.referrals.follow";

  @Property
  protected static final String KEYSTORE_LOCATION = "sakai.ldap.keystore.location";

  @Property
  protected static final String KEYSTORE_PASSWORD = "sakai.ldap.keystore.password";

  @Property
  protected static final String HOST = "sakai.ldap.host";

  @Property(intValue = LDAPConnection.DEFAULT_PORT)
  protected static final String PORT = "sakai.ldap.port";

  @Property
  protected static final String USER = "sakai.ldap.user";

  @Property
  protected static final String PASSWORD = "sakai.ldap.password";

  @Property(boolValue = false)
  protected static final String SECURE_CONNECTION = "sakai.ldap.connection.secure";

  @Property(intValue = 5000)
  protected static final String OPERATION_TIMEOUT = "sakai.ldap.operation.timeout";

  @Property(boolValue = true)
  protected static final String POOLING = "sakai.ldap.pooling";

  @Property(intValue = 10)
  protected static final String POOLING_MAX_CONNS = "sakai.ldap.pooling.maxConns";

  @Property(boolValue = false)
  protected static final String TLS = "sakai.ldap.tls";

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
    defaults = null;
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
    LdapConnectionManager mgr = create(name, defaults);
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
    PoolingLdapConnectionManager mgr = new PoolingLdapConnectionManager(this, poolName);
    mgr.setConfig(config);
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
   * @see org.sakaiproject.nakamura.api.ldap.LdapConnectionBroker#getBoundConnection(String)
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
    return defaults.copy();
  }

  @SuppressWarnings("unchecked")
  public void update(Dictionary props) {
    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();
    if (props != null && !props.isEmpty()) {
      Boolean autoBind = (Boolean) props.get(AUTO_BIND);
      Boolean followReferrals = (Boolean) props.get(FOLLOW_REFERRALS);
      String keystoreLocation = (String) props.get(KEYSTORE_LOCATION);
      String keystorePassword = (String) props.get(KEYSTORE_PASSWORD);
      Boolean secureConnection = (Boolean) props.get(SECURE_CONNECTION);
      String host = (String) props.get(HOST);
      Integer port = (Integer) props.get(PORT);
      String user = (String) props.get(USER);
      String password = (String) props.get(PASSWORD);
      Integer operationTimeout = (Integer) props.get(OPERATION_TIMEOUT);
      Boolean pooling = (Boolean) props.get(POOLING);
      Integer maxConns = (Integer) props.get(POOLING_MAX_CONNS);
      Boolean tls = (Boolean) props.get(TLS);

      if (autoBind != null) {
        config.setAutoBind(autoBind);
      }
      if (followReferrals != null) {
        config.setFollowReferrals(followReferrals);
      }
      config.setKeystoreLocation(keystoreLocation);
      config.setKeystorePassword(keystorePassword);
      config.setLdapHost(host);
      config.setLdapPassword(password);
      if (port != null) {
        config.setLdapPort(port);
      }
      config.setLdapUser(user);
      if (operationTimeout != null) {
        config.setOperationTimeout(operationTimeout);
      }
      if (pooling != null) {
        config.setPooling(pooling);
      }
      if (maxConns != null) {
        config.setPoolMaxConns(maxConns);
      }
      if (secureConnection != null) {
        config.setSecureConnection(secureConnection);
      }
      if (tls != null) {
        config.setTLS(tls);
      }
    }

    // set the default configuration
    defaults = config;
  }
}
