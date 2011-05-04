package org.sakaiproject.nakamura.profile;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@Component(inherit = true, label = "%sakai-event.name", immediate = true)
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Event Handler counting Group Membership ADDED and UPDATED Events."),
    @Property(name = "event.topics", value = {
        "org/sakaiproject/nakamura/lite/group/ADDED",
        "org/sakaiproject/nakamura/lite/group/UPDATED",
        "org/sakaiproject/nakamura/lite/group/updated",
        "org/sakaiproject/nakamura/lite/authorizables/ADDED",
        "org/sakaiproject/nakamura/lite/authorizables/UPDATED",
        "org/sakaiproject/nakamura/lite/content/UPDATED"}) })
        
//org/sakaiproject/nakamura/lite/authorizables/ADDED
//org/sakaiproject/nakamura/lite/authorizables/UPDATED
//org/sakaiproject/nakamura/lite/content/UPDATED
        
public class GroupMembershipCountChangeListener implements EventHandler {
  
  private static final Logger LOG = LoggerFactory.getLogger(GroupMembershipCountChangeListener.class);
  
  public void handleEvent(Event event) {
    event.toString();
    if (LOG.isDebugEnabled()) LOG.debug("handleEvent() event: {} with properties: {}", new Object[]{event, Arrays.toString(event.getPropertyNames())});

  }

}
