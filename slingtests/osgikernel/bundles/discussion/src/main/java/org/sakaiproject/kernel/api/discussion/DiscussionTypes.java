package org.sakaiproject.kernel.api.discussion;

public enum DiscussionTypes {
  discussion, comment;

  public static boolean hasValue(String s) {
    DiscussionTypes[] types = values();
    for (DiscussionTypes t : types) {
      if (t.name().equalsIgnoreCase(s)) {
        return true;
      }
    }
    return false;
  }
}
