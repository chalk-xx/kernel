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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;

import com.novell.ldap.LDAPConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.api.configuration.ConfigurationService;
import org.sakaiproject.kernel.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.kernel.api.ldap.LdapConstants;
import org.sakaiproject.kernel.api.ldap.LdapException;

import java.net.ServerSocket;
import java.util.HashMap;

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

    final PoolingLdapConnectionManager mgr = new PoolingLdapConnectionManager() {
      @Override
      public LDAPConnection getConnection() {
        return new LDAPConnection();
      }

      @Override
      public LDAPConnection getBoundConnection(String user, String pass) {
        return new LDAPConnection();
      }
    };
    config = new LdapConnectionManagerConfig();
    config.setLdapHost("localhost");
    config.setLdapPort(LDAPConnection.DEFAULT_PORT + 1000);

    broker = new PoolingLdapConnectionBroker(configService) {
      @Override
      protected PoolingLdapConnectionManager newPoolingLdapConnectionManager() {
        return mgr;
      }
    };
    broker.activate(null);

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
    HashMap<String, String> m = new HashMap<String, String>();
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
    String t = Boolean.TRUE.toString();
    String f = Boolean.FALSE.toString();
    HashMap<String, String> m = new HashMap<String, String>();
    m.put(LdapConstants.AUTO_BIND, t);
    m.put(LdapConstants.FOLLOW_REFERRALS, t);
    m.put(LdapConstants.HOST, "localhoster");
    m.put(LdapConstants.KEYSTORE_LOCATION, "over there");
    m.put(LdapConstants.KEYSTORE_PASSWORD, "open sesame");
    m.put(LdapConstants.OPERATION_TIMEOUT, Integer.toString(Integer.MIN_VALUE));
    m.put(LdapConstants.PASSWORD, "secret");
    m.put(LdapConstants.POOLING, f);
    m.put(LdapConstants.POOLING_MAX_CONNS, Integer.toString(Integer.MAX_VALUE));
    m.put(LdapConstants.PORT, Integer.toString(LDAPConnection.DEFAULT_SSL_PORT));
    m.put(LdapConstants.SECURE_CONNECTION, f);
    m.put(LdapConstants.TLS, t);
    m.put(LdapConstants.USER, "user1");
    broker.update(m);

    LdapConnectionManagerConfig defaults = broker.getDefaultConfig();
    assertEquals(Boolean.parseBoolean(m.get(LdapConstants.AUTO_BIND)), defaults
        .isAutoBind());
    assertEquals(Boolean.parseBoolean(m.get(LdapConstants.FOLLOW_REFERRALS)),
        defaults.isFollowReferrals());
    assertEquals(m.get(LdapConstants.HOST), defaults.getLdapHost());
    assertEquals(m.get(LdapConstants.KEYSTORE_LOCATION), defaults
        .getKeystoreLocation());
    assertEquals(m.get(LdapConstants.KEYSTORE_PASSWORD), defaults
        .getKeystorePassword());
    assertEquals(Integer.parseInt(m.get(LdapConstants.OPERATION_TIMEOUT)), defaults
        .getOperationTimeout());
    assertEquals(m.get(LdapConstants.PASSWORD), defaults.getLdapPassword());
    assertEquals(Boolean.parseBoolean(m.get(LdapConstants.POOLING)), defaults
        .isPooling());
    assertEquals(Integer.parseInt(m.get(LdapConstants.POOLING_MAX_CONNS)), defaults
        .getPoolMaxConns());
    assertEquals(Integer.parseInt(m.get(LdapConstants.PORT)), defaults
        .getLdapPort());
    assertEquals(Boolean.parseBoolean(m.get(LdapConstants.SECURE_CONNECTION)),
        defaults.isSecureConnection());
    assertEquals(Boolean.parseBoolean(m.get(LdapConstants.TLS)), defaults.isTLS());
    assertEquals(m.get(LdapConstants.USER), defaults.getLdapUser());
  }

  @Test
  public void testGetConnectionBeforeCreate() {
    try {
      broker.getConnection("whatever");
      fail("Should've thrown an exception since the manager wasn't created.");
    } catch (LdapException e) {
      // expected
    }
  }

  @Test
  public void testGetBoundConnectionBeforeCreate() {
    try {
      broker.getBoundConnection("whatever", "dude", "sweet");
      fail("Should've thrown an exception since the manager wasn't created.");
    } catch (LdapException e) {
      // expected
    }
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
    LDAPConnection conn = broker.getBoundConnection(name, "dude", "sweet");
    assertNotNull(conn);
  }
}
