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

import static junit.framework.Assert.assertTrue;

import static junit.framework.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;

import java.util.HashMap;

import static org.mockito.Mockito.*;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

@RunWith(MockitoJUnitRunner.class)
public class LdapAuthenticationPluginTest {
  
  private LdapAuthenticationPlugin ldapAuthenticationPlugin;
  
  @Mock
  private LdapConnectionManager connMgr;
  
  @Before
  public void setup() throws Exception {
    when(connMgr.getBoundConnection(anyString(), anyString())).thenReturn(null);
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
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, "uid={}");
    ldapAuthenticationPlugin.activate(props);
    
    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(simpleCredentials()));
  }

  private SimpleCredentials simpleCredentials() {
    String name = "joe";
    char[] password = {'p','a','s','s'};
    return new SimpleCredentials(name, password);
  }
}
