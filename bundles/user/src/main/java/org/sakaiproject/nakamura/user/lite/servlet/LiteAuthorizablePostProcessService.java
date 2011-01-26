package org.sakaiproject.nakamura.user.lite.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;

public interface LiteAuthorizablePostProcessService {

  void process(Authorizable user, Session selfRegSession, ModificationType create,
      SlingHttpServletRequest request) throws Exception;

}
