package org.sakaiproject.kernel.discussion.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.servlet.ServletException;

/**
 * 
 * Gets or sets settings on the BigStore node. This should be temp.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/discussionstore"
 * @scr.property name="sling.servlet.methods" values.0="POST" values.1="GET"
 * @scr.property name="sling.servlet.selectors" values.0="settings"
 */
public class SettingsDiscussionServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -8979106860637910142L;
  public static final Logger LOG = LoggerFactory
      .getLogger(SettingsDiscussionServlet.class);

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    try {
      Node n = (Node) request.getResource().adaptTo(Node.class);
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.node(n);
    } catch (Exception ex) {
      LOG.warn("Unable to get settings. {}", ex.getMessage());
      ex.printStackTrace();
      response.sendError(500, "Unable to get settings.");
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    try {
      Node n = (Node) request.getResource().adaptTo(Node.class);
      Map<String, String[]> map = request.getParameterMap();
      for (Entry<String, String[]> param : map.entrySet()) {
        String[] values = param.getValue();
        if (values.length == 1) {
          n.setProperty(param.getKey(), values[0]);
        } else {
          n.setProperty(param.getKey(), values);
        }
      }

      n.save();

    } catch (Exception ex) {
      LOG.warn("Unable to save settings. {}", ex.getMessage());
      ex.printStackTrace();
      response.sendError(500, "Unable to save settings.");
    }
  }
}
