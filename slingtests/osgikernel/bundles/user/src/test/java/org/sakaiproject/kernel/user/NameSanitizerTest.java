package org.sakaiproject.kernel.user;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import javax.jcr.RepositoryException;

public class NameSanitizerTest {

  @Test
  public void testValidGroup() {
    String name = "g-mygroup-foo";
    boolean result = testName(name, false);
    assertEquals(name + " is a correct name. This should pass.", true, result);
  }

  @Test
  public void testInvalidGroup() {
    String name = "mygroup-foo";
    boolean result = testName(name, false);
    assertEquals(name + " is an incorrect name. This should fail.", false, result);
  }
  
  @Test
  public void testInvalidCharacters() {
    String name = "g%2Dbob";
    boolean result = testName(name, true);
    assertEquals(name + " is an incorrect name. This should fail.", false, result);
  }
  
  @Test
  public void testInvalidCharactersGroup() {
    String name = "g-foo%$£bar";
    boolean result = testName(name, true);
    assertEquals(name + " is an incorrect name. This should fail.", false, result);
  }
  
  private boolean testName(String name, boolean isUser) {
    NameSanitizer san = new NameSanitizer(name, isUser);
    try {
      san.validate();
      return true;
    } catch (RepositoryException e) {
      return false;
    }
  }
}
