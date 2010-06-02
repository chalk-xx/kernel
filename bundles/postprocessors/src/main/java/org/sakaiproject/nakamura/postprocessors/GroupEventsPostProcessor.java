package org.sakaiproject.nakamura.postprocessors;

import edu.nyu.XythosRemote;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.site.SiteService.SiteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component(enabled = true, immediate = true, metatype = true)
@Service(value = EventHandler.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "New York University"),
    @Property(name = "service.description", value = "Provides a place to respond when sites are created and memberships updated"),
    @Property(name = "event.topics", value = SiteService.SiteEvent.TOPIC + "created") })
// SiteService.SiteEvent.created.getTopic()
public class GroupEventsPostProcessor implements EventHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GroupEventsPostProcessor.class);

  public static final String DEFAULT_SITES_REPLACEMENT = "alex3";
  @Property(value = DEFAULT_SITES_REPLACEMENT)
  static final String SITES_REPLACEMENT = "sakai.sites.replacement";
  private String sitesReplacement;

  @Reference
  XythosRemote xythosService;

  @Activate
  protected void activate(Map<?, ?> props) {
    sitesReplacement = OsgiUtil.toString(SITES_REPLACEMENT, DEFAULT_SITES_REPLACEMENT);
  }

  public void handleEvent(Event event) {
    String sitePath = (String) event.getProperty(SiteEvent.SITE);
    sitePath = sitePath.replaceFirst("\\/sites\\/", "\\/" + sitesReplacement + "\\/");
    String userId = (String) event.getProperty(SiteEvent.USER);
    try {
      xythosService.createGroup(sitePath, userId);
      xythosService.createDirectory(userId, null, "/" + sitesReplacement, sitePath);
    } catch (Exception e1) {
      LOGGER.warn("failed to create Xythos group when creating site: " + e1.getMessage());
    }
  }

}
