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
package org.sakaiproject.kernel.persondirectory;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PersonImplTest {
  String name = "tuser";
  private PersonImpl p;

  @Before
  public void setUp() {
    p = new PersonImpl(name);
  }

  @Test
  public void testConstructPerson() {
    String[] values = { "a value 1", "22", "true", "BLOCKY_TEST", "r4nd0m th!ngs" };
    for (int i = 0; i < values.length; i++) {
      p.addAttribute("attr" + i, values[i]);
    }

    PersonImpl myp = new PersonImpl(p);
    Set<String> pAttrNames = p.getAttributeNames();
    Set<String> mypAttrNames = p.getAttributeNames();
    assertEquals(pAttrNames.size(), mypAttrNames.size());

    for (String pAttrName : pAttrNames) {
      assertTrue(mypAttrNames.contains(pAttrName));
      String pVal = p.getAttributeValue(pAttrName);
      String mypVal = myp.getAttributeValue(pAttrName);
      assertEquals(pVal, mypVal);
    }
  }

  @Test
  public void testName() {
    assertEquals(name, p.getName());
  }

  @Test
  public void testGetAttributeNames() {
    String[] values = { "a value 1", "22", "true", "BLOCKY_TEST", "r4nd0m th!ngs" };
    for (int i = 0; i < values.length; i++) {
      p.addAttribute("attr" + i, values[i]);
    }

    Set<String> attrNames = p.getAttributeNames();
    assertEquals(values.length, attrNames.size());
    for (int i = 0; i < values.length; i++) {
      assertTrue(attrNames.contains("attr" + i));
    }
  }

  @Test
  public void testAddingGettingUniqueAttributes() {
    String[] values = { "a value 1", "22", "true", "BLOCKY_TEST", "r4nd0m th!ngs" };
    for (int i = 0; i < values.length; i++) {
      p.addAttribute("attr" + i, values[i]);
    }

    Map<String, String[]> attrs = p.getAttributes();
    for (int i = 0; i < values.length; i++) {
      String key = "attr" + i;
      assertTrue(attrs.containsKey(key));
      String vals = p.getAttributeValue(key);
      assertEquals(values[i], vals);
    }
  }

  @Test
  public void testAddingGettingNonUniqueAttributes() {
    p.addAttribute("attr0", "a value 1");
    p.addAttribute("attr1", "22");
    p.addAttribute("attr0", "true");
    p.addAttribute("attr1", "BLOCK_TEST");
    p.addAttribute("attr2", "r4nd0m th!ngs");

    Map<String, String[]> attrs = p.getAttributes();
    assertTrue(attrs.containsKey("attr0"));
    String[] vals = p.getAttributeValues("attr0");
    assertEquals(2, vals.length);
    assertEquals("a value 1", vals[0]);
    assertEquals("true", vals[1]);

    assertTrue(attrs.containsKey("attr1"));
    vals = p.getAttributeValues("attr1");
    assertEquals(2, vals.length);
    assertEquals("22", vals[0]);
    assertEquals("BLOCK_TEST", vals[1]);

    assertTrue(attrs.containsKey("attr2"));
    vals = p.getAttributeValues("attr2");
    assertEquals(1, vals.length);
    assertEquals("r4nd0m th!ngs", vals[0]);
  }

  @Test
  public void testAddAttributes() {
    Map<String, String[]> mattrs = new HashMap<String, String[]>();
    mattrs.put("attr0", new String[] { "a value 1", "true" });
    mattrs.put("attr1", new String[] { "22", "BLOCK_TEST" });
    mattrs.put("attr2", new String[] { "r4nd0m th!ngs" });
    p.addAttributes(mattrs);

    Map<String, String[]> attrs = p.getAttributes();
    assertTrue(attrs.containsKey("attr0"));
    String[] vals = p.getAttributeValues("attr0");
    assertEquals(2, vals.length);
    assertEquals("a value 1", vals[0]);
    assertEquals("true", vals[1]);

    assertTrue(attrs.containsKey("attr1"));
    vals = p.getAttributeValues("attr1");
    assertEquals(2, vals.length);
    assertEquals("22", vals[0]);
    assertEquals("BLOCK_TEST", vals[1]);

    assertTrue(attrs.containsKey("attr2"));
    vals = p.getAttributeValues("attr2");
    assertEquals(1, vals.length);
    assertEquals("r4nd0m th!ngs", vals[0]);
  }

  @Test
  public void testAddAttributesWithFilter() {
    Map<String, String[]> mattrs = new HashMap<String, String[]>();
    mattrs.put("attr0", new String[] { "a value 1", "true" });
    mattrs.put("attr1", new String[] { "22", "BLOCK_TEST" });
    mattrs.put("attr2", new String[] { "r4nd0m th!ngs" });
    p.addAttributes(mattrs, "attr0", "attr2");

    Map<String, String[]> attrs = p.getAttributes();
    assertTrue(attrs.containsKey("attr0"));
    String[] vals = p.getAttributeValues("attr0");
    assertEquals(2, vals.length);
    assertEquals("a value 1", vals[0]);
    assertEquals("true", vals[1]);

    assertFalse(attrs.containsKey("attr1"));

    assertTrue(attrs.containsKey("attr2"));
    vals = p.getAttributeValues("attr2");
    assertEquals(1, vals.length);
    assertEquals("r4nd0m th!ngs", vals[0]);
  }
}
