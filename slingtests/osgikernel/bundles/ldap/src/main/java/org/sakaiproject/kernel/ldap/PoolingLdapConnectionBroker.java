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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.configuration.ConfigurationListener;
import org.sakaiproject.kernel.api.configuration.ConfigurationService;
import org.sakaiproject.kernel.ldap.api.LdapConnectionBroker;
import org.sakaiproject.kernel.ldap.api.LdapConnectionManager;
import org.sakaiproject.kernel.ldap.api.LdapConnectionManagerConfig;
import org.sakaiproject.kernel.ldap.api.LdapException;

import java.util.Hashtable;
import java.util.Map;

/**
 * Simple implementation of an {@link LdapConnectionBroker}. Maintains an
 * associative pairing of connection factories and names. As connections are
 * requested by name, the associated factory is used to create a pooled
 * connection.
 */
@Component
@Service
public class PoolingLdapConnectionBroker implements LdapConnectionBroker, ConfigurationListener {
  private Hashtable<String, LdapConnectionManager> factories;
  private LdapConnectionManagerConfig defaults;

  @Reference
  protected ConfigurationService configService;

  static final String AUTO_BIND = "sakai.ldap.autobind";
  static final String FOLLOW_REFERRALS = "sakai.ldap.referrals.follow";
  static final String KEYSTORE_LOCATION = "sakai.ldap.keystore.location";
  static final String KEYSTORE_PASSWORD = "sakai.ldap.keystore.password";
  static final String HOST = "sakai.ldap.host";
  static final String PORT = "sakai.ldap.port";
  static final String USER = "sakai.ldap.user";
  static final String PASSWORD = "sakai.ldap.password";
  static final String SECURE_CONNECTION = "sakai.ldap.connection.secure";
  static final String OPERATION_TIMEOUT = "sakai.ldap.operation.timeout";
  static final String POOLING = "sakai.ldap.pooling";
  static final String POOLING_MAX_CONNS = "sakai.ldap.pooling.maxConns";
  static final String TLS = "sakai.ldap.tls";

  /**
   * Default constructor for normal usage.
   */
  public PoolingLdapConnectionBroker() {

  }

  /**
   * Parameterized constructor for use in testing.
   *
   * @param configService
   */
  protected PoolingLdapConnectionBroker(ConfigurationService configService) {
    this.configService = configService;
  }

  /**
   * Activate/initialize the instance. Normally called by OSGi.
   *
   * @param ctx
   */
  protected void activate(ComponentContext ctx) {
    factories = new Hashtable<String, LdapConnectionManager>();

    // Do we want to listen for changes from the central config service?
    // configService.addListener(this);

    // read up properties for defaults
    Map<String, String> config = configService.getProperties();
    update(config);
  }

  /**
   * Deactivate/finalize the instance. Normally called by OSGi.
   *
   * @param ctx
   */
  protected void deactivate(ComponentContext ctx) {
    for (LdapConnectionManager conns : factories.values()) {
      conns.destroy();
    }
    factories = null;

    defaults = null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.ldap.api.LdapConnectionBroker#create(java.lang.String)
   */
  public void create(String name) throws LdapException {
    create(name, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.ldap.api.LdapConnectionBroker#create(java.lang.String,
   *      org.sakaiproject.kernel.ldap.api.LdapConnectionManagerConfig)
   */
  public void create(String name, LdapConnectionManagerConfig config) throws LdapException {
    if (config == null) {
      config = defaults;
    }

    // create a new connection manager, set the config and initialize it.
    PoolingLdapConnectionManager mgr = new PoolingLdapConnectionManager();
    mgr.setConfig(config);
    mgr.init();

    // put the new connection manager in the store and set it to be
    // available outside of this block.
    factories.put(name, mgr);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.ldap.api.LdapConnectionBroker#destroy(java.lang.String)
   */
  public void destroy(String name) {
    if (factories.contains(name)) {
      LdapConnectionManager mgr = factories.get(name);
      mgr.destroy();
      factories.remove(name);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.ldap.api.LdapConnectionBroker#exists(java.lang.String)
   */
  public boolean exists(String name) {
    boolean exists = factories.contains(name);
    return exists;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.ldap.api.LdapConnectionBroker#getConnection(java.lang.String)
   */
  public LDAPConnection getConnection(String name) throws LdapException {
    // get a connection manager from the local store. if not found, create a
    // new one and store it locally for reuse.
    if (factories.containsKey(name)) {
      LdapConnectionManager mgr = factories.get(name);

      // get a connection from the manager and return it
      LDAPConnection conn = mgr.getConnection();
      return conn;
    } else {
      throw new LdapException("No factory found for [" + name
          + "].  Be sure to call create(String) before calling getConnection(String).");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.ldap.api.LdapConnectionBroker#getConnection(java.lang.String,
   *      org.sakaiproject.kernel.ldap.api.LdapConnectionManagerConfig)
   */
  public LDAPConnection getBoundConnection(String name, String loginDn, String password)
      throws LdapException {
    // get a connection manager from the local store. if not found, create a
    // new one and store it locally for reuse.
    if (factories.contains(name)) {
      LdapConnectionManager mgr = factories.get(name);

      // get a connection from the manager and return it
      LDAPConnection conn = mgr.getBoundConnection(loginDn, password);
      return conn;
    } else {
      throw new LdapException("No factory found for [" + name
          + "].  Be sure to call create(String) before calling getBoundConnection(String, char[]).");
    }
  }

  public LdapConnectionManagerConfig getDefaultConfig() {
    return defaults;
  }

  public void update(Map<String, String> props) {
    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();
    if (props != null) {
      String autoBind = props.get(AUTO_BIND);
      String followReferrals = props.get(FOLLOW_REFERRALS);
      String keystoreLocation = props.get(KEYSTORE_LOCATION);
      String keystorePassword = props.get(KEYSTORE_PASSWORD);
      String secureConnection = props.get(SECURE_CONNECTION);
      String host = props.get(HOST);
      String port = props.get(PORT);
      String user = props.get(USER);
      String password = props.get(PASSWORD);
      String operationTimeout = props.get(OPERATION_TIMEOUT);
      String pooling = props.get(POOLING);
      String maxConns = props.get(POOLING_MAX_CONNS);
      String tls = props.get(TLS);

      if (autoBind != null) {
        config.setAutoBind(Boolean.parseBoolean(autoBind));
      }
      if (followReferrals != null) {
        config.setFollowReferrals(Boolean.parseBoolean(followReferrals));
      }
      config.setKeystoreLocation(keystoreLocation);
      config.setKeystorePassword(keystorePassword);
      config.setLdapHost(host);
      config.setLdapPassword(password);
      if (port != null) {
        config.setLdapPort(Integer.parseInt(port));
      }
      config.setLdapUser(user);
      if (operationTimeout != null) {
        config.setOperationTimeout(Integer.parseInt(operationTimeout));
      }
      if (pooling != null) {
        config.setPooling(Boolean.parseBoolean(pooling));
      }
      if (maxConns != null) {
        config.setPoolMaxConns(Integer.parseInt(maxConns));
      }
      if (secureConnection != null) {
        config.setSecureConnection(Boolean.parseBoolean(secureConnection));
      }
      if (tls != null) {
        config.setTLS(Boolean.parseBoolean(tls));
      }
    }

    // set the default configuration
    defaults = config;
  }
}
