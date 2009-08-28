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
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.kernel.api.ldap.LdapException;

import java.net.URL;

/**
 *
 */
public class SimpleLdapConnectionManagerTest {
  private LdapConnectionManagerConfig config;
  private SimpleLdapConnectionManager mgr;
  private LDAPConnection conn;
  private String keystoreLocation;
  private String keystorePassword = "keystore123";

  @Before
  public void setUp() throws Exception {
    URL url = getClass().getResource("server_keystore.jks");
    keystoreLocation = url.getPath();

    config = new LdapConnectionManagerConfig();
    config.setLdapHost("localhost");
    config.setLdapPort(LDAPConnection.DEFAULT_PORT);
    config.setLdapUser("ldapUser");
    config.setLdapPassword("ldapPassword");

    conn = createMock(LDAPConnection.class);

    mgr = new SimpleLdapConnectionManager() {
      @Override
      protected LDAPConnection newLDAPConnection() {
        return conn;
      }
    };
    mgr.setConfig(config);
  }

  @After
  public void tearDown() throws Exception {
    mgr.destroy();
  }

  @Test
  public void testGetConfig() {
    assertEquals(mgr.getConfig(), config);
  }

  @Test
  public void testNewLdapConnection() {
    new SimpleLdapConnectionManager().newLDAPConnection();
  }

  @Test
  public void testConnection() throws Exception {
    mgr.init();
    mgr.getConnection();
  }

  @Test
  public void testConnectionCantConnect() throws Exception {
    try {
      conn.setConstraints((LDAPConstraints) anyObject());
      expectLastCall();

      conn.connect((String) anyObject(), anyInt());
      expectLastCall().andThrow(new LDAPException());
      replay(conn);

      mgr.init();
      mgr.getConnection();
      fail("Should throw an exception when can't connect.");
    } catch (LdapException e) {
      // expected
    }
  }

  @Test
  public void testConnectionLdapFailPostConnect() throws Exception {
    config.setSecureConnection(true);
    config.setTLS(true);

    conn.setConstraints((LDAPConstraints) anyObject());

    conn.connect((String) anyObject(), anyInt());

    conn.startTLS();
    expectLastCall().andThrow(new LDAPException());

    conn.disconnect();

    replay(conn);
    try {
      mgr.getConnection();
      fail("Should throw an exception when can't start TLS.");
    } catch (LdapException e) {
      // expected
    }
  }

  @Test
  public void testConnectionLdapFailPostConnectFailDisconnect() throws Exception {
    config.setSecureConnection(true);
    config.setTLS(true);

    conn.setConstraints((LDAPConstraints) anyObject());

    conn.connect((String) anyObject(), anyInt());

    conn.startTLS();
    expectLastCall().andThrow(new LDAPException());

    conn.disconnect();
    expectLastCall().andThrow(new LDAPException());

    replay(conn);
    try {
      mgr.getConnection();
      fail("Should throw an exception when can't start TLS.");
    } catch (LdapException e) {
      // expected
    }
  }

  @Test
  public void testConnectionRuntimeFailPostConnect() throws Exception {
    config.setSecureConnection(true);
    config.setTLS(true);

    conn.setConstraints((LDAPConstraints) anyObject());

    conn.connect((String) anyObject(), anyInt());

    conn.startTLS();
    expectLastCall().andThrow(new RuntimeException());

    conn.disconnect();

    replay(conn);
    try {
      mgr.getConnection();
      fail("Should throw an exception when can't start TLS.");
    } catch (RuntimeException e) {
      // expected
    }
  }

  @Test
  public void testConnectionRuntimeFailPostConnectDisconnect() throws Exception {
    config.setSecureConnection(true);
    config.setTLS(true);

    conn.setConstraints((LDAPConstraints) anyObject());

    conn.connect((String) anyObject(), anyInt());

    conn.startTLS();
    expectLastCall().andThrow(new RuntimeException());

    conn.disconnect();
    expectLastCall().andThrow(new LDAPException());

    replay(conn);
    try {
      mgr.getConnection();
      fail("Should throw an exception when can't start TLS.");
    } catch (RuntimeException e) {
      // expected
    }
  }

  @Test
  public void testBoundConnection() throws Exception {
    conn.setConstraints((LDAPConstraints) anyObject());
    expectLastCall();

    conn.connect((String) anyObject(), anyInt());
    expectLastCall();

    conn.bind(anyInt(), (String) anyObject(), (byte[]) anyObject());
    expectLastCall();
    replay(conn);

    mgr.getBoundConnection("someDN", "somePassword");
  }

  @Test
  public void testBoundConnectionCantBind() throws Exception {
    conn.setConstraints((LDAPConstraints) anyObject());
    expectLastCall();

    conn.connect((String) anyObject(), anyInt());
    expectLastCall();

    conn.bind(anyInt(), (String) anyObject(), (byte[]) anyObject());
    expectLastCall().andThrow(new LDAPException());
    replay(conn);

    try {
      mgr.getBoundConnection("someDN", "somePassword");
      fail("Should throw an exception when bind throws an exception.");
    } catch (LdapException e) {
      // expected
    }
  }

  @Test
  public void testConnectionAutoBind() throws Exception {
    conn.setConstraints((LDAPConstraints) anyObject());
    expectLastCall();

    conn.connect((String) anyObject(), anyInt());
    expectLastCall();

    conn.bind(anyInt(), (String) anyObject(), (byte[]) anyObject());

    config.setAutoBind(true);
    mgr.init();

    replay(conn);

    mgr.getConnection();
  }

  @Test
  public void testInitKeystoreNoPassword() throws Exception {
    config.setKeystoreLocation(keystoreLocation);
    config.setSecureConnection(true);
    mgr.init();
  }

  @Test
  public void testInitKeystorePassword() throws Exception {
    config.setKeystoreLocation(keystoreLocation);
    config.setKeystorePassword(keystorePassword);
    config.setSecureConnection(true);
    mgr.init();
  }

  @Test
  public void testInitKeystoreMissing() {
    try {
      config.setKeystoreLocation(keystoreLocation + "xxx");
      config.setSecureConnection(true);
      mgr.init();
      fail("Should throw exception if the keystore location is invalid.");
    } catch (LdapException e) {
      // expected exception
    }
  }

  @Test
  public void testConnectionSecureTls() throws Exception {
    config.setKeystoreLocation(keystoreLocation);
    config.setSecureConnection(true);
    config.setTLS(true);
    mgr.init();
    mgr.getConnection();
  }

  @Test
  public void testReturnNullConnection() throws Exception {
    mgr.returnConnection(null);
  }

  @Test
  public void testReturnLiveConnection() throws Exception {
    conn.disconnect();
    expectLastCall();
    replay(conn);
    mgr.returnConnection(conn);
  }

  @Test
  public void testReturnBadConnection() throws Exception {
    conn.disconnect();
    expectLastCall().andThrow(new LDAPException());
    replay(conn);

    mgr.returnConnection(conn);
  }
}
