/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.persondirectory.providers;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;

import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionBroker;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.nakamura.api.ldap.LdapException;
import org.sakaiproject.nakamura.api.persondirectory.Person;
import org.sakaiproject.nakamura.api.persondirectory.PersonProviderException;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class LdapPersonProviderTest {
  /**
   * Test for the default constructor. Too simple to not have and boosts code
   * coverage.
   *
   * @throws Exception
   */
  @Test
  public void testDefaultConstructor() throws Exception {
    try {
      LdapPersonProvider provider = new LdapPersonProvider();
      provider.getPerson("whatever", null);
      fail("Should fail when the broker isn't explicitly set or injected by OSGi reference.");
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void testActivateWithAllProperties() throws Exception {
    LdapConnectionBroker broker = createMock(LdapConnectionBroker.class);
    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();
    expect(broker.getDefaultConfig()).andReturn(config);
    expect(broker.create(isA(String.class), isA(LdapConnectionManagerConfig.class)))
        .andReturn(null);
    replay(broker);

    LdapPersonProvider provider = new LdapPersonProvider(broker);
    String[] attrMap = new String[] { "attr0=>wow wee", "attr1 => camera 1" };
    provider.activate(buildContext(attrMap));
    Map<String, String> attributesMap = provider.getAttributesMap();
    assertTrue(attributesMap.containsKey("attr0"));
    assertEquals("wow wee", attributesMap.get("attr0"));
    assertTrue(attributesMap.containsKey("attr1"));
    assertEquals("camera 1", attributesMap.get("attr1"));
  }

  @Test
  public void testActivateWithBadPropertySeparator() {
    LdapPersonProvider provider = new LdapPersonProvider();
    String[] attrMap = new String[] { "attr1 -> camera 1" };
    try {
      provider.activate(buildContext(attrMap));
      fail("Should fail on improper mapping syntax.");
    } catch (ComponentException e) {
      // expected
    }
  }

  @Test
  public void testActivateWithEmptyFirstKey() {
    LdapPersonProvider provider = new LdapPersonProvider();
    String[] attrMap = new String[] { "=>wow wee" };
    try {
      provider.activate(buildContext(attrMap));
      fail("Should fail on improper mapping syntax.");
    } catch (ComponentException e) {
      // expected
    }
  }

  @Test
  public void testActivateWithEmptySecondKey() {
    LdapPersonProvider provider = new LdapPersonProvider();
    String[] attrMap = new String[] { "attr0=> " };
    try {
      provider.activate(buildContext(attrMap));
      fail("Should fail on improper mapping syntax.");
    } catch (ComponentException e) {
      // expected
    }
  }

  /**
   * Test getting a person from an ldap provider.
   *
   * @throws Exception
   */
  @Test
  public void testGetPerson() throws Exception {
    String[] attrMap = new String[] { "firstname => called" };
    LdapPersonProvider provider = setUpForPositiveTest(attrMap);
    Person person = provider.getPerson("tUser", null);
    assertNotNull(person);

    Set<String> attributeNames = person.getAttributeNames();
    assertNotNull(attributeNames);

    assertEquals(2, attributeNames.size());

    assertTrue(attributeNames.contains("called"));
    assertEquals("Tester", person.getAttributeValue("called"));

    assertTrue(attributeNames.contains("lastname"));
    assertEquals("User", person.getAttributeValue("lastname"));
  }

  /**
   * Test getting "admin" from an ldap provider.
   *
   * @throws Exception
   */
  @Test
  public void testGetAdmin() throws Exception {
    String[] attrMap = new String[] { "firstname => called" };
    LdapPersonProvider provider = setUpForPositiveTest(attrMap);
    Person person = provider.getPerson("admin", null);
    assertNull("Should not allow lookup of 'admin'", person);

    provider = setUpForPositiveTest(attrMap, true);
    person = provider.getPerson("admin", null);
    Set<String> attributeNames = person.getAttributeNames();
    assertNotNull("Should allow lookup of 'admin'", attributeNames);

    assertEquals(2, attributeNames.size());

    assertTrue(attributeNames.contains("called"));
    assertEquals("Tester", person.getAttributeValue("called"));

    assertTrue(attributeNames.contains("lastname"));
    assertEquals("User", person.getAttributeValue("lastname"));
  }

  /**
   * Test getPerson() when LdapConnectionBroker.getBoundConnection(..) throws an
   * LdapException.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonThrowsLdapException() throws Exception {
    LdapConnectionBroker broker = createMock(LdapConnectionBroker.class);
    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();
    expect(broker.getDefaultConfig()).andReturn(config);
    expect(broker.getBoundConnection(isA(String.class))).andThrow(new LdapException("oops"));
    expect(broker.create(isA(String.class), isA(LdapConnectionManagerConfig.class)))
        .andReturn(null);
    replay(broker);

    LdapPersonProvider provider = new LdapPersonProvider(broker);
    provider.activate(buildContext(null));
    try {
      provider.getPerson("tUser", null);
      fail("Should bubble up exceptions that are thrown internally.");
    } catch (PersonProviderException e) {
      // expected
    }
  }

  /**
   * Test getPerson() when LDAPConnection.search(..) throws an LDAPException.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonThrowsLDAPException() throws Exception {
    LdapConnectionBroker broker = createMock(LdapConnectionBroker.class);
    LDAPConnection connection = EasyMock.createMock(LDAPConnection.class);
    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();
    expect(broker.getDefaultConfig()).andReturn(config);
    expect(broker.getBoundConnection(isA(String.class))).andReturn(connection);
    expect(broker.create(isA(String.class), isA(LdapConnectionManagerConfig.class)))
        .andReturn(null);
    replay(broker);
    expect(
        connection.search(isA(String.class), anyInt(), isA(String.class), (String[]) anyObject(),
            anyBoolean(), isA(LDAPSearchConstraints.class))).andThrow(new LDAPException());
    EasyMock.replay(connection);

    LdapPersonProvider provider = new LdapPersonProvider(broker);
    provider.activate(buildContext(null));
    try {
      provider.getPerson("tUser", null);
      fail("Should bubble up exceptions that are thrown internally.");
    } catch (PersonProviderException e) {
      // expected
    }
  }

  private LdapPersonProvider setUpForPositiveTest(String[] attributeMap) throws Exception {
    return setUpForPositiveTest(attributeMap, false);
  }

  /**
   * Setup everything needed for a test that follows the most positive path of
   * action.
   *
   * @return
   * @throws Exception
   */
  private LdapPersonProvider setUpForPositiveTest(String[] attributeMap, boolean allowAdmin)
      throws Exception {
    LDAPConnection connection = EasyMock.createMock(LDAPConnection.class);
    LDAPSearchResults results = EasyMock.createMock(LDAPSearchResults.class);
    LDAPAttributeSet attrSet = EasyMock.createMock(LDAPAttributeSet.class);
    Iterator attrIter = createMock(Iterator.class);
    LDAPEntry entry = EasyMock.createMock(LDAPEntry.class);

    LdapConnectionBroker broker = createMock(LdapConnectionBroker.class);
    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();
    expect(broker.getDefaultConfig()).andReturn(config);
    expect(broker.create(isA(String.class), isA(LdapConnectionManagerConfig.class)))
        .andReturn(null);
    expect(broker.getBoundConnection(isA(String.class))).andReturn(connection);
    replay(broker);
    expect(
        connection.search(isA(String.class), anyInt(), isA(String.class), (String[]) anyObject(),
            anyBoolean(), isA(LDAPSearchConstraints.class))).andReturn(results);
    EasyMock.replay(connection);

    // get a result
    expect(results.hasMore()).andReturn(TRUE);
    expect(results.next()).andReturn(entry);

    // get the attributes and an iterator to them
    expect(entry.getAttributeSet()).andReturn(attrSet);
    expect(attrSet.iterator()).andReturn(attrIter);
    EasyMock.replay(entry, attrSet);

    // first loop through
    expect(attrIter.hasNext()).andReturn(TRUE);
    LDAPAttribute attr = EasyMock.createMock(LDAPAttribute.class);
    expect(attr.getName()).andReturn("firstname");
    expect(attr.getStringValueArray()).andReturn(new String[] { "Tester" });
    expect(attrIter.next()).andReturn(attr);
    EasyMock.replay(attr);

    // second loop through
    expect(attrIter.hasNext()).andReturn(TRUE);
    attr = EasyMock.createMock(LDAPAttribute.class);
    expect(attr.getName()).andReturn("lastname");
    expect(attr.getStringValueArray()).andReturn(new String[] { "User" });
    expect(attrIter.next()).andReturn(attr);
    EasyMock.replay(attr);

    // stop loop through attributes
    expect(attrIter.hasNext()).andReturn(FALSE);
    replay(attrIter);

    // stop loop through results
    expect(results.hasMore()).andReturn(FALSE);
    EasyMock.replay(results);

    LdapPersonProvider provider = new LdapPersonProvider(broker);
    provider.activate(buildContext(attributeMap, allowAdmin));
    return provider;
  }

  private ComponentContext buildContext(String[] attributeMap) {
    return buildContext(attributeMap, false);
  }

  private ComponentContext buildContext(String[] attributeMap, boolean allowAdmin) {
    Properties props = new Properties();
    props.put(LdapPersonProvider.PROP_BASE_DN, "ou=accounts,dc=sakai");
    props.put(LdapPersonProvider.PROP_FILTER_PATTERN, "uid={}");
    if (attributeMap != null) {
      props.put(LdapPersonProvider.PROP_ATTRIBUTES_MAP, attributeMap);
    }
    props.put(LdapPersonProvider.PROP_ALLOW_ADMIN_LOOKUP, allowAdmin);

    ComponentContext ctx = createMock(ComponentContext.class);
    expect(ctx.getProperties()).andReturn(props);
    replay(ctx);
    return ctx;
  }
}
