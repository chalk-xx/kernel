package org.sakaiproject.nakamura.api.resource;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;

public class AllMethodsHandlingServlet  extends SlingAllMethodsServlet {

  public static final Logger LOG = LoggerFactory.getLogger(AllMethodsHandlingServlet.class);
  /**
   *
   */
  private static final long serialVersionUID = -4838347347796204151L;

  private Set<ServletResourceHandler> servletResourceHandlers = new HashSet<ServletResourceHandler>();
  private ServletResourceHandler[] handlers = new ServletResourceHandler[0];

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    ServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (ServletResourceHandler r : shandlers) {
      if (r.accepts(request)) {
        r.doGet(request, response);
        return;
      }
    }
    super.doGet(request, response);
  }

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    ServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (ServletResourceHandler r : shandlers) {
      if (r.accepts(request)) {
        r.doPost(request, response);
        return;
      }
    }
    super.doPost(request, response);
  }
  
  @Override
  protected void doDelete(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    ServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (ServletResourceHandler r : shandlers) {
      if (r.accepts(request)) {
        r.doDelete(request, response);
        return;
      }
    }
    super.doDelete(request, response);
  }

  
  @Override
  protected void doGeneric(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    ServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (ServletResourceHandler r : shandlers) {
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
    ServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (ServletResourceHandler r : shandlers) {
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
    ServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (ServletResourceHandler r : shandlers) {
      if (r.accepts(request)) {
        r.doOptions(request, response);
        return;
      }
    }
    super.doOptions(request, response);
  }
  
  @Override
  protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    ServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (ServletResourceHandler r : shandlers) {
      if (r.accepts(request)) {
        r.doPut(request, response);
        return;
      }
    }
    super.doPut(request, response);
  }
  
  @Override
  protected void doTrace(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    ServletResourceHandler[] shandlers = handlers;
    LOG.info("Checking {} ", Arrays.toString(shandlers));
    for (ServletResourceHandler r : shandlers) {
      if (r.accepts(request)) {
        r.doTrace(request, response);
        return;
      }
    }
    super.doTrace(request, response);
  }
  protected void bindServletResourceHandler(ServletResourceHandler handler) {
    synchronized (servletResourceHandlers) {
      servletResourceHandlers.add(handler);
      handlers = servletResourceHandlers
          .toArray(new ServletResourceHandler[servletResourceHandlers.size()]);
    }
  }

  protected void unbindServletResourceHandler(ServletResourceHandler handler) {
    synchronized (servletResourceHandlers) {
      servletResourceHandlers.remove(handler);
      handlers = servletResourceHandlers
          .toArray(new ServletResourceHandler[servletResourceHandlers.size()]);
    }
  }

}
