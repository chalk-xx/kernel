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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.ldap.LdapConnectionBroker;
import org.sakaiproject.kernel.api.ldap.LdapConnectionManager;
import org.sakaiproject.kernel.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.kernel.api.ldap.LdapConstants;
import org.sakaiproject.kernel.api.ldap.LdapException;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Simple implementation of an {@link LdapConnectionBroker}. Maintains an
 * associative pairing of connection factories and names. As connections are
 * requested by name, the associated factory is used to create a pooled
 * connection.
 */
@Component
@Service
public class PoolingLdapConnectionBroker implements LdapConnectionBroker {
  private Hashtable<String, LdapConnectionManager> factories;
  private LdapConnectionManagerConfig defaults;

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

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.api.ldap.LdapConnectionBroker#create(java.lang.String)
   */
  public void create(String name) throws LdapException {
    create(name, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.api.ldap.LdapConnectionBroker#create(java.lang.String,
   *      org.sakaiproject.kernel.api.ldap.LdapConnectionManagerConfig)
   */
  public LdapConnectionManager create(String name, LdapConnectionManagerConfig config)
      throws LdapException {
    if (config == null) {
      config = defaults;
    }

    // create a new connection manager, set the config and initialize it.
    PoolingLdapConnectionManager mgr = newPoolingLdapConnectionManager(name);
    mgr.setConfig(config);
    mgr.init();

    // put the new connection manager in the store and set it to be
    // available outside of this block.
    factories.put(name, mgr);

    return mgr;
  }

  protected PoolingLdapConnectionManager newPoolingLdapConnectionManager(String poolName) {
    return new PoolingLdapConnectionManager(this, poolName);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.api.ldap.LdapConnectionBroker#destroy(java.lang.String)
   */
  public void destroy(String name) {
    if (factories.containsKey(name)) {
      LdapConnectionManager mgr = factories.get(name);
      mgr.destroy();
      factories.remove(name);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.api.ldap.LdapConnectionBroker#exists(java.lang.String)
   */
  public boolean exists(String name) {
    boolean exists = factories.containsKey(name);
    return exists;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.api.ldap.LdapConnectionBroker#getConnection(java.lang.String)
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
   * @see org.sakaiproject.kernel.api.ldap.LdapConnectionBroker#getConnection(java.lang.String,
   *      org.sakaiproject.kernel.api.ldap.LdapConnectionManagerConfig)
   */
  public LDAPConnection getBoundConnection(String name, String loginDn, String password)
      throws LdapException {
    // get a connection manager from the local store. if not found, create a
    // new one and store it locally for reuse.
    if (factories.containsKey(name)) {
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
    return defaults.copy();
  }

  public void update(Dictionary props) {
    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();
    if (props != null && !props.isEmpty()) {
      String autoBind = (String) props.get(LdapConstants.AUTO_BIND);
      String followReferrals = (String) props.get(LdapConstants.FOLLOW_REFERRALS);
      String keystoreLocation = (String) props.get(LdapConstants.KEYSTORE_LOCATION);
      String keystorePassword = (String) props.get(LdapConstants.KEYSTORE_PASSWORD);
      String secureConnection = (String) props.get(LdapConstants.SECURE_CONNECTION);
      String host = (String) props.get(LdapConstants.HOST);
      String port = (String) props.get(LdapConstants.PORT);
      String user = (String) props.get(LdapConstants.USER);
      String password = (String) props.get(LdapConstants.PASSWORD);
      String operationTimeout = (String) props.get(LdapConstants.OPERATION_TIMEOUT);
      String pooling = (String) props.get(LdapConstants.POOLING);
      String maxConns = (String) props.get(LdapConstants.POOLING_MAX_CONNS);
      String tls = (String) props.get(LdapConstants.TLS);

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
