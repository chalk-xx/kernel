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
package org.sakaiproject.nakamura.auth.ldap;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionBroker;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.nakamura.api.ldap.LdapException;

import java.util.Dictionary;

import static org.mockito.Mockito.*;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

@RunWith(MockitoJUnitRunner.class)
public class LdapAuthenticationPluginTest {
  
  private LdapAuthenticationPlugin ldapAuthenticationPlugin;
  
  @Mock
  private ComponentContext componentContext;
  
  @Mock
  private LdapConnectionBroker connBroker;

  @Mock
  private LdapConnectionManager connMgr;
  
  @Before
  public void setup() throws Exception {
    when(connBroker.create(isA(String.class), isA(LdapConnectionManagerConfig.class))).thenReturn(connMgr);
    when(connMgr.getBoundConnection(anyString(), anyString())).thenReturn(null);
    ldapAuthenticationPlugin = new LdapAuthenticationPlugin();
    ldapAuthenticationPlugin.connBroker = connBroker;
  }

  @Test
  public void canHandleSimpleCredentials() {
    assertTrue(ldapAuthenticationPlugin.canHandle(simpleCredentials()));
  }
  
  @Test
  public void canNotHandleOtherThanSimpleCredentials() {
    Credentials credentials = mock(Credentials.class);
    assertFalse(ldapAuthenticationPlugin.canHandle(credentials));
  }
  
  @Test
  public void createsAConnectionBrokerUponActivation() throws Exception {
    // given
    aContextThatCanReturnProperties();
    
    // when
    ldapAuthenticationPlugin.activate(componentContext);
    
    // then
    verify(connBroker).create(isA(String.class), isA(LdapConnectionManagerConfig.class));
  }
  
  @Test
  public void destroysConnectionBrokerOnDeactivation() throws Exception {
    // when
    ldapAuthenticationPlugin.deactivate(componentContext);
    
    // then
    verify(connBroker).destroy(anyString());
  }
  
  @Test
  public void authenticationFailsIfNotSimpleCredentials() throws Exception {
    // given
    Credentials credentials = mock(Credentials.class);
    
    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(credentials));
  }
  
  @Test
  public void canAuthenticateWithValidCredentials() throws Exception {
    // given
    connectionBrokerWillAllowAnything();
    aContextThatCanReturnProperties();
    ldapAuthenticationPlugin.activate(componentContext);
    
    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(simpleCredentials()));
  }

  private SimpleCredentials simpleCredentials() {
    String name = "joe";
    char[] password = {'p','a','s','s'};
    return new SimpleCredentials(name, password);
  }

  private void connectionBrokerWillAllowAnything() throws LdapException, LDAPException {
    LDAPConnection conn = mock(LDAPConnection.class);
    when(conn.compare(anyString(), (LDAPAttribute)anyObject())).thenReturn(Boolean.TRUE);
    when(connBroker.getConnection(anyString())).thenReturn(conn);
  }

  @SuppressWarnings("unchecked")
  private void aContextThatCanReturnProperties() {
    Dictionary props = mock(Dictionary.class);
    when(props.get(LdapAuthenticationPlugin.LDAP_CONNECTION_SECURE)).thenReturn(Boolean.TRUE);
    when(props.get(LdapAuthenticationPlugin.LDAP_PORT)).thenReturn(389);
    when(props.get(LdapAuthenticationPlugin.LDAP_BASE_DN)).thenReturn("uid=%s");
    when(componentContext.getProperties()).thenReturn(props);
  }
}
