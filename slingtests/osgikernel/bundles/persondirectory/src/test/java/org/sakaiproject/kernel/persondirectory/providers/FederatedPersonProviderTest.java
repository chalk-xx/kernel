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
package org.sakaiproject.kernel.persondirectory.providers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sakaiproject.kernel.api.persondirectory.Person;
import org.sakaiproject.kernel.api.persondirectory.PersonProvider;
import org.sakaiproject.kernel.persondirectory.PersonImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Test the federation of person providers. Tests that the federating happens
 * correctly. Not so much about the actual retrieval of info but that each bound
 * provider is used.
 *
 * @author Carl Hall
 */
public class FederatedPersonProviderTest {
  private FederatedPersonProvider provider;
  private PersonProvider provider0;
  private PersonProvider provider1;
  private PersonProvider provider2;
  private PersonProvider provider3;
  private PersonProvider provider4;

  private PersonImpl person0;
  private PersonImpl person1;
  private PersonImpl person2;
  private PersonImpl person3;
  private PersonImpl person4;

  private static HashMap<String, String[]> attrs0 = new HashMap<String, String[]>();
  private static HashMap<String, String[]> attrs1 = new HashMap<String, String[]>();
  private static HashMap<String, String[]> attrs2 = new HashMap<String, String[]>();
  private static HashMap<String, String[]> attrs3 = new HashMap<String, String[]>();
  private static HashMap<String, String[]> attrs4 = new HashMap<String, String[]>();

  @BeforeClass
  public static void beforeClass() {
    attrs0.put("attr0", new String[] { "val0" });

    attrs1.putAll(attrs0);
    attrs1.put("attr1", new String[] { "val1" });

    attrs2.putAll(attrs1);
    attrs2.put("attr2", new String[] { "val2" });

    attrs3.putAll(attrs2);
    attrs3.put("attr3", new String[] { "val3" });

    attrs4.putAll(attrs3);
    attrs4.put("attr4", new String[] { "val4" });
  }

  @Before
  public void setUp() {
    provider = new FederatedPersonProvider();

    // create some providers
    provider0 = createMock(PersonProvider.class);
    provider.bindProvider(provider0);
    provider1 = createMock(PersonProvider.class);
    provider.bindProvider(provider1);
    provider2 = createMock(PersonProvider.class);
    provider.bindProvider(provider2);
    provider3 = createMock(PersonProvider.class);
    provider.bindProvider(provider3);
    provider4 = createMock(PersonProvider.class);
    provider.bindProvider(provider4);

    // create some people to return
    person0 = new PersonImpl("user0");
    person0.addAttributes(attrs0);
    person1 = new PersonImpl("user1");
    person1.addAttributes(attrs1);
    person2 = new PersonImpl("user2");
    person2.addAttributes(attrs2);
    person3 = new PersonImpl("user3");
    person3.addAttributes(attrs3);
    person4 = new PersonImpl("user4");
    person4.addAttributes(attrs4);
  }

  @After
  public void tearDown() {
    provider.unbindProvider(provider0);
    provider.unbindProvider(provider1);
    provider.unbindProvider(provider2);
    provider.unbindProvider(provider3);
    provider.unbindProvider(provider4);
  }

  /**
   * Get a person when there are no providers bound.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonWithNoProviders() throws Exception {
    FederatedPersonProvider provider = new FederatedPersonProvider();
    Person person = provider.getPerson("doesn't matter");
    assertNull(person);
  }

  /**
   * Get people when there are no providers bound.
   *
   * @throws Exception
   */
  @Test
  public void testGetPeopelWithNoProviders() throws Exception {
    HashSet<String> uids = new HashSet<String>();
    uids.add("user0");
    uids.add("user2");
    FederatedPersonProvider provider = new FederatedPersonProvider();
    Set<Person> people = provider.getPeople(uids);
    assertNull(people);
  }

  /**
   * Try to get a person that is not returned by any of the providers.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonAllAttrsNoneFound() throws Exception {
    expect(provider0.getPerson("user0")).andReturn(null);
    expect(provider1.getPerson("user0")).andReturn(null);
    expect(provider2.getPerson("user0")).andReturn(null);
    expect(provider3.getPerson("user0")).andReturn(null);
    expect(provider4.getPerson("user0")).andReturn(null);
    replay(provider0, provider1, provider2, provider3, provider4);

    Person person = provider.getPerson("user0");
    assertNull(person);
  }

  /**
   * Try to get people that is not returned by any of the providers.
   *
   * @throws Exception
   */
  @Test
  public void testGetPeopleAllAttrsNoneFound() throws Exception {
    HashSet<String> uids = new HashSet<String>();
    uids.add("user0");
    uids.add("user2");

    expect(provider0.getPeople(uids)).andReturn(null);
    expect(provider1.getPeople(uids)).andReturn(null);
    expect(provider2.getPeople(uids)).andReturn(null);
    expect(provider3.getPeople(uids)).andReturn(null);
    expect(provider4.getPeople(uids)).andReturn(null);
    replay(provider0, provider1, provider2, provider3, provider4);

    Set<Person> people = provider.getPeople(uids);
    assertNull(people);
  }

  /**
   * Get a person and all associated attributes. Tests with all providers
   * returning information for the requested person.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonAllAttrsAllReturn() throws Exception {
    expect(provider0.getPerson("user0")).andReturn(person0);
    expect(provider1.getPerson("user0")).andReturn(person1);
    expect(provider2.getPerson("user0")).andReturn(person2);
    expect(provider3.getPerson("user0")).andReturn(person3);
    expect(provider4.getPerson("user0")).andReturn(person4);
    replay(provider0, provider1, provider2, provider3, provider4);

    Person person = provider.getPerson("user0");
    assertNotNull(person);

    // expected size == maximum number of attrs returned by a provider
    // since we're using person4, we can get up to 5 attrs.
    Map<String, String[]> attrs = person.getAttributes();
    Set<String> attrNames = attrs.keySet();
    assertEquals(5, attrs.size());
    assertEquals(5, attrNames.size());

    for (int i = 0; i < 5; i++) {
      String[] vals = person.getAttributeValues("attr" + i);
      assertEquals(5 - i, vals.length);
      for (String val : vals) {
        assertEquals("val" + i, val);
      }
    }
  }

  /**
   * Get people and all associated attributes. Tests with all providers
   * returning information for the requested person.
   *
   * @throws Exception
   */
  @Test
  public void testGetPeopleAllAttrsAllReturn() throws Exception {
    HashSet<Person> people02 = new HashSet<Person>();
    people02.add(person0);
    people02.add(person2);

    HashSet<String> uids = new HashSet<String>();
    uids.add("user0");
    uids.add("user2");

    expect(provider0.getPeople(uids)).andReturn(people02);
    expect(provider1.getPeople(uids)).andReturn(people02);
    expect(provider2.getPeople(uids)).andReturn(people02);
    expect(provider3.getPeople(uids)).andReturn(people02);
    expect(provider4.getPeople(uids)).andReturn(people02);
    replay(provider0, provider1, provider2, provider3, provider4);

    Set<Person> people = provider.getPeople(uids);
    assertNotNull(people);

    for (Person person : people) {
      // expected size == maximum number of attrs returned by a provider
      // since we're using person2, we can get up to 3 attrs.
      Map<String, String[]> attrs = person.getAttributes();
      Set<String> attrNames = attrs.keySet();

      assertTrue(attrNames.contains("attr0"));
      assertFalse(attrNames.contains("attr3"));
      assertFalse(attrNames.contains("attr4"));

      // common things of all the returned people
      String[] vals = person.getAttributeValues("attr0");
      assertEquals(5, vals.length);
      String iVal = "val0";
      for (String val : vals) {
        assertEquals(iVal, val);
      }

      if ("user0".equals(person.getName())) {
        assertEquals(1, attrs.size());
        assertEquals(1, attrNames.size());
        assertFalse(attrNames.contains("attr1"));
        assertFalse(attrNames.contains("attr2"));
      } else if ("user2".equals(person.getName())) {
        assertEquals(3, attrs.size());
        assertEquals(3, attrNames.size());
        assertTrue(attrNames.contains("attr1"));
        assertTrue(attrNames.contains("attr2"));

        vals = person.getAttributeValues("attr1");
        assertEquals(5, vals.length);
        iVal = "val1";
        for (String val : vals) {
          assertEquals(iVal, val);
        }

        vals = person.getAttributeValues("attr2");
        assertEquals(5, vals.length);
        iVal = "val2";
        for (String val : vals) {
          assertEquals(iVal, val);
        }
      }
    }
  }

  /**
   * Get a person and some associated attributes. Tests with all providers
   * returning information for the requested person. Also test that even if a
   * provider returns too much information, only the request attributes are
   * returned to the caller.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonSomeAttrsAllReturn() throws Exception {
    expect(provider0.getPerson("user0", "attr1", "attr3")).andReturn(person0);
    expect(provider1.getPerson("user0", "attr1", "attr3")).andReturn(person1);
    expect(provider2.getPerson("user0", "attr1", "attr3")).andReturn(person2);
    expect(provider3.getPerson("user0", "attr1", "attr3")).andReturn(person3);
    expect(provider4.getPerson("user0", "attr1", "attr3")).andReturn(person4);
    replay(provider0, provider1, provider2, provider3, provider4);

    Person person = provider.getPerson("user0", "attr1", "attr3");
    assertNotNull(person);

    // expected size == maximum number of attrs returned by a provider
    // since we're using person4, we can get up to 5 attrs.
    Map<String, String[]> attrs = person.getAttributes();
    Set<String> attrNames = attrs.keySet();
    assertEquals(2, attrs.size());
    assertEquals(2, attrNames.size());

    assertFalse(attrNames.contains("attr0"));
    assertFalse(attrNames.contains("attr2"));
    assertFalse(attrNames.contains("attr4"));

    String[] vals = person.getAttributeValues("attr1");
    assertEquals(4, vals.length);
    for (String val : vals) {
      assertEquals("val1", val);
    }

    vals = person.getAttributeValues("attr3");
    assertEquals(2, vals.length);
    for (String val : vals) {
      assertEquals("val3", val);
    }
  }

  /**
   * Get a person and all associated attributes. Tests with only some providers
   * returning information for the requested person.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonAllAttrsSomeReturn() throws Exception {
    expect(provider0.getPerson("user1")).andReturn(null);
    expect(provider1.getPerson("user1")).andReturn(person1);
    expect(provider2.getPerson("user1")).andReturn(null);
    expect(provider3.getPerson("user1")).andReturn(person3);
    expect(provider4.getPerson("user1")).andReturn(null);
    replay(provider0, provider1, provider2, provider3, provider4);

    Person person = provider.getPerson("user1");
    assertNotNull(person);

    // expected size == maximum number of attrs returned by a provider
    // since we're using person3, we can get up to 4 attrs.
    Map<String, String[]> attrs = person.getAttributes();
    Set<String> attrNames = attrs.keySet();
    assertEquals(4, attrs.size());
    assertEquals(4, attrNames.size());

    String[] vals = person.getAttributeValues("attr0");
    assertEquals(2, vals.length);
    for (String val : vals) {
      assertEquals("val0", val);
    }

    vals = person.getAttributeValues("attr1");
    assertEquals(2, vals.length);
    for (String val : vals) {
      assertEquals("val1", val);
    }

    vals = person.getAttributeValues("attr2");
    assertEquals(1, vals.length);
    assertEquals("val2", vals[0]);

    vals = person.getAttributeValues("attr3");
    assertEquals(1, vals.length);
    assertEquals("val3", vals[0]);

    vals = person.getAttributeValues("attr4");
    assertNull(vals);
  }

  /**
   * Get people and all associated attributes. Tests with some providers
   * returning information for the requested person.
   *
   * @throws Exception
   */
  @Test
  public void testGetPeopleAllAttrsSomeReturn() throws Exception {
    HashSet<Person> people02 = new HashSet<Person>();
    people02.add(person0);
    people02.add(person2);

    HashSet<String> uids = new HashSet<String>();
    uids.add("user0");
    uids.add("user2");

    expect(provider0.getPeople(uids)).andReturn(null);
    expect(provider1.getPeople(uids)).andReturn(people02);
    expect(provider2.getPeople(uids)).andReturn(people02);
    expect(provider3.getPeople(uids)).andReturn(null);
    expect(provider4.getPeople(uids)).andReturn(null);
    replay(provider0, provider1, provider2, provider3, provider4);

    Set<Person> people = provider.getPeople(uids);
    assertNotNull(people);

    for (Person person : people) {
      // expected size == maximum number of attrs returned by a provider
      // since we're using person2, we can get up to 3 attrs.
      Map<String, String[]> attrs = person.getAttributes();
      Set<String> attrNames = attrs.keySet();

      assertTrue(attrNames.contains("attr0"));
      assertFalse(attrNames.contains("attr3"));
      assertFalse(attrNames.contains("attr4"));

      // common things of all the returned people
      String[] vals = person.getAttributeValues("attr0");
      assertEquals(2, vals.length);
      String iVal = "val0";
      for (String val : vals) {
        assertEquals(iVal, val);
      }

      if ("user0".equals(person.getName())) {
        assertEquals(1, attrs.size());
        assertEquals(1, attrNames.size());
        assertFalse(attrNames.contains("attr1"));
        assertFalse(attrNames.contains("attr2"));
      } else if ("user2".equals(person.getName())) {
        assertEquals(3, attrs.size());
        assertEquals(3, attrNames.size());
        assertTrue(attrNames.contains("attr1"));
        assertTrue(attrNames.contains("attr2"));

        vals = person.getAttributeValues("attr1");
        assertEquals(2, vals.length);
        iVal = "val1";
        for (String val : vals) {
          assertEquals(iVal, val);
        }

        vals = person.getAttributeValues("attr2");
        assertEquals(2, vals.length);
        iVal = "val2";
        for (String val : vals) {
          assertEquals(iVal, val);
        }
      }
    }
  }

  /**
   * Get a person and some associated attributes. Tests with some providers
   * returning information for the requested person. Also test that even if a
   * provider returns too much information, only the request attributes are
   * returned to the caller.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonSomeAttrsSomeReturn() throws Exception {
    expect(provider0.getPerson("user0", "attr0", "attr2")).andReturn(person0);
    expect(provider1.getPerson("user0", "attr0", "attr2")).andReturn(null);
    expect(provider2.getPerson("user0", "attr0", "attr2")).andReturn(person2);
    expect(provider3.getPerson("user0", "attr0", "attr2")).andReturn(null);
    expect(provider4.getPerson("user0", "attr0", "attr2")).andReturn(null);
    replay(provider0, provider1, provider2, provider3, provider4);

    Person person = provider.getPerson("user0", "attr0", "attr2");
    assertNotNull(person);

    // expected size == maximum number of attrs returned by a provider
    // since we're using person4, we can get up to 5 attrs.
    Map<String, String[]> attrs = person.getAttributes();
    Set<String> attrNames = attrs.keySet();
    assertEquals(2, attrs.size());
    assertEquals(2, attrNames.size());

    assertFalse(attrNames.contains("attr1"));
    assertFalse(attrNames.contains("attr3"));
    assertFalse(attrNames.contains("attr4"));

    String[] vals = person.getAttributeValues("attr0");
    assertEquals(2, vals.length);
    for (String val : vals) {
      assertEquals("val0", val);
    }

    vals = person.getAttributeValues("attr2");
    assertEquals(1, vals.length);
    assertEquals("val2", vals[0]);
  }
}
