package org.sakaiproject.kernel.discussion;

import org.sakaiproject.kernel.api.discussion.DiscussionConstants;
import org.sakaiproject.kernel.api.discussion.DiscussionManager;
import org.sakaiproject.kernel.api.discussion.DiscussionTypes;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageRoute;
import org.sakaiproject.kernel.api.message.MessageRouter;
import org.sakaiproject.kernel.api.message.MessageRoutes;
import org.sakaiproject.kernel.message.listener.MessageRouteImpl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * 
 * @scr.component inherit="true" label="DiscussionRouter" immediate="true"
 * @scr.service interface="org.sakaiproject.kernel.api.message.MessageRouter"
 * @scr.property name="service.description"
 *               value="Manages Routing for the discussion posts."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.reference name="DiscussionManager"
 *                interface="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 */
public class DiscussionRouter implements MessageRouter {

  private DiscussionManager discussionManager;

  protected void bindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }

  protected void unbindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = null;
  }

  public int getPriority() {
    return 0;
  }

  public void route(Node n, MessageRoutes routing) {
    List<MessageRoute> toRemove = new ArrayList<MessageRoute>();
    List<MessageRoute> toAdd = new ArrayList<MessageRoute>();

    // Check if this message is a discussion message.
    try {
      if (n.hasProperty(MessageConstants.PROP_SAKAI_TYPE)
          && n.hasProperty(DiscussionConstants.PROP_MARKER)
          && DiscussionTypes.hasValue(n.getProperty(MessageConstants.PROP_SAKAI_TYPE)
              .getString())) {

        // This is a discussion message, find the settings file for it.
        String marker = n.getProperty(DiscussionConstants.PROP_MARKER).getString();
        String type = n.getProperty(MessageConstants.PROP_SAKAI_TYPE).getString();

        Node settings = discussionManager.findSettings(marker, n.getSession(), type);
        if (settings != null
            && settings.hasProperty(DiscussionConstants.PROP_NOTIFICATION)) {
          boolean sendMail = settings.getProperty(DiscussionConstants.PROP_NOTIFICATION)
              .getBoolean();
          if (sendMail && settings.hasProperty(DiscussionConstants.PROP_NOTIFY_ADDRESS)) {
            String address = settings
                .getProperty(DiscussionConstants.PROP_NOTIFY_ADDRESS).getString();
            toAdd.add(new DiscussionRoute("internal:" + address));

          }
        }

      }
    } catch (ValueFormatException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (PathNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    for (MessageRoute route : routing) {
      if (DiscussionTypes.hasValue(route.getTransport())) {
        toAdd.add(new DiscussionRoute("internal:" + route.getRcpt()));
        toRemove.add(route);
      }
    }
    // Add the new routes
    for (MessageRoute route : toAdd) {
      routing.add(route);
    }
    // Remove the discussion route (if there is any).
    for (MessageRoute route : toRemove) {
      routing.remove(route);
    }
  }

}
