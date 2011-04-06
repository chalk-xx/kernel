package org.sakaiproject.nakamura.user.lite.servlet;

import org.apache.sling.api.resource.ValueMap;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;


public interface LiteProfileService {

  ValueMap getProfileMap(Authorizable au, Session session);

  /**
   * Gets the compact profile map.
   *
   * @param au the authorizable
   * @param session the session
   * @return the compact profile map
   * @deprecated use BasicUserInfo#getProperties() instead
   */
  ValueMap getCompactProfileMap(Authorizable au, Session session);

}
