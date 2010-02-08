package org.sakaiproject.nakamura.mailman.impl;

import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRouter;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.mailman.MailmanManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

/**
 * 
 * @scr.component inherit="true" label="MailmanMessageRouter" immediate="true"
 * @scr.service interface="org.sakaiproject.nakamura.api.message.MessageRouter"
 * @scr.property name="service.description"
 *               value="Manages Routing for group mailing lists."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 */
public class MailmanMessageRouter implements MessageRouter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MailmanMessageRouter.class);
  
  /** @scr.reference */
  private MailmanManager mailmanManager;
  
  public int getPriority() {
    return 1;
  }

  public void route(Node n, MessageRoutes routing) {
    LOGGER.info("Mailman routing message: " + n);
    List<MessageRoute> toRemove = new ArrayList<MessageRoute>();
    List<MessageRoute> toAdd = new ArrayList<MessageRoute>();
    for (MessageRoute route : routing) {
      if ("internal".equals(route.getTransport()) && route.getRcpt().startsWith("g-")) {
        LOGGER.info("Found an internal group message. Routing to SMTP");
        toRemove.add(route);
        toAdd.add(mailmanManager.generateMessageRouteForGroup(route.getRcpt()));
      }
    }
    routing.removeAll(toRemove);
    routing.addAll(toAdd);
  }

}
