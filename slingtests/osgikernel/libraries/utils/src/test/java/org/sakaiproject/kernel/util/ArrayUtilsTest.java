package org.sakaiproject.kernel.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class ArrayUtilsTest {

  @Test
  public void testCopy() {
    String[] from = { "Lorem", "ipsum", "dolor", "sit" };
    String[] to = new String[from.length];
    ArrayUtils.copy(from, to);
    // We should have two different objects
    assertFalse(from == to);
    // Every entry in both arrays should match
    for (int i = 0; i < to.length; i++) {
      assertTrue(from[i].equals(to[i]));
    }
  }
}
