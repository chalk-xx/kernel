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
import org.sakaiproject.nakamura.api.lite.authorizable.User;
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
        
public class GroupMembersCountChangeListener extends AbstractCountHandler implements EventHandler {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupMembersCountChangeListener.class);
  static final String PSEUDOGROUP_PARENT = "sakai:pseudogroupparent";

  private GroupMembersCounter groupMembersCounter = new GroupMembersCounter();

  public void handleEvent(Event event) {
    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative();
      AuthorizableManager authorizableManager = adminSession.getAuthorizableManager();
      LOGGER.debug("handleEvent() " + dumpEvent(event));
      // The members of a group are defined in the membership, so simply use that value, no need to increment or decrement.
      String groupId = (String) event.getProperty(StoreListener.PATH_PROPERTY);
      if ( !CountProvider.IGNORE_AUTHIDS.contains(groupId) ) {
        Authorizable au = authorizableManager.findAuthorizable(groupId);
        if ( au instanceof Group ) {
          if (au.hasProperty(PSEUDOGROUP_PARENT)) {
            String parent = String.valueOf(au.getProperty(PSEUDOGROUP_PARENT));
            au = authorizableManager.findAuthorizable(parent);
          }
          int n = groupMembersCounter.count((Group) au, authorizableManager);
          Integer v = (Integer) au.getProperty(UserConstants.GROUP_MEMBERS_PROP);
          if ( v == null || n != v.intValue()) {
            au.setProperty(UserConstants.GROUP_MEMBERS_PROP, n);
            authorizableManager.updateAuthorizable(au);
          }
        }
        else if (au instanceof User) {
          String userId = (String) event.getProperty(StoreListener.PATH_PROPERTY);
          if (LOGGER.isDebugEnabled()) LOGGER.debug("got User event for " + userId);
        }
      }
    } catch (StorageClientException e) {
      LOGGER.debug("Failed to update count ", e);
    } catch (AccessDeniedException e) {
      LOGGER.debug("Failed to update count ", e);
    } finally {
      if ( adminSession != null ) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOGGER.warn(e.getMessage(),e);
        }
      }
    }
  }
}