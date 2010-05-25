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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.stubbing.defaultanswers.Answers.RETURNS_DEEP_STUBS;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;

import java.util.HashMap;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

@RunWith(MockitoJUnitRunner.class)
public class LdapAuthenticationPluginTest {

  private final static String LDAP_USER = "uid=admin,ou=Special People,o=nyu.edu,o=nyu";
  private final static String LDAP_PASS = "admin";
  private final static String LDAP_BASE_DN = "ou=People,o=nyu.edu,o=nyu";
  private final static String USER_FILTER = "uid={}";
  private final static String AUTHZ_FILTER = "eduEntitlements=sakai";
  private final static String USER = "joe";
  private final static String PASS = "pass";

  private LdapAuthenticationPlugin ldapAuthenticationPlugin;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private LdapConnectionManager connMgr;

  @Mock
  private LDAPConnection conn;

  @Mock
  private LDAPSearchResults results;

  @Before
  public void setup() throws Exception {
    when(connMgr.getConfig().getLdapUser()).thenReturn(LDAP_USER);
    when(connMgr.getConfig().getLdapPassword()).thenReturn(LDAP_PASS);
    ldapAuthenticationPlugin = new LdapAuthenticationPlugin(connMgr);
  }

  @Test
  public void defaultConstructor() {
    new LdapAuthenticationPlugin();
  }

  @Test
  public void activateModify() {
    new LdapAuthenticationPlugin().activate(new HashMap<String, String>());
    new LdapAuthenticationPlugin().modified(new HashMap<String, String>());
  }

  @Test
  public void authenticationFailsIfNotSimpleCredentials() throws Exception {
    // given
    Credentials credentials = mock(Credentials.class);

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(credentials));
  }

  @Test
  public void canAuthenticateWithValidCredentialsNoFilter() throws Exception {
    // given
    HashMap<String, String> props = new HashMap<String, String>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(anyString(), anyInt(), anyString(), any(String[].class), anyBoolean()))
        .thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry ldapEntry = mock(LDAPEntry.class, RETURNS_DEEP_STUBS.get());
    when(results.next()).thenReturn(ldapEntry);
    when(ldapEntry.getAttribute("objectClass").getStringValue()).thenReturn(
        "inetOrgPerson");

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(simpleCredentials()));

    verify(ldapEntry).getDN();
  }

  @Test
  public void authenticateWithValidCredentialsWithFilter() throws Exception {
    // given
    HashMap<String, String> props = new HashMap<String, String>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.AUTHZ_FILTER, AUTHZ_FILTER);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(anyString(), anyInt(), anyString(), any(String[].class), anyBoolean()))
        .thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry ldapEntry = mock(LDAPEntry.class, RETURNS_DEEP_STUBS.get());
    when(results.next()).thenReturn(ldapEntry);
    when(ldapEntry.getAttribute("objectClass").getStringValue()).thenReturn(
        "inetOrgPerson");

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(simpleCredentials()));

    verify(ldapEntry).getDN();
  }

  @Test
  public void useAliasObject() throws Exception {
    // given
    HashMap<String, String> props = new HashMap<String, String>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.AUTHZ_FILTER, AUTHZ_FILTER);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(anyString(), anyInt(), anyString(), any(String[].class), anyBoolean()))
        .thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    LDAPEntry ldapEntry = mock(LDAPEntry.class, RETURNS_DEEP_STUBS.get());

    when(results.next()).thenReturn(ldapEntry);
    when(ldapEntry.getAttribute("objectClass").getStringValue())
        .thenReturn("aliasObject");

    // then
    assertTrue(ldapAuthenticationPlugin.authenticate(simpleCredentials()));

    // verify that the alias attributes where accessed
    verify(ldapEntry, atLeastOnce()).getAttribute("objectClass");
    verify(ldapEntry, atLeastOnce()).getAttribute("aliasedObjectName");
  }

  @Test
  public void failConnectingToLdapHost() throws Exception {
    // given
    HashMap<String, String> props = new HashMap<String, String>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenThrow(new LDAPException());

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(simpleCredentials()));
  }

  @Test
  public void failBindingAsAppUser() throws Exception {
    // given
    HashMap<String, String> props = new HashMap<String, String>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    doThrow(new LDAPException()).when(conn).bind(LDAPConnection.LDAP_V3, LDAP_USER,
        LDAP_PASS.getBytes("UTF-8"));

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(simpleCredentials()));
  }

  @Test
  public void failSearchingForUser() throws Exception {
    // given
    HashMap<String, String> props = new HashMap<String, String>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(anyString(), anyInt(), anyString(), any(String[].class), anyBoolean()))
        .thenReturn(results);
    when(results.hasMore()).thenReturn(false);

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(simpleCredentials()));
  }

  @Test
  public void failBindingAsUser() throws Exception {
    // given
    HashMap<String, String> props = new HashMap<String, String>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.AUTHZ_FILTER, AUTHZ_FILTER);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(anyString(), anyInt(), anyString(), any(String[].class), anyBoolean()))
        .thenReturn(results);
    when(results.hasMore()).thenReturn(true);
    doThrow(new LDAPException()).when(conn).bind(LDAPConnection.LDAP_V3,
        USER_FILTER.replace("{}", USER) + ", " + LDAP_BASE_DN, PASS.getBytes("UTF-8"));

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(simpleCredentials()));
  }

  @Test
  public void failBindingAsAppUserAfterUserSearch() throws Exception {
    // given
    HashMap<String, String> props = new HashMap<String, String>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.AUTHZ_FILTER, AUTHZ_FILTER);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    doNothing().doThrow(new LDAPException()).when(conn).bind(LDAPConnection.LDAP_V3,
        LDAP_USER, LDAP_PASS.getBytes("UTF-8"));
    when(
        conn.search(anyString(), anyInt(), anyString(), any(String[].class), anyBoolean()))
        .thenReturn(results);
    when(results.hasMore()).thenReturn(true);

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(simpleCredentials()));
  }

  @Test
  public void failAuthzFilterSearch() throws Exception {
    // given
    HashMap<String, String> props = new HashMap<String, String>();
    props.put(LdapAuthenticationPlugin.LDAP_BASE_DN, LDAP_BASE_DN);
    props.put(LdapAuthenticationPlugin.USER_FILTER, USER_FILTER);
    props.put(LdapAuthenticationPlugin.AUTHZ_FILTER, AUTHZ_FILTER);
    ldapAuthenticationPlugin.activate(props);

    when(connMgr.getConnection()).thenReturn(conn);
    when(
        conn.search(anyString(), anyInt(), anyString(), any(String[].class), anyBoolean()))
        .thenReturn(results);
    when(results.hasMore()).thenReturn(true).thenReturn(false);

    // then
    assertFalse(ldapAuthenticationPlugin.authenticate(simpleCredentials()));
  }

  private SimpleCredentials simpleCredentials() {
    return new SimpleCredentials(USER, PASS.toCharArray());
  }
}
