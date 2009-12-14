package org.sakaiproject.kernel.user;

import javax.jcr.RepositoryException;

public class NameSanitizer {

  private String name;
  private boolean isUser;

  public NameSanitizer(String name, boolean isUser) {
    this.name = name;
    this.isUser = isUser;
  }

  public void validate() throws RepositoryException {
    String name = this.name;

    // User names can't start with g-
    if (isUser && name.startsWith("g-")) {
      throw new RepositoryException("User name must not begin 'g-'");
    }

    // Group names HAVE to start with a g-
    if (!isUser && !name.startsWith("g-")) {
      throw new RepositoryException("Group names must begin with 'g-'");
    }

    if (!isUser && name.startsWith("g-")) {
      name = name.substring(2);
    }

    // At least 3 chars.
    if (name.length() < 3) {
      throw new RepositoryException("Name must be bigger than 3 chars.");
    }

    // KERN-271
    // Only allowing 0-9a-Z-_
    char[] chars = name.toCharArray();
    for (char ch : chars) {
      if (!Character.isLetterOrDigit(ch) && '_' != ch && '-' != ch) {
        throw new RepositoryException("Invalid characters in name: " + name);
      }
    }
  }
}
