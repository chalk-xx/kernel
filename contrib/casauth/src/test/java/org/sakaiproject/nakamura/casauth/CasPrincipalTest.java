package org.sakaiproject.nakamura.casauth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CasPrincipalTest {

  @Test
  public void testGetName() {
    CasPrincipal cp = new CasPrincipal("foo");
    assertEquals("foo", cp.getName());
  }
}
