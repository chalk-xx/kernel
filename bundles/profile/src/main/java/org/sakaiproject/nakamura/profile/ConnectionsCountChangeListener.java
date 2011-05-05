package org.sakaiproject.nakamura.profile;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StoreListener;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.profile.CountProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype=true, immediate = true, inherit=true)
@Service(value=EventHandler.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Event Handler counting Group Membership ADDED and UPDATED Events."),
    @Property(name = "event.topics", value = {
        "org/sakaiproject/nakamura/lite/group/ADDED",
        "org/sakaiproject/nakamura/lite/group/UPDATED"}) })
        
public class ConnectionsCountChangeListener extends AbstractCountHandler implements EventHandler {
  
  private static final Logger LOG = LoggerFactory.getLogger(ConnectionsCountChangeListener.class);

  public void handleEvent(Event event) {
    try {
      String path = (String) event.getProperty(StoreListener.PATH_PROPERTY);
      if ( path.startsWith("g-contacts-")) {
        // contacts are
        String userId = path.substring("g-contacts-".length());
        Authorizable user = authorizableManager.findAuthorizable(userId);
        Authorizable contactsGroup = authorizableManager.findAuthorizable(path);
        if ( user != null && contactsGroup instanceof Group ) {
          user.setProperty(CountProvider.CONTACTS_PROP, ((Group) contactsGroup).getMembers().length);
          authorizableManager.updateAuthorizable(user);
        }        
      }
    } catch (StorageClientException e) {
      LOG.debug("Failed to update count ", e);
    } catch (AccessDeniedException e) {
      LOG.debug("Failed to update count ", e);
    }
  }
  
  
  
}
