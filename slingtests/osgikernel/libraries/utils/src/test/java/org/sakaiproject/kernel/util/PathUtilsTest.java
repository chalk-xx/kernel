package org.sakaiproject.kernel.util;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

public class PathUtilsTest {

  
  @Test
  public void toInternalHash() {
   assertEquals("/testing/d0/33/e2/2a/admin/.extra", PathUtils.toInternalHashedPath("/testing", "admin", ".extra"));
   assertEquals("/testing/0a/92/fa/b3/anonymous/.extra", PathUtils.toInternalHashedPath("/testing", "anonymous", ".extra"));
  }
  @Test
  public void testGetUserPrefix() {
    assertEquals("61/51/anon/", PathUtils.getUserPrefix("",2));
    assertNull(PathUtils.getUserPrefix(null,2));
    assertEquals("22/c6/Lorem/", PathUtils.getUserPrefix("Lorem",2));
    assertEquals("da/3b/ipsum/", PathUtils.getUserPrefix("ipsum",2));
    assertEquals("90/8b/amet_/", PathUtils.getUserPrefix("amet.",2));
  }

  @Test
  public void testGetMessagePrefix() {
    Pattern prefixFormat = Pattern.compile("^/\\d{4}/\\d{1,2}/$");
    assertTrue(prefixFormat.matcher(PathUtils.getMessagePath()).matches());
  }

  @Test
  public void testGetParentReference() {
    assertEquals("/Lorem/ipsum/dolor", PathUtils
        .getParentReference("/Lorem/ipsum/dolor/sit"));
    assertEquals("/Lorem/ipsum", PathUtils
        .getParentReference("/Lorem/ipsum/dolor/"));
    assertEquals("/Lorem/ipsum", PathUtils
        .getParentReference("/Lorem/ipsum/dolor"));
    assertEquals("/", PathUtils.getParentReference("/Lorem/"));
    assertEquals("/", PathUtils.getParentReference("/"));
    assertEquals("/", PathUtils.getParentReference(""));
  }

  @Test
  public void testGetDatePrefix() {
    Pattern prefixFormat = Pattern
        .compile("^/\\d{4}/\\d{1,2}/\\p{XDigit}{2}/\\p{XDigit}{2}/\\w+/$");
    String path = PathUtils.getDatePath("Lorem",2);
    assertTrue(path,prefixFormat.matcher(path).matches());
    assertTrue(path.endsWith("/22/c6/Lorem/"));
  }
  @Test
  public void testGetHashPrefix() {
    Pattern prefixFormat = Pattern
        .compile("^/\\p{XDigit}{2}/\\p{XDigit}{2}/\\w+/$");
    String path = PathUtils.getHashedPath("Lorem",2);
    assertTrue(path,prefixFormat.matcher(path).matches());
    
    assertEquals("/22/c6/Lorem/",path);
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
  
  
  @Test
  public void testNodePatParts() {
    assertArrayEquals(new String[]{"/a/b/c/x/",""},PathUtils.getNodePathParts("/a/b/c/x/"));
    assertArrayEquals(new String[]{"/a/b/c/x",""},PathUtils.getNodePathParts("/a/b/c/x"));
    assertArrayEquals(new String[]{"/a.a/b/c/x/",""},PathUtils.getNodePathParts("/a.a/b/c/x/"));
    assertArrayEquals(new String[]{"/aaa.a/b/c/x",""},PathUtils.getNodePathParts("/aaa.a/b/c/x"));
    assertArrayEquals(new String[]{"/aaa.aa.aa/b/c/x",""},PathUtils.getNodePathParts("/aaa.aa.aa/b/c/x"));
    assertArrayEquals(new String[]{"aaa.aa.aa/b/c/x",""},PathUtils.getNodePathParts("aaa.aa.aa/b/c/x"));
    assertArrayEquals(new String[]{"aaa.aa.aa/b/c/xxxx",".x"},PathUtils.getNodePathParts("aaa.aa.aa/b/c/xxxx.x"));
    assertArrayEquals(new String[]{"aaa.aa.aa/b/c/xxxx",".x.a"},PathUtils.getNodePathParts("aaa.aa.aa/b/c/xxxx.x.a"));
    assertArrayEquals(new String[]{"aaa.aa.aa/b/c/xxxx.x.a/",""},PathUtils.getNodePathParts("aaa.aa.aa/b/c/xxxx.x.a/"));
    assertArrayEquals(new String[]{"",""},PathUtils.getNodePathParts(""));
    assertArrayEquals(new String[]{"/",""},PathUtils.getNodePathParts("/"));
    assertArrayEquals(new String[]{"/",".aaa"},PathUtils.getNodePathParts("/.aaa"));
    assertArrayEquals(new String[]{"/",".a.aa"},PathUtils.getNodePathParts("/.a.aa"));
    assertArrayEquals(new String[]{"",".aaa"},PathUtils.getNodePathParts(".aaa"));
  }

  @Test
  public void testLastElement() { 
    assertEquals("",PathUtils.lastElement("/a/b/c/x/"));
    assertEquals("x",PathUtils.lastElement("/a/b/c/x"));
    assertEquals("",PathUtils.lastElement("/a.a/b/c/x/"));
    assertEquals("x",PathUtils.lastElement("/aaa.a/b/c/x"));
    assertEquals("x",PathUtils.lastElement("/aaa.aa.aa/b/c/x"));
    assertEquals("x",PathUtils.lastElement("aaa.aa.aa/b/c/x"));
    assertEquals("xxxx",PathUtils.lastElement("aaa.aa.aa/b/c/xxxx.x"));
    assertEquals("xxxx",PathUtils.lastElement("aaa.aa.aa/b/c/xxxx.x.a"));
    assertEquals("",PathUtils.lastElement("aaa.aa.aa/b/c/xxxx.x.a/"));
    assertEquals("",PathUtils.lastElement(""));
    assertEquals("",PathUtils.lastElement("/"));
    assertEquals("",PathUtils.lastElement("/.aaa"));
    assertEquals("",PathUtils.lastElement("/.a.aa"));
    assertEquals("",PathUtils.lastElement(".aaa"));
  }

}
