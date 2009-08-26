package org.sakaiproject.kernel.discussion.servlets;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.api.discussion.DiscussionException;
import org.sakaiproject.kernel.api.discussion.DiscussionManager;

/**
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes"
 *               values="sakai/discussionpost"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.selectors" values.0="delete"
 * @scr.reference name="DiscussionManager"
 *                interface="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 *                bind="bindDiscussionManager" unbind="unbindDiscussionManager"
 */
public class DeleteDiscussionServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 7053513377391656857L;

  private DiscussionManager discussionManager;

  protected void bindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }

  protected void unbindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = null;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
    try {
      discussionManager.delete(request.getResource());
    } catch (DiscussionException ex) {
      response.sendError(ex.getCode(), ex.getMessage());
    }
  }
}
