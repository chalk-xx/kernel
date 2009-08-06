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

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertNotNull;

import com.novell.ldap.LDAPConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.api.configuration.ConfigurationService;
import org.sakaiproject.kernel.ldap.api.LdapConnectionManagerConfig;

import java.net.ServerSocket;

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
    config = new LdapConnectionManagerConfig();
    config.setLdapHost("localhost");
    config.setLdapPort(LDAPConnection.DEFAULT_PORT + 1000);

    broker = new PoolingLdapConnectionBroker(configService);
    broker.activate(null);

    // Provide a port for the connection to attach to. Does not do anything LDAP
    // specific.
    serverSocket = new ServerSocket(LDAPConnection.DEFAULT_PORT + 1000);
  }

  @After
  public void tearDown() throws Exception {
    serverSocket.close();
  }

  @Test
  public void testCreateDefault() {
    broker.create(name);
  }

  @Test
  public void testCreateCustom() {
    broker.create(name, config);
  }

  @Test
  public void getConnection() throws Exception {
    broker.create(name, config);
    LDAPConnection conn = broker.getConnection(name);
    assertNotNull(conn);
  }
}
