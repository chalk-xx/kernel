package org.sakaiproject.nakamura.user.counts;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;


@Component(componentAbstract=true)
public abstract class AbstractCountHandler {

  @Reference
  protected Repository repository;

  protected void inc(String id, String key) throws AccessDeniedException,
      StorageClientException {
    Session session = null;
    try {
      session = repository.loginAdministrative();
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      Authorizable au = authorizableManager.findAuthorizable(id);
      if (au != null) {
        au.setProperty(key, toInt(au.getProperty(key)) + 1);
        authorizableManager.updateAuthorizable(au);
      }
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  protected void dec(String id, String key) throws AccessDeniedException,
      StorageClientException {
    Session session = null;
    try {
      session = repository.loginAdministrative();
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      Authorizable au = authorizableManager.findAuthorizable(id);
      if (au != null) {
        int v = toInt(au.getProperty(key)) - 1;
        au.setProperty(key, v < 0 ? 0 : v);
        authorizableManager.updateAuthorizable(au);
      }
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  protected String dumpEvent(Event event) {
    StringBuilder sb = new StringBuilder();
    sb.append("Event ").append(event).append(" with properties");
    String[] propNames = event.getPropertyNames();
    for (int i = 0; i < propNames.length; i++) {
      String propName = propNames[i];
      sb.append(propName).append("=").append(event.getProperty(propName)).append(", ");
    }
    return sb.toString();
  }

  private int toInt(Object property) {
    if (property instanceof Integer) {
      return (Integer) property;
    }
    return 0;
  }
}
