package org.sakaiproject.kernel.meservice;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

public class MeResource implements Resource {

  private static final Logger LOG = LoggerFactory.getLogger(MeResource.class);
  private ResourceResolver resourceResolver;
  private String resourceType;
  private String path;
  private ResourceMetadata resourceMetadata;

  public MeResource(ResourceResolver resolver, String path, String resourceType) {
    this.resourceResolver = resolver;
    this.path = path;
    this.resourceType = resourceType;
    this.resourceMetadata = new ResourceMetadata();
    this.resourceMetadata.setResolutionPath(path);
  }

  public String getPath() {
    return path;
  }

  public ResourceMetadata getResourceMetadata() {
    return resourceMetadata;
  }

  public ResourceResolver getResourceResolver() {
    return resourceResolver;
  }

  public String getResourceSuperType() {
    return JcrResourceUtil.getResourceSuperType(this);
  }

  public String getResourceType() {
    return resourceType;
  }

  @SuppressWarnings("unchecked")
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
    LOG.info("Adapting meresource to " + type);
    if (type == Node.class) {
      return (AdapterType) new MeNode(getPath());
    } else if (type == String.class) {
      return (AdapterType)"";
    } else if (type == InputStream.class) {
      return (AdapterType)IOUtils.toInputStream("My Page");
    } else if (type == ValueMap.class) {
      Map<String,Object> props = new HashMap<String,Object>();
      props.put("me", "resource");
      return (AdapterType) new ValueMapDecorator(props);
    }
    return null;
  }

}
