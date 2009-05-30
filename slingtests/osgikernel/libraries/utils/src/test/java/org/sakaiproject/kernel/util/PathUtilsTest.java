package org.sakaiproject.kernel.util;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

public class PathUtilsTest {

  @Test
  public void testGetUserPrefix() {
    assertEquals("61/51/anon/", PathUtils.getUserPrefix("",2));
    assertNull(PathUtils.getUserPrefix(null,2));
    assertEquals("22/C6/Lorem/", PathUtils.getUserPrefix("Lorem",2));
    assertEquals("DA/3B/ipsum/", PathUtils.getUserPrefix("ipsum",2));
    assertEquals("90/8B/amet_/", PathUtils.getUserPrefix("amet.",2));
  }

  @Test
  public void testGetMessagePrefix() {
    Pattern prefixFormat = Pattern.compile("^\\d{4}/\\d{1,2}/$");
    assertTrue(prefixFormat.matcher(PathUtils.getMessagePrefix()).matches());
  }

  @Test
  public void testGetParentReference() {
    assertEquals("/Lorem/ipsum/dolor", PathUtils
        .getParentReference("/Lorem/ipsum/dolor/sit"));
    assertEquals("/", PathUtils.getParentReference("/Lorem/"));
    assertEquals("/", PathUtils.getParentReference("/"));
    assertEquals("/", PathUtils.getParentReference(""));
  }

  @Test
  public void testGetDatePrefix() {
    Pattern prefixFormat = Pattern
        .compile("^\\d{4}/\\d{1,2}/\\p{XDigit}{2}/\\p{XDigit}{2}/\\w+/$");
    String path = PathUtils.getDatePrefix("Lorem",2);
    assertTrue(path,prefixFormat.matcher(path).matches());
    assertTrue(path.endsWith("/22/C6/Lorem/"));
  }
  @Test
  public void testGetHashPrefix() {
    Pattern prefixFormat = Pattern
        .compile("^\\p{XDigit}{2}/\\p{XDigit}{2}/\\w+/$");
    String path = PathUtils.getHashedPrefix("Lorem",2);
    assertTrue(path,prefixFormat.matcher(path).matches());
    
    assertEquals("22/C6/Lorem/",path);
  }

  @Test
  public void testNormalizePath() {
    assertEquals("/Lorem/ipsum/dolor/sit", PathUtils
        .normalizePath("/Lorem//ipsum/dolor///sit"));
    assertEquals("/Lorem/ipsum", PathUtils.normalizePath("//Lorem/ipsum/"));
    assertEquals("/Lorem/ipsum", PathUtils.normalizePath("/Lorem/ipsum//"));
    assertEquals("/", PathUtils.normalizePath("/"));
    assertEquals("/Lorem", PathUtils.normalizePath("Lorem"));
    assertEquals("/", PathUtils.normalizePath(""));
  }
  
  
  @Test
  public void testRemoveFistElement() {
    assertEquals("/a/b/c", PathUtils.removeFirstElement("/x/a/b/c"));
    assertEquals("/a/b/c", PathUtils.removeFirstElement("x/a/b/c"));
    assertEquals("/a/b/c/", PathUtils.removeFirstElement("//x/a/b/c/"));
    assertEquals(null, PathUtils.removeFirstElement(null));
    assertEquals("/", PathUtils.removeFirstElement("/x/"));
    assertEquals("/", PathUtils.removeFirstElement("/"));
    assertEquals("", PathUtils.removeFirstElement(""));
  }
  
  @Test
  public void testRemoveLastElement() {
    assertEquals("/a/b/c", PathUtils.removeLastElement("/a/b/c/x/"));
    assertEquals("/a/b/c", PathUtils.removeLastElement("/a/b/c/x"));
    assertEquals("/a/b/c", PathUtils.removeLastElement("/a/b/c/x///"));
    assertEquals(null, PathUtils.removeLastElement(null));
    assertEquals("/", PathUtils.removeLastElement("/x/"));
    assertEquals("/", PathUtils.removeLastElement("/"));
    assertEquals("", PathUtils.removeLastElement(""));
  }
}
