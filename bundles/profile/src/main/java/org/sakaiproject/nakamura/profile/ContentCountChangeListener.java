package org.sakaiproject.nakamura.profile;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StoreListener;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.profile.CountProvider;
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
        "org/sakaiproject/nakamura/lite/content/UPDATED"}) })
        
public class ContentCountChangeListener extends AbstractCountHandler implements EventHandler {
  
  private static final Logger LOG = LoggerFactory.getLogger(ContentCountChangeListener.class);

  public void handleEvent(Event event) {
    try {
      // The members of a group are defined in the membership, so simply use that value, no need to increment or decrement.
      String path = (String) event.getProperty(StoreListener.PATH_PROPERTY);
      Content content = contentManager.get(path);
      if ( content.hasProperty("sling:resourceType") && "sakai/pooled-content".equals(content.getProperty("sling:resourceType")) ) {
        // this is a content node.
        @SuppressWarnings("unchecked")
        Map<String, Object> beforeEvent = (Map<String, Object>) event.getProperty(StoreListener.BEFORE_EVENT_PROPERTY);
        Set<String> before = Sets.newHashSet((String[])beforeEvent.get("sakai:pooled-content-viewer"));
        before.addAll(ImmutableList.of((String[])beforeEvent.get("sakai:pooled-content-manager")));
        Set<String> after = Sets.newHashSet((String[])content.getProperty("sakai:pooled-content-viewer"));
        after.addAll(ImmutableList.of((String[])content.getProperty("sakai:pooled-content-manager")));
        Set<String> removed = Sets.difference(before,after);
        Set<String> added = Sets.difference(after, before);
        for ( String userId : added ) {
          inc(userId, CountProvider.CONTENT_ITEMS_PROP);
        }
        for ( String userId : removed ) {
          dec(userId, CountProvider.CONTENT_ITEMS_PROP);
        }
      }
    } catch (StorageClientException e) {
      LOG.debug("Failed to update count ", e);
    } catch (AccessDeniedException e) {
      LOG.debug("Failed to update count ", e);
    }
  }
  
  
}
