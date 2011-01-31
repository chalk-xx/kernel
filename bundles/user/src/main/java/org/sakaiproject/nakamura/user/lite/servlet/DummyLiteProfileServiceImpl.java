package org.sakaiproject.nakamura.user.lite.servlet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;

@Component(enabled=false, immediate=true, metatype=true)
@Service(value=LiteProfileService.class)
public class DummyLiteProfileServiceImpl implements LiteProfileService {

  public ValueMap getProfileMap(Authorizable au, Session session) {
    return new ValueMapDecorator(au.getSafeProperties());
  }

  public ValueMap getCompactProfileMap(Authorizable au, Session session) {
    return new ValueMapDecorator(au.getSafeProperties());
  }

}
