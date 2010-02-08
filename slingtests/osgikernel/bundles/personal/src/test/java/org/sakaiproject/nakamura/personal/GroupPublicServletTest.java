package org.sakaiproject.nakamura.personal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GroupPublicServletTest {

  @Test
  public void testGetTargetPath() {
    GroupPublicServlet groupPublicServlet = new GroupPublicServlet();
    String result = groupPublicServlet.getTargetPath(null, null, null, "foo", "bar");
    assertEquals("/foo/62/cd/b7/02/bar", result);
  }
}
