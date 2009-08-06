package org.sakaiproject.kernel.activity;

import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @scr.component immediate="true" label="ActivityEventProcessor"
 *                description="ActivityEventProcessor"
 * @scr.property name="service.description" value="ActivityEventProcessor"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="event.topics" value="org/sakaiproject/kernel/activity"
 * @scr.service interface="org.osgi.service.event.EventHandler"
 * @scr.reference name="SlingRepository"
 *                interface="org.apache.sling.jcr.api.SlingRepository"
 * 
 */
public class ActivityEventProcessor implements EventHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ActivityEventProcessor.class);

  protected SlingRepository slingRepository;

  @Override
  public void handleEvent(Event event) {
    LOG.debug("handleEvent(Event {})", event);
    LOG.debug("event.getTopic()={}", event.getTopic());
    LOG.debug("activityItemPath={}", event.getProperty("activityItemPath"));
    final String activityItemPath = (String) event.getProperty("activityItemPath");
    Session session = null;
    try {
      session = slingRepository.loginAdministrative(null);
      Node activity = (Node) session.getItem(activityItemPath);
      LOG.debug("node={}", activity);
      if (activity != null) {
        // process activity
        // hints will be attached to the activity as to where we need to deliver
        // for example: connections, siteA, siteB
        // Let's try the the simpler connections case first
        
      } else {
        LOG.error("Could not process activity: {}", activityItemPath);
        throw new Error("Could not process activity: " + activityItemPath);
      }

    } catch (RepositoryException e) {
      LOG.error("Could not process activity: {}", activityItemPath);
      LOG.error(e.getMessage(), e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }

  }

  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

}
