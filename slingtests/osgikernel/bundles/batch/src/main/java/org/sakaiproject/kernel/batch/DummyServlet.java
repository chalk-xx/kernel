package org.sakaiproject.kernel.batch;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;

import java.io.IOException;

import javax.servlet.ServletException;

@SlingServlet(paths = { "/test/foo" }, generateComponent = true, generateService = true)
@Reference(referenceInterface = ConfigurationPrinter.class, policy = ReferencePolicy.DYNAMIC, name = "ConfigurationPrinter", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class DummyServlet extends SlingSafeMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 8437590888820005901L;

  private ConfigurationPrinter configurationPrinter;
  private ServiceTracker tracker;
  private static final String SLING_MAIN_SERVLET_NAME = "org.apache.sling.engine.impl.SlingMainServlet";

  public void activate(ComponentContext componentContext) {
    tracker = new ServiceTracker(componentContext.getBundleContext(),
        SLING_MAIN_SERVLET_NAME, null);
    tracker.open();
    System.err.println("Tracking service");
  }

  protected void bindConfigurationPrinter(
      ConfigurationPrinter configurationPrinter) {
    System.err.println(configurationPrinter.getClass().getName());
    if (configurationPrinter.getClass().getName().equals(
        SLING_MAIN_SERVLET_NAME)) {
      this.configurationPrinter = configurationPrinter;
    }
  }

  protected void unbindConfigurationPrinter(
      ConfigurationPrinter configurationPrinter) {
    if (configurationPrinter.getClass().getName().equals(
        SLING_MAIN_SERVLET_NAME)) {
      this.configurationPrinter = null;
    }
  }

  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {

    System.err.println(tracker.getTrackingCount());

    response.setStatus(201);
    response.addHeader("X-SAKAI-test", "Dummy");
    response.getWriter().write("Bla");
  }

}
