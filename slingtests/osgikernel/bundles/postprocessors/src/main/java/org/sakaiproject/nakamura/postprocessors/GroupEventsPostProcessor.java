package org.sakaiproject.nakamura.postprocessors;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.nyu.XythosRemote;

@Component(enabled = true, immediate = true, metatype = true)
@Service(value = EventHandler.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "New York University"),
    @Property(name = "service.description", value = "Provides a place to respond when sites are created and memberships updated"),
    @Property(name = "event.topics", value = "org/sakaiproject/nakamura/api/site/event/create") })
public class GroupEventsPostProcessor implements EventHandler {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupEventsPostProcessor.class);
  
  @Reference
  XythosRemote xythosService;
  
  public void handleEvent(Event event) {
    String sitePath = (String) event.getProperty("sitePath");
    sitePath = sitePath.replaceFirst("\\/sites\\/", "\\/alex3\\/");
    String userId = (String) event.getProperty("userId");
    try {
      xythosService.createGroup(sitePath, userId);
    } catch (Exception e1) {
      LOGGER.warn("failed to create Xythos group when creating site: " + e1.getMessage());
    }
  }

}
