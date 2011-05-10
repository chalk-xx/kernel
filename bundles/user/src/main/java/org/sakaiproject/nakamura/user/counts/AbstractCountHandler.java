package org.sakaiproject.nakamura.user.counts;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;


@Component(componentAbstract=true)
public abstract class AbstractCountHandler {

  @Reference
  private Repository repository;

  private Session adminSession;

  protected AuthorizableManager authorizableManager;

  protected ContentManager contentManager;

  

  protected void inc(String id, String key) throws AccessDeniedException,
      StorageClientException {
    Authorizable au = authorizableManager.findAuthorizable(id);
    if (au != null) {
      au.setProperty(key, toInt(au.getProperty(key)) + 1);
      authorizableManager.updateAuthorizable(au);
    }
  }

  protected void dec(String id, String key) throws AccessDeniedException,
      StorageClientException {
    Authorizable au = authorizableManager.findAuthorizable(id);
    if (au != null) {
      int v = toInt(au.getProperty(key)) - 1;
      au.setProperty(key, v < 0 ? 0 : v);
      authorizableManager.updateAuthorizable(au);
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
  
  // ---------- SCR integration ---------------------------------------------
  @Activate
  public void activate(ComponentContext componentContext) throws StorageClientException,
      AccessDeniedException {
    adminSession = repository.loginAdministrative();
    authorizableManager = adminSession.getAuthorizableManager();
    contentManager = adminSession.getContentManager();
  }

  @Deactivate
  protected void deactivate(ComponentContext ctx) throws ClientPoolException {
    authorizableManager = null;
    contentManager = null;
    adminSession.logout();
    adminSession = null;

  }


}
