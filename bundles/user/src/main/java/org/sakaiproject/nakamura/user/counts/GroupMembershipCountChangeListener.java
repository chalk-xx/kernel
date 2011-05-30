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
        
public class GroupMembershipCountChangeListener extends AbstractCountHandler implements EventHandler {
  
  private static final Logger LOG = LoggerFactory.getLogger(GroupMembershipCountChangeListener.class);
  private GroupMembershipCounter groupMembershipCounter = new GroupMembershipCounter();

  public void handleEvent(Event event) {
    Session session = null;
    try {
      LOG.debug("handleEvent() " + dumpEvent(event));
      // The members of a group are defined in the membership, so simply use that value, no need to increment or decrement.
      String groupId = (String) event.getProperty(StoreListener.PATH_PROPERTY);
      if ( !CountProvider.IGNORE_AUTHIDS.contains(groupId)) {
        session = repository.loginAdministrative();
        AuthorizableManager authMgr = session.getAuthorizableManager();
        Authorizable au = authMgr.findAuthorizable(groupId);
        if ( au != null ) {
          int n = groupMembershipCounter.count(au, authMgr);
          Integer v = (Integer) au.getProperty(UserConstants.GROUP_MEMBERSHIPS_PROP);
          if ( v == null || n != v.intValue()) {
            au.setProperty(UserConstants.GROUP_MEMBERSHIPS_PROP, n);
            authMgr.updateAuthorizable(au);
          }
        }
      }
    } catch (StorageClientException e) {
      LOG.error("Failed to update count ", e);
    } catch (AccessDeniedException e) {
      LOG.error("Failed to update count ", e);
    } finally {
      if (session != null) {
        try {
          session.logout();
        } catch (ClientPoolException e) {
          LOG.warn(e.getMessage());
        }
      }
    }
  }
}

