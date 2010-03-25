package org.sakaiproject.nakamura.docproxy.url;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class HttpAnyMethodTest {

  @Test
  public void basic() throws Exception {
    HttpAnyMethod method = new HttpAnyMethod("GET", "http://localhost/file");
    assertEquals("http://localhost/file", method.getURI().toString());
    assertEquals("GET", method.getName());
  }

}
