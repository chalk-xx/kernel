package org.sakaiproject.nakamura.user.counts;

import com.google.common.collect.ImmutableSet;

import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;

/**
 * Membership counter.
 */
public class GroupMembershipCounter {
  
  private static final Set<String> IGNORE_AUTHIDS = ImmutableSet.of(Group.EVERYONE);
  private static final int MAX_GROUP_COUNT = 5000; // to limit iteration through groups count to prevent any DOS attacks there
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupMembershipCounter.class);


  /**
   * Counts the Groups this authorizable is a member of excluding everyone. Includes group that the member is indirectly a memberOf
   * @param au the authorizable
   * @param authorizableManager
   * @return the number of unique groups the authorizable is a member of, including idirect and intermediage membership.
   * @throws AccessDeniedException
   * @throws StorageClientException
   */
  public int count(Authorizable au, AuthorizableManager authorizableManager) throws AccessDeniedException, StorageClientException {
    
    int count = 0;
    // code borrowed from LiteMeServlet to include indirect memberships
    // KERN-1831 changed from getPrincipals to memberOf to drill down list
    for (Iterator<Group> memberOf = au.memberOf(authorizableManager); memberOf.hasNext();) {
      Authorizable group = memberOf.next();
      if (group == null || !(group instanceof Group)
          || IGNORE_AUTHIDS.contains(group.getId())) {
        // we don't want to count the everyone groups
        continue;
      }
      if (group.hasProperty("sakai:managed-group")) {
        // fetch the group that the manager group manages
        Authorizable managedGroup = authorizableManager.findAuthorizable((String) group
            .getProperty("sakai:managed-group"));
        if (managedGroup == null || !(managedGroup instanceof Group)) {
          // dont count this group if the managed group doesnt exist. (ieb why ?, the users is still a member of this group even if the managed group doesnt exist)
          continue;
        }
      }
      count++;
      if (count >= MAX_GROUP_COUNT) {
        LOGGER.warn("getGroupsCount() has reached its maximum of {} check for reason, possible DOS issue?", new Object[]{MAX_GROUP_COUNT});
        return count;
      }
    }
    return count;
  }

}
