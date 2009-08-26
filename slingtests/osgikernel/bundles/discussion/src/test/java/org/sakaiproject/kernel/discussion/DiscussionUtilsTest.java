package org.sakaiproject.kernel.discussion;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.sakaiproject.kernel.api.discussion.DiscussionUtils;

public class DiscussionUtilsTest {

  private static final String POST = "6a54e2ecc61aa419f87559e74db29a38ac153ad0";
  private static final String STORE = "/test/store";
  private static final String FULL = "/test/store/ef/35/29/ed/6a54e2ecc61aa419f87559e74db29a38ac153ad0";
  
  @Test
  public void testPath() {
    assertEquals(FULL, DiscussionUtils.getFullPostPath(STORE, POST));
  }
  
  @Test
  public void TestPathWithSelector() {
    String sel = ".reply.html";
    assertEquals(FULL + sel, DiscussionUtils.getFullPostPath(STORE, POST) + sel);
  }
}
