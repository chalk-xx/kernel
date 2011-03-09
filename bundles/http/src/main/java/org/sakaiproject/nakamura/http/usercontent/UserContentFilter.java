package org.sakaiproject.nakamura.http.usercontent;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.framework.Constants;
import org.sakaiproject.nakamura.api.http.usercontent.ServerProtectionService;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@Properties(value = {
    @Property(name = "service.description", value = "Nakamura Quality of Service Filter"),
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Sakai Foundation"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Nakamura i18n Filter")
})
// Disable for the moment @SlingFilter(order=10, metatype=true, scope=SlingFilterScope.REQUEST)
public class UserContentFilter implements Filter {


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
    SlingHttpServletRequest srequest = (SlingHttpServletRequest) request;
    SlingHttpServletResponse sresponse = (SlingHttpServletResponse) response;
    if ( serverProtectionService.isRequestSafe(srequest, sresponse) ) {
        chain.doFilter(request, response);
    }
  }

}
