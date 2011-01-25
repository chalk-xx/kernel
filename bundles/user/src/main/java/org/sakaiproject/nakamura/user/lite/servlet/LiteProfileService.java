package org.sakaiproject.nakamura.user.lite.servlet;

import org.apache.sling.api.resource.ValueMap;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;


public interface LiteProfileService {

  ValueMap getProfileMap(Authorizable au, Session session);

  ValueMap getCompactProfileMap(Authorizable au, Session session);

}
