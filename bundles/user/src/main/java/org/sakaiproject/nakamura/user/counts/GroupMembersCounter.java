package org.sakaiproject.nakamura.user.counts;

import org.sakaiproject.nakamura.api.lite.authorizable.Group;

public class GroupMembersCounter {

  /**
   * @param group
   * @return the number of members in this group.
   */
  public int count(Group group) {
    return group.getMembers().length;
  }

}
