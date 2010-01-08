package org.sakaiproject.kernel.batch;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

public class ResourceRequestWrapper extends SlingHttpServletRequestWrapper {

  private Resource wrappedResource;
  private SlingRequestPathInfo pathInfo;

  public ResourceRequestWrapper(SlingHttpServletRequest wrappedRequest, Resource wrappedResource, SlingRequestPathInfo pathInfo) {
    super(wrappedRequest);
    this.pathInfo = pathInfo;
    this.wrappedResource = wrappedResource;
  }

    /*new RequestPathInfo() {

    public String getExtension() {
      return "json";
    }

    public String getResourcePath() {
      return wrappedResource.getPath();
    }

    public String getSelectorString() {
      return null;
    }

    public String[] getSelectors() {
      return new String[] {};
    }

    public String getSuffix() {
      return null;
    }

  }; */

  @Override
  public RequestPathInfo getRequestPathInfo() {
    return pathInfo;
  }

  @Override
  public Resource getResource() {
    return wrappedResource;
  }

}
