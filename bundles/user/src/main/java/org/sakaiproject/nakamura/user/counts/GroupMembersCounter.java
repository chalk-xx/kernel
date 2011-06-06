package org.sakaiproject.nakamura.user.counts;

import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupMembersCounter {
  static final String PSEUDOGROUP = "sakai:pseudoGroup";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(GroupMembersCountChangeListener.class);

  /**
   * @param group
   * @return the number of members in this group.
   */
  public int count(Group group, AuthorizableManager authMgr) {
    if ( group != null && !CountProvider.IGNORE_AUTHIDS.contains(group.getId())) {
      return countMembers(group.getMembers(), authMgr);
    }
    return 0;
  }

  private int countMembers(String[] members, AuthorizableManager authMgr) {
    int count = 0;
    for (String member : members) {
      try {
        Authorizable auth = authMgr.findAuthorizable(member);
        if (auth instanceof Group && "true".equals(auth.getProperty(PSEUDOGROUP))) {
          // only count the members in a pseudogroup; not the group itself
          count += countMembers(((Group) auth).getMembers(), authMgr);
        } else {
          // users and non-pseudo groups get counted as 1
          count++;
        }
      } catch (AccessDeniedException e) {
        LOGGER.error(e.getMessage(), e);
      } catch (StorageClientException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
    return count;
  }
}
