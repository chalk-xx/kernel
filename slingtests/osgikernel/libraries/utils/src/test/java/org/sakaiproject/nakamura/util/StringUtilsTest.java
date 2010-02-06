package org.sakaiproject.nakamura.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringUtilsTest {

  @Test
  public void testSplitStringChar() {
    String s = "Lorem ipsum dolor sit amet.";
    String[] result = StringUtils.split(s, ' ');
    assertEquals(5, result.length);

    s = "/Lorem/ipsum/dolor/sit/amet./";
    result = StringUtils.split(s, '/');
    assertEquals(5, result.length);
    assertEquals("Lorem", result[0]);
    assertEquals("ipsum", result[1]);
    assertEquals("dolor", result[2]);
    assertEquals("sit", result[3]);
    assertEquals("amet.", result[4]);

    s = "";
    result = StringUtils.split(s, '/');
    assertEquals(0, result.length);

    s = null;
    result = StringUtils.split(s, '/');
    assertEquals(0, result.length);
  }

  @Test
  public void testSplitStringCharInt() {
    String s = "Lorem ipsum dolor sit amet.";
    String[] result = StringUtils.split(s, ' ', 3);
    assertEquals(3, result.length);

    s = "/Lorem/ipsum/dolor/sit/amet./";
    result = StringUtils.split(s, '/', 3);
    assertEquals(3, result.length);
    assertEquals("Lorem", result[0]);
    assertEquals("ipsum", result[1]);
    assertEquals("dolor", result[2]);

    // try without leading and trailing
    s = "Lorem/ipsum/dolor/sit/amet.";
    result = StringUtils.split(s, '/', 3);
    assertEquals(3, result.length);
    assertEquals("Lorem", result[0]);
    assertEquals("ipsum", result[1]);
    assertEquals("dolor", result[2]);

    result = StringUtils.split(s, '/', 1);
    assertEquals(1, result.length);
    assertEquals("Lorem", result[0]);

    s = "Lorem";
    result = StringUtils.split(s, '/', 1);
    assertEquals(1, result.length);
    assertEquals("Lorem", result[0]);

    s = "";
    result = StringUtils.split(s, '/', 1);
    assertEquals(1, result.length);

    s = null;
    result = StringUtils.split(s, '/', 1);
    assertEquals(1, result.length);
  }

  @Test
  public void testSha1Hash() throws Exception {
    String hash = StringUtils.sha1Hash("Lorem");
    assertEquals("a246fbb4b1fb7f249c1c5496f46d3b54103ad85d", hash);
  }

  @Test
  public void testAddString() {
    String[] a = {"Lorem"};
    String v = "ipsum";

    String[] result = StringUtils.addString(a, v);
    assertEquals(2, result.length);
    assertEquals("Lorem", result[0]);
    assertEquals("ipsum", result[1]);
    // Try adding the same string again and verify it doesn't change anything
    result = StringUtils.addString(result, v);
    assertEquals(2, result.length);
    assertEquals("Lorem", result[0]);
    assertEquals("ipsum", result[1]);
  }

  @Test
  public void testRemoveString() {
    String[] a = {"Lorem", "ipsum"};
    String v = "ipsum";

    String[] result = StringUtils.removeString(a, v);
    assertEquals(1, result.length);
    assertEquals("Lorem", result[0]);
    // Try removing a string that isn't there and verify it doesn't change
    // anything
    result = StringUtils.removeString(result, v);
    assertEquals(1, result.length);
    assertEquals("Lorem", result[0]);
  }

  @Test
  public void testIsEmpty() {
    assertTrue(StringUtils.isEmpty(""));
    assertTrue(StringUtils.isEmpty(null));
    assertFalse(StringUtils.isEmpty("Lorem"));
  }

  @Test
  public void testEscapeJCRSQL() {
    String query = "'Lorem' \"ipsum\"";
    assertEquals("''Lorem'' \\\"ipsum\\\"", StringUtils.escapeJCRSQL(query));
  }

  @Test
  public void testStripBlanks() {
    assertEquals("Loremipsumdolor", StringUtils.stripBlanks(" Lorem ipsum dolor "));
    assertEquals("Lorem", StringUtils.stripBlanks("Lorem"));
  }

  @Test
  public void testJoin() {
    String[] elements = new String[] {"foo", "bar"};
    assertEquals("/foo/bar", StringUtils.join(elements, 0, '/'));
    elements = new String[] {};
    assertEquals(",", StringUtils.join(elements, 0, ','));
  }
}
