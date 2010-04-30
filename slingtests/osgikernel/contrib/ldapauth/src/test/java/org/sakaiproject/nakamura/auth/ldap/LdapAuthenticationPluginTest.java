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

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPSearchResults;

import static junit.framework.Assert.assertTrue;

import static junit.framework.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.stubbing.defaultanswers.Answers;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;

import java.util.HashMap;

import static org.mockito.Mockito.*;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

@RunWith(MockitoJUnitRunner.class)
public class LdapAuthenticationPluginTest {
  
  private LdapAuthenticationPlugin ldapAuthenticationPlugin;
  
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private LdapConnectionManager connMgr;
  
  @Mock
  private LDAPConnection conn;
  
  @Mock
  private LDAPSearchResults results;
  
  @Before
  public void setup() throws Exception {
    when(connMgr.getBoundConnection(anyString(), anyString())).thenReturn(conn);
    when(connMgr.getConfig().getLdapUser()).thenReturn("admin");
    when(connMgr.getConfig().getLdapPassword()).thenReturn("admin");
    ldapAuthenticationPlugin = new LdapAuthenticationPlugin(connMgr);
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
    HashMap<String, String> props = new HashMap<String, String>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, "ou=People,o=nyu.edu,o=nyu");
    props.put(LdapAuthenticationPlugin.USER_FILTER, "uid={}");
    props.put(LdapAuthenticationPlugin.AUTHZ_FILTER, "eduEntitlements=sakai");
    ldapAuthenticationPlugin.activate(props);
    
    when(
        conn.search(anyString(), anyInt(), anyString(), any(String[].class), anyBoolean()))
        .thenReturn(results);
    when(results.getCount()).thenReturn(1);

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(simpleCredentials()));
  }

  private SimpleCredentials simpleCredentials() {
    String name = "joe";
    char[] password = {'p','a','s','s'};
    return new SimpleCredentials(name, password);
  }
}
