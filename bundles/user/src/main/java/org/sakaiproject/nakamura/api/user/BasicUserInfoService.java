package org.sakaiproject.nakamura.api.user;

import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;

import java.util.Map;


public interface BasicUserInfoService {

  Map<String, Object> getProperties(Authorizable au);

  String[] getBasicProfileElements();

}
