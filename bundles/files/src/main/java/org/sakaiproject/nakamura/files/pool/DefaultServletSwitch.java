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
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = { "GET" }, extensions = { "*" }, resourceTypes = { "sakai/pooled-content" })
@ServiceDocumentation(name = "CanModifyContentPoolServlet documentation", okForVersion = "0.11",
  shortDescription = "Delegates to another servlet",
  description = "Delegates to another servlet",
  bindings = @ServiceBinding(type = BindingType.TYPE, bindings = "sakai/pooled-content",
    extensions = { @ServiceExtension(name = "*") }),
  methods = {
    @ServiceMethod(name = "GET", description = "",
      parameters = {
        @ServiceParameter(name = "none", description = "")
      },
      response = {
        @ServiceResponse(code = HttpServletResponse.SC_OK, description = "Request has been processed successfully."),
        @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Resource could not be found."),
        @ServiceResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to process request due to a runtime error.")
      })
})
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
