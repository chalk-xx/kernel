package org.sakaiproject.nakamura.jetty;

import org.apache.felix.http.api.ExtHttpService;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * This class that operates as a managed service.
 */
public class OSGiGZipFilter extends GzipFilter {

  protected ExtHttpService extHttpService;

  @SuppressWarnings("rawtypes")
  public void activate(Map<String, Object> properties) throws ServletException {
    Hashtable<String, Object> props = new Hashtable<String, Object>();
    props.putAll(properties);
    extHttpService.registerFilter(this, ".*", (Dictionary) properties, 100, null);

  }

  @Override
  public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2)
      throws IOException, ServletException {
    super.doFilter(arg0, arg1, arg2);
  }

  public void deactivate(Map<String, Object> properties) {
    extHttpService.unregisterFilter(this);
  }

  protected void bind(ExtHttpService extHttpService) {
    this.extHttpService = extHttpService;
  }

  protected void unbind(ExtHttpService extHttpService) {
    this.extHttpService = null;
  }

}
