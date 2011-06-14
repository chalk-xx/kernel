package org.sakaiproject.nakamura.user.counts;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.StoreListener;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

@Component(metatype=true, immediate = true, inherit=true)
@Service(value=EventHandler.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Event Handler counting Group Membership ADDED and UPDATED Events."),
    @Property(name = "event.topics", value = {
        "org/sakaiproject/nakamura/lite/content/ADDED",
        "org/sakaiproject/nakamura/lite/content/UPDATED",
        "org/sakaiproject/nakamura/lite/content/DELETE"}) })
        
public class ContentCountChangeListener extends AbstractCountHandler implements EventHandler {
  
  private static final Logger LOG = LoggerFactory.getLogger(ContentCountChangeListener.class);

  public void handleEvent(Event event) {
    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative();
      ContentManager contentManager = adminSession.getContentManager();
      LOG.debug("handleEvent() " + dumpEvent(event));
      // The members of a group are defined in the membership, so simply use that value, no need to increment or decrement.
      String path = (String) event.getProperty(StoreListener.PATH_PROPERTY);
      Content content = contentManager.get(path);        
      @SuppressWarnings("unchecked")
      Map<String, Object> beforeEvent = (Map<String, Object>) event.getProperty(StoreListener.BEFORE_EVENT_PROPERTY);
      // content will be null when listening to DELETE topic as it has been deleted before reaching here
      String resourceType = null;
      if (content != null) {
        resourceType = content.hasProperty("sling:resourceType") ? (String)content.getProperty("sling:resourceType") : null;
      }
      else if (beforeEvent != null) {
        resourceType = beforeEvent.containsKey("sling:resourceType") ? (String)beforeEvent.get("sling:resourceType") : null;
      }
      if ( "sakai/pooled-content".equals(resourceType) ) {
        // this either is or was a content node.
        if ( beforeEvent != null && content != null) {
          Set<String> before = Sets.newHashSet();
          before.addAll(ImmutableList.of(StorageClientUtils.nonNullStringArray((String[])beforeEvent.get("sakai:pooled-content-viewer"))));
          before.addAll(ImmutableList.of(StorageClientUtils.nonNullStringArray((String[])beforeEvent.get("sakai:pooled-content-manager"))));
          Set<String> after = Sets.newHashSet(StorageClientUtils.nonNullStringArray((String[])content.getProperty("sakai:pooled-content-viewer")));
          after.addAll(ImmutableList.of(StorageClientUtils.nonNullStringArray((String[])content.getProperty("sakai:pooled-content-manager"))));
          before = Sets.difference(before, CountProvider.IGNORE_AUTHIDS);
          after = Sets.difference(after, CountProvider.IGNORE_AUTHIDS);
          Set<String> removed = Sets.difference(before,after);
          Set<String> added = Sets.difference(after, before);
          LOG.info("Path:{} Before:{} After:{} Added:{} Removed:{} ",new Object[]{path, before, after, added, removed});
          for ( String userId : added ) {
            if ( !CountProvider.IGNORE_AUTHIDS.contains(userId) ) {
              inc(userId, UserConstants.CONTENT_ITEMS_PROP);
            }
          }
          for ( String userId : removed ) {
            if ( !CountProvider.IGNORE_AUTHIDS.contains(userId) ) {
              dec(userId, UserConstants.CONTENT_ITEMS_PROP);
            }
          }
        } // we're in a DELETE topic where content is null because it has been deleted already and removed is just the users in the beforeEvent
        else if ("org/sakaiproject/nakamura/lite/content/DELETE".equals(event.getTopic()) && beforeEvent != null) { 
          Set<String> removed = Sets.newHashSet(StorageClientUtils.nonNullStringArray((String[])beforeEvent.get("sakai:pooled-content-viewer")));
          removed.addAll(ImmutableList.of(StorageClientUtils.nonNullStringArray((String[])beforeEvent.get("sakai:pooled-content-manager")))); 
          for ( String userId : removed ) {
            if ( !CountProvider.IGNORE_AUTHIDS.contains(userId) ) {
              dec(userId, UserConstants.CONTENT_ITEMS_PROP);
            }
          }
        }
      }
    } catch (StorageClientException e) {
      LOG.debug("Failed to update count ", e);
    } catch (AccessDeniedException e) {
      LOG.debug("Failed to update count ", e);
    }
  }
}
