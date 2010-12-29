package org.sakaiproject.nakamura.api.resource;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;

public class SafeMethodsHandlingServlet  extends SlingSafeMethodsServlet {

  public static final Logger LOG = LoggerFactory.getLogger(SafeMethodsHandlingServlet.class);
  /**
   *
   */
  private static final long serialVersionUID = -4838347347796204151L;

  private Set<SafeServletResourceHandler> servletResourceHandlers = new HashSet<SafeServletResourceHandler>();
  private SafeServletResourceHandler[] handlers = new SafeServletResourceHandler[0];

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    SafeServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (SafeServletResourceHandler r : shandlers) {
      if (r.accepts(request)) {
        r.doGet(request, response);
        return;
      }
    }
    super.doGet(request, response);
  }
  
  @Override
  protected void doGeneric(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    SafeServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (SafeServletResourceHandler r : shandlers) {
      if (r.accepts(request)) {
        r.doGeneric(request, response);
        return;
      }
    }
    super.doGeneric(request, response);
  }
  
  @Override
  protected void doHead(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    SafeServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (SafeServletResourceHandler r : shandlers) {
      if (r.accepts(request)) {
        r.doHead(request, response);
        return;
      }
    }
    super.doHead(request, response);
  }
  
  @Override
  protected void doOptions(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    SafeServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (SafeServletResourceHandler r : shandlers) {
      if (r.accepts(request)) {
        r.doOptions(request, response);
        return;
      }
    }
    super.doOptions(request, response);
  }
  
  @Override
  protected void doTrace(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    SafeServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (SafeServletResourceHandler r : shandlers) {
      if (r.accepts(request)) {
        r.doTrace(request, response);
        return;
      }
    }
    super.doTrace(request, response);
  }

  protected void bindServletResourceHandler(SafeServletResourceHandler handler) {
    synchronized (servletResourceHandlers) {
      servletResourceHandlers.add(handler);
      handlers = servletResourceHandlers
          .toArray(new SafeServletResourceHandler[servletResourceHandlers.size()]);
    }
  }

  protected void unbindServletResourceHandler(SafeServletResourceHandler handler) {
    synchronized (servletResourceHandlers) {
      servletResourceHandlers.remove(handler);
      handlers = servletResourceHandlers
          .toArray(new SafeServletResourceHandler[servletResourceHandlers.size()]);
    }
  }

}
