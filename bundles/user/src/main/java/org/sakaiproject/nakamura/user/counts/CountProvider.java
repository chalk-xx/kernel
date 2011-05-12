package org.sakaiproject.nakamura.user.counts;

import com.google.common.collect.ImmutableSet;

import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;

import java.util.Set;


/**
 * Provides counts of various entities assocaited with a user, 
 * e.g. number of groups user is a member of, number of contacts has user has, the number of content items a user owns or can view
 */
public interface CountProvider {

  /**
   * Set of AuthIDs that are excluded from counting. Some because their properties are immutable (everyone), some because they make no sense (anon)
   */
  public static final Set<String> IGNORE_AUTHIDS = ImmutableSet.of(Group.EVERYONE, User.ANON_USER, User.ADMIN_USER);

  /**
   * get total counts for group memberships, contacts and content items
   * @param au the authorizable, may be modified by the update operation.
   * @param session
   * @throws AccessDeniedException
   * @throws StorageClientException
   */
  public void update(Authorizable au) throws AccessDeniedException, StorageClientException;
  
  /**
   * are the counts null or too old
   * @param authorizable
   * @return
   * @throws AccessDeniedException
   * @throws StorageClientException
   */
  public boolean needsRefresh(Authorizable authorizable) throws AccessDeniedException, StorageClientException;
  

}
