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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;

import com.novell.ldap.LDAPConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.configuration.ConfigurationService;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.nakamura.api.ldap.LdapException;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Properties;

/**
 * Unit test for {@link PoolingLdapConnectionBroker}
 */
public class PoolingLdapConnectionBrokerTest {
  private LdapConnectionManagerConfig config;
  private PoolingLdapConnectionBroker broker;
  private String name = "brokerTest";
  private ServerSocket serverSocket;

  @Before
  public void setUp() throws Exception {
    ConfigurationService configService = createMock(ConfigurationService.class);
    expect(configService.getProperties()).andReturn(new HashMap<String, String>());

    // don't mock this. we just need to inject the connection.
    final PoolingLdapConnectionManager mgr = new PoolingLdapConnectionManager() {
      @Override
      public LDAPConnection getConnection() {
        return new LDAPConnection();
      }

      @Override
      public LDAPConnection getBoundConnection() {
        return new LDAPConnection();
      }
    };
    config = new LdapConnectionManagerConfig();
    config.setLdapHost("localhost");
    config.setLdapPort(LDAPConnection.DEFAULT_PORT + 1000);
    config.setLdapUser("dude");
    config.setLdapPassword("sweet");

    mgr.setConfig(config);
    mgr.init();

    // don't mock this. we just need to inject the manager.
    broker = new PoolingLdapConnectionBroker() {
      @Override
      protected PoolingLdapConnectionManager newPoolingLdapConnectionManager(String poolName,
          LdapConnectionManagerConfig config) {
        return mgr;
      }
    };

    Properties props = new Properties();
    ComponentContext ctx = createMock(ComponentContext.class);
    expect(ctx.getProperties()).andReturn(props);
    replay(ctx);
    broker.activate(ctx);

    // Provide a port for the connection to attach to. Does not do anything LDAP
    // specific.
    serverSocket = new ServerSocket(LDAPConnection.DEFAULT_PORT + 1000);
  }

  @After
  public void tearDown() throws Exception {
    broker.deactivate(null);
    serverSocket.close();
  }

  @Test
  public void testCreateDefault() throws Exception {
    broker.create(name);
  }

  @Test
  public void testCreateCustom() throws Exception {
    broker.create(name, config);
  }

  @Test
  public void testCreateGetDestroy() throws Exception {
    broker.create(name, config);
    LDAPConnection conn = broker.getConnection(name);
    assertNotNull(conn);

    broker.destroy(name);
  }

  @Test
  public void testCreateIfNotExists() throws Exception {
    broker.create(name);

    if (!broker.exists(name)) {
      fail("Broker should already know about [" + name + "]");
    }
  }

  @Test
  public void testDefaults() throws Exception {
    LdapConnectionManagerConfig defaultConfig = broker.getDefaultConfig();
    assertNotNull(defaultConfig);
    assertFalse(defaultConfig.isAutoBind());
  }

  @Test
  public void testEmptyUpdate() throws Exception {
    LdapConnectionManagerConfig realDefaults = new LdapConnectionManagerConfig();
    Properties m = new Properties();
    broker.update(m);

    LdapConnectionManagerConfig defaults = broker.getDefaultConfig();
    assertEquals(realDefaults.isAutoBind(), defaults.isAutoBind());
    assertEquals(realDefaults.isFollowReferrals(), defaults.isFollowReferrals());
    assertEquals(realDefaults.isPooling(), defaults.isPooling());
    assertEquals(realDefaults.isSecureConnection(), defaults.isSecureConnection());
    assertEquals(realDefaults.isTLS(), defaults.isTLS());
    assertEquals(realDefaults.getKeystoreLocation(), defaults.getKeystoreLocation());
    assertEquals(realDefaults.getKeystorePassword(), defaults.getKeystorePassword());
    assertEquals(realDefaults.getLdapHost(), defaults.getLdapHost());
    assertEquals(realDefaults.getLdapPassword(), realDefaults.getLdapPassword());
    assertEquals(realDefaults.getLdapPort(), defaults.getLdapPort());
    assertEquals(realDefaults.getLdapUser(), defaults.getLdapUser());
    assertEquals(realDefaults.getOperationTimeout(), defaults.getOperationTimeout());
    assertEquals(realDefaults.getPoolMaxConns(), defaults.getPoolMaxConns());
  }

  @Test
  public void testUpdate() throws Exception {
    Boolean t = Boolean.TRUE;
    Boolean f = Boolean.FALSE;
    Properties m = new Properties();
    m.put(PoolingLdapConnectionBroker.AUTO_BIND, t);
    m.put(PoolingLdapConnectionBroker.FOLLOW_REFERRALS, t);
    m.put(PoolingLdapConnectionBroker.HOST, "localhoster");
    m.put(PoolingLdapConnectionBroker.KEYSTORE_LOCATION, "over there");
    m.put(PoolingLdapConnectionBroker.KEYSTORE_PASSWORD, "open sesame");
    m.put(PoolingLdapConnectionBroker.OPERATION_TIMEOUT, Integer.MIN_VALUE);
    m.put(PoolingLdapConnectionBroker.PASSWORD, "secret");
    m.put(PoolingLdapConnectionBroker.POOLING, f);
    m.put(PoolingLdapConnectionBroker.POOLING_MAX_CONNS, Integer.MAX_VALUE);
    m.put(PoolingLdapConnectionBroker.PORT, LDAPConnection.DEFAULT_SSL_PORT);
    m.put(PoolingLdapConnectionBroker.SECURE_CONNECTION, f);
    m.put(PoolingLdapConnectionBroker.TLS, t);
    m.put(PoolingLdapConnectionBroker.USER, "user1");
    broker.update(m);

    LdapConnectionManagerConfig defaults = broker.getDefaultConfig();
    assertEquals(((Boolean) m.get(PoolingLdapConnectionBroker.AUTO_BIND)).booleanValue(), defaults
        .isAutoBind());
    assertEquals(((Boolean) m.get(PoolingLdapConnectionBroker.FOLLOW_REFERRALS)).booleanValue(),
        defaults.isFollowReferrals());
    assertEquals(m.getProperty(PoolingLdapConnectionBroker.HOST), defaults.getLdapHost());
    assertEquals(m.getProperty(PoolingLdapConnectionBroker.KEYSTORE_LOCATION), defaults
        .getKeystoreLocation());
    assertEquals(m.getProperty(PoolingLdapConnectionBroker.KEYSTORE_PASSWORD), defaults
        .getKeystorePassword());
    assertEquals(((Integer) m.get(PoolingLdapConnectionBroker.OPERATION_TIMEOUT))
        .intValue(), defaults.getOperationTimeout());
    assertEquals(m.getProperty(PoolingLdapConnectionBroker.PASSWORD), defaults.getLdapPassword());
    assertEquals(((Boolean) m.get(PoolingLdapConnectionBroker.POOLING)).booleanValue(), defaults
        .isPooling());
    assertEquals(((Integer) m.get(PoolingLdapConnectionBroker.POOLING_MAX_CONNS)).intValue(),
        defaults.getPoolMaxConns());
    assertEquals(((Integer) m.get(PoolingLdapConnectionBroker.PORT)).intValue(), defaults
        .getLdapPort());
    assertEquals(((Boolean) m.get(PoolingLdapConnectionBroker.SECURE_CONNECTION)).booleanValue(),
        defaults.isSecureConnection());
    assertEquals(((Boolean) m.get(PoolingLdapConnectionBroker.TLS)).booleanValue(), defaults
        .isTLS());
    assertEquals(m.getProperty(PoolingLdapConnectionBroker.USER), defaults.getLdapUser());
  }

  @Test
  public void testGetConnectionBeforeCreate() throws Exception {
    LDAPConnection conn = broker.getConnection("whatever");
    assertNotNull(conn);
    boolean exists = broker.exists("whatever");
    assertTrue(exists);
  }

  @Test
  public void testGetBoundConnectionBeforeCreate() throws Exception {
    LDAPConnection conn = broker.getBoundConnection("whatever");
    assertNotNull(conn);
    boolean exists = broker.exists("whatever");
    assertTrue(exists);
  }

  @Test
  public void testGetConnection() throws LdapException {
    String name = "whatever";
    broker.create(name);
    LDAPConnection conn = broker.getConnection(name);
    assertNotNull(conn);
  }

  @Test
  public void testGetBoundConnection() throws LdapException {
    String name = "whatever";
    broker.create(name);
    LDAPConnection conn = broker.getBoundConnection(name);
    assertNotNull(conn);
  }
}
