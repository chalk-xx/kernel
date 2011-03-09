package org.sakaiproject.nakamura.http.usercontent;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.http.usercontent.ServerProtectionService;

import java.io.IOException;
import java.util.Dictionary;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Protects the server against POSTs to non safe hosts,
 * 
 */
@Component(immediate = true, metatype = true, enabled=false)
@Properties(value = {
    @Property(name = "service.description", value = "Nakamura Quality of Service Filter"),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class SafeHostFilter implements Filter {

  
  /**
   * Priority of this filter, higher number means sooner, this should be before anything that holds state. ie cache, but can be after QoS
   */
  @Property(intValue=8)
  private static final String FILTER_PRIORITY_CONF = "filter.priority";

  @Reference
  protected ExtHttpService extHttpService;

  @Reference
  protected ServerProtectionService serverProtectionService;



  public void init(FilterConfig filterConfig) throws ServletException {    
  }

  public void destroy() {
  }
  
  /**
   * {@inheritDoc}
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
  throws IOException, ServletException {
    HttpServletRequest hrequest = (HttpServletRequest) request;
    HttpServletResponse hresponse = (HttpServletResponse) response;
    if (serverProtectionService.isMethodSafe(hrequest, hresponse)) {
      chain.doFilter(request, response);      
    }    
  }
  
  

  @SuppressWarnings("unchecked")
  @Activate
  protected void activate(ComponentContext componentContext) throws ServletException {
    Dictionary<String, Object> properties = componentContext.getProperties();
    int filterPriority = OsgiUtil.toInteger(properties.get(FILTER_PRIORITY_CONF),10);
    extHttpService.registerFilter(this, ".*", null, filterPriority, null);
  }


  @Deactivate
  public void deactivate(ComponentContext componentContext) {
    extHttpService.unregisterFilter(this);
  }

}
