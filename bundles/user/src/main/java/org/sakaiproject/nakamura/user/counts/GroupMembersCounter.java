package org.sakaiproject.nakamura.user.counts;

import org.sakaiproject.nakamura.api.lite.authorizable.Group;

public class GroupMembersCounter {

  /**
   * @param group
   * @return the number of members in this group.
   */
  public int count(Group group) {
    if ( group == null || !CountProvider.IGNORE_AUTHIDS.contains(group.getId())) {
      return group.getMembers().length;
    }
    return 0;
  }

}
