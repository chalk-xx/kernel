package org.sakaiproject.kernel.connections.servlets;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

public class ResourceWrapper implements Resource {

  private String uri;
  private Resource wrapped;

  public ResourceWrapper(String path, String resourceName, Resource wrapped) {
    this.wrapped = wrapped;
    this.uri = path + "/" + resourceName;
  }

  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
    return wrapped.adaptTo(type);
  }

  public String getPath() {
    return uri;
  }

  public ResourceMetadata getResourceMetadata() {
    return wrapped.getResourceMetadata();
  }

  public ResourceResolver getResourceResolver() {
    return wrapped.getResourceResolver();
  }

  public String getResourceSuperType() {
    return wrapped.getResourceSuperType();
  }

  public String getResourceType() {
    return wrapped.getResourceType();
  }

}
