package org.sakaiproject.nakamura.files.pool;

import com.google.common.collect.Maps;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;

@SlingServlet(methods = { "GET" }, extensions = { "*" }, resourceTypes = { "sakai/pooled-content" })
public class DefaultServletSwitch extends SlingSafeMethodsServlet implements
    OptingServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 2749890375273124526L;
  @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, referenceInterface = DefaultServletDelegate.class, strategy = ReferenceStrategy.EVENT, bind = "bindDelegate", unbind = "unbindDelegate")
  public DefaultServletDelegate[] delegates = new DefaultServletDelegate[0];
  public Map<DefaultServletDelegate, DefaultServletDelegate> delegateStore = Maps
      .newConcurrentHashMap();

  public boolean accepts(SlingHttpServletRequest request) {
    for (DefaultServletDelegate delegate : delegates) {
      if (delegate.accepts(request)) {
        request.setAttribute(DefaultServletSwitch.class.getName(), delegate);
        return true;
      }
    }
    return false;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    DefaultServletDelegate delegate = (DefaultServletDelegate) request
        .getAttribute(DefaultServletSwitch.class.getName());
    if (delegate != null) {
      delegate.doDelegateGet(request, response);
    } else {
      response.sendError(404);
    }
  }

  public void bindDelegate(DefaultServletDelegate defaultServletDelegate)
      throws ServletException {
    if (defaultServletDelegate != null) {
      if (!delegateStore.containsKey(defaultServletDelegate)) {
        delegateStore.put(defaultServletDelegate, defaultServletDelegate);
        delegates = delegateStore.values().toArray(
            new DefaultServletDelegate[delegateStore.size()]);
        defaultServletDelegate.init(getServletConfig());
      }
    }
  }

  public void unbindDelegate(DefaultServletDelegate defaultServletDelegate) {
    if (defaultServletDelegate != null) {
      if (delegateStore.containsKey(defaultServletDelegate)) {
        defaultServletDelegate.destroy();
        delegateStore.remove(defaultServletDelegate);
        delegates = delegateStore.values().toArray(
            new DefaultServletDelegate[delegateStore.size()]);
      }
    }
  }

  @Activate
  public void activate(ComponentContext componentContext) throws InvalidSyntaxException,
      ServletException {
    BundleContext bc = componentContext.getBundleContext();
    ServiceReference[] rs = bc.getServiceReferences(
        DefaultServletDelegate.class.getName(), null);
    if (rs != null) {
      for (ServiceReference r : rs) {
        bindDelegate((DefaultServletDelegate) bc.getService(r));
      }
    }
  }

  @Deactivate
  public void deactivate(ComponentContext componentContext) {
    delegateStore.clear();
    delegates = new DefaultServletDelegate[0];
  }

}
