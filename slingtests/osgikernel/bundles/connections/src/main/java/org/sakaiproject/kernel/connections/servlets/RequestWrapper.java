package org.sakaiproject.kernel.connections.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

public class RequestWrapper extends SlingHttpServletRequestWrapper {

  private String requestPath;
  private Resource resource;

  public RequestWrapper(SlingHttpServletRequest wrapped, String path, String resourceName, Resource resource) {
    super(wrapped);
    this.requestPath = path + "/" + resourceName;
    this.resource = resource;
  }

  @Override
  public Resource getResource() {
    return resource;
  }

  @Override
  public String getPathInfo() {
    return requestPath;
  }
}
