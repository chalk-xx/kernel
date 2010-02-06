package org.sakaiproject.nakamura.util;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class MapUtilsTest {

  @SuppressWarnings("unchecked")
  @Test
  public void testConvertToImmutableMap() {
    String s = "Lorem=ipsum; dolor = sit ;amet=.";
    Map<String, String> m = MapUtils.convertToImmutableMap(s);
    assertTrue(m instanceof ImmutableMap);
    assertEquals("ipsum", m.get("Lorem"));
    assertEquals("sit", m.get("dolor"));
    assertEquals(".", m.get("amet"));
  }
}
