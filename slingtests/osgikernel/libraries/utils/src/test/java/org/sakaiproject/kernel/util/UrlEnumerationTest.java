package org.sakaiproject.kernel.util;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.Enumeration;

import org.junit.Test;

public class UrlEnumerationTest {

  @Test
  public void testUrlEnumeration() throws Exception {
    Enumeration<URL> eu = new UrlEnumeration(new URL("http://example.com"));
    assertTrue(eu.hasMoreElements());
    assertEquals(new URL("http://example.com"), eu.nextElement());
    assertFalse(eu.hasMoreElements());
    assertNull(eu.nextElement());
  }
}
