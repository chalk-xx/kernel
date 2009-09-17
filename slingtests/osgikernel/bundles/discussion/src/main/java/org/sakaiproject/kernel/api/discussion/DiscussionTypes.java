package org.sakaiproject.kernel.api.discussion;

public enum DiscussionTypes {
  discussion, comment;

  // TODO: IMHO, hasValue should not do things like this. We have an enum, and we are
  // encouraging abuse of the enum since Discssion DiScUsSiOn are all ok, but wont be
  // searchable, and can't be coerced into the DiscussionType I would rather using     
  // DiscussionTypes.valueOf(string) and catch 
  // the InvalidArgumentException 

  
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
