package org.sakaiproject.nakamura.postprocessors;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.client.HessianProxyFactory;

import edu.nyu.XythosRemote;

/**
 * @scr.component immediate="true" label="GroupEventsPostProcessor"
 *                description="handle events when groups and memberships are created or modified"
 * @scr.service interface="org.osgi.service.event.EventHandler"
 * @scr.property name="service.description"
 *               value="Provides a place to respond when sites are created and memberships updated"
 * @scr.property name="event.topics" value="org/sakaiproject/nakamura/api/site/event/*"
 */
public class GroupEventsPostProcessor implements EventHandler {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupEventsPostProcessor.class);
  
  private static final String xythosHost = "http://localhost:9090";
  private static final String remotePath = "/remoting/remoting/XythosService";
  
  public void handleEvent(Event event) {
    String siteId = (String) event.getProperty("siteId");
    String userId = (String) event.getProperty("userId");
    try {
      HessianProxyFactory factory = new HessianProxyFactory();
      XythosRemote xythosService = (XythosRemote) factory.create(XythosRemote.class,
          xythosHost + remotePath, GroupEventsPostProcessor.class.getClassLoader());
      xythosService.createGroup(siteId, userId);
    } catch (Exception e1) {
      LOGGER.warn("failed to create Xythos group when creating site: " + e1.getMessage());
    }
  }

}
