package org.sakaiproject.nakamura.user.counts;

import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;

public class ConnectionsCounter {

  public int count(Authorizable au, AuthorizableManager authorizableManager) throws AccessDeniedException, StorageClientException {
    // find the number of contacts in the current users store that are in state Accepted,
    // invited or pending.
    String userID = au.getId();
    Authorizable g = authorizableManager.findAuthorizable("g-contacts-" + userID);
    if (g instanceof Group) {
      return ((Group) g).getMembers().length;
    }
    return 0;
  }

}
