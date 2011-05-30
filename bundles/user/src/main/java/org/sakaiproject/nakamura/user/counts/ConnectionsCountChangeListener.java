package org.sakaiproject.nakamura.user.counts;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StoreListener;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype=true, immediate = true, inherit=true)
@Service(value=EventHandler.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Event Handler counting Group Membership ADDED and UPDATED Events."),
    @Property(name = "event.topics", value = {
        "org/sakaiproject/nakamura/lite/authorizables/ADDED",
        "org/sakaiproject/nakamura/lite/authorizables/UPDATED"}) })
        
public class ConnectionsCountChangeListener extends AbstractCountHandler implements EventHandler {
  
  private static final Logger LOG = LoggerFactory.getLogger(ConnectionsCountChangeListener.class);
  private ConnectionsCounter connectionsCounter = new ConnectionsCounter();

  public void handleEvent(Event event) {
    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative();
      AuthorizableManager authorizableManager = adminSession.getAuthorizableManager();
      if (LOG.isDebugEnabled()) LOG.debug("handleEvent() " + dumpEvent(event));
      String path = (String) event.getProperty(StoreListener.PATH_PROPERTY);
      if ( path.startsWith("g-contacts-")) {
        // contacts are
        String userId = path.substring("g-contacts-".length());
        Authorizable user = authorizableManager.findAuthorizable(userId);
        Authorizable contactsGroup = authorizableManager.findAuthorizable(path);
        if ( user != null && contactsGroup instanceof Group ) {
          int n = connectionsCounter.count(user, authorizableManager);
          Integer v = (Integer) user.getProperty(UserConstants.CONTACTS_PROP);
          if ( v == null || n != v.intValue()) {
            user.setProperty(UserConstants.CONTACTS_PROP, n);
            authorizableManager.updateAuthorizable(user);
          }
        } else {
          LOG.error("Failed to locate User ID from {} ",path);
        }
      }
    } catch (StorageClientException e) {
      LOG.debug("Failed to update count ", e);
    } catch (AccessDeniedException e) {
      LOG.debug("Failed to update count ", e);
    } finally {
      if ( adminSession != null ) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOG.warn(e.getMessage(),e);
        }
      }
    }
  }
  
  
  
}
