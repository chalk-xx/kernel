/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.kernel.resource;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

/**
 * The <code>MeServiceResourceProvider</code>
 * 
 */
@Component(immediate=true, label="VirtualResourceProvider",
                 description="Virtual resource provider")
@Service(value=ResourceProvider.class)
@Properties(value={
   @Property(name="service.description", value="Handles requests for Virtual resources"),
   @Property(name="service.vendor", value="The Sakai Foundation"),
     @Property(name="provider.roots",value="")
    
})
public class VirtualResourceProvider implements ResourceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(VirtualResourceProvider.class);
  
  @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = VirtualResourceType.class, bind = "bindVirtualResourceType", unbind = "unbindVirtualResourceType")
  private Map<String, VirtualResourceType> virtualResourceTypes = new ConcurrentHashMap<String, VirtualResourceType>();

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver, String path) {
    Session session = resourceResolver.adaptTo(Session.class);
    try {
      if ( session.itemExists(path)) {
        return null;
      }
      return getVirtualResource(resourceResolver, null, path);
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(),e);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, javax.servlet.http.HttpServletRequest, java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver,
      HttpServletRequest request, String path) {
    Session session = resourceResolver.adaptTo(Session.class);
    try {
      if ( session.itemExists(path)) {
        return null;
      }
      return getVirtualResource(resourceResolver, request, path);
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(),e);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceProvider#listChildren(org.apache.sling.api.resource.Resource)
   */
  public Iterator<Resource> listChildren(Resource parent) {
    
    LOGGER.info("Listing children of "+parent);
    return null;
  }
  
  

  /**
   * {@inheritDoc}
   * @param request 
   * 
   * @see org.apache.sling.jcr.resource.PathResourceTypeProvider#getResourceTypeFromPath(java.lang.String)
   */
  private Resource getVirtualResource(ResourceResolver resourceResolver, HttpServletRequest request, String absRealPath) {
    // prevent recursion 
    try {
      Session session = resourceResolver.adaptTo(Session.class);


      Item item = null;
      try {
        item = session.getItem(absRealPath);
      } catch (PathNotFoundException ex) {
      }
      String parentPath = absRealPath;
      String lastElement = PathUtils.lastElement(parentPath);
      if ( "jcr:content".equals(lastElement) ) {
        // this is a property so we would return null anyway.
        // not certain if there is away to detect properties over nodes before they have been created.
        return null;
      }
      while (item == null && !"/".equals(parentPath)) {
        parentPath = getParentReference(parentPath);
        try {
          item = session.getItem(parentPath);
        } catch (PathNotFoundException ex) {
        }
      }
      if ( item == null ) {
        return null;
      }
      // convert first item to a node.
      if (!item.isNode()) {
        item = item.getParent();
      }

      Node n = (Node) item;
      Node firstRealNode = n;
      while (!"/".equals(n.getPath())) {
        if (n.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY) ) {
          String resourceType = n.getProperty(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString();
          if (virtualResourceTypes.containsKey(resourceType)) {
            VirtualResourceType virtualResourceType = virtualResourceTypes.get(resourceType);
            return virtualResourceType.getResource(resourceResolver, request, n, firstRealNode, absRealPath);
          }
        }
        n = n.getParent();
      }
      
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    LOGGER.debug(" {} is not a messagestore file ", absRealPath);
    
    return null;
  }
  

  protected void bindVirtualResourceType(VirtualResourceType virtualResourceType) {
    virtualResourceTypes.put(virtualResourceType.getResourceType(), virtualResourceType);
  }

  protected void unbindVirtualResourceType(VirtualResourceType virtualResourceType) {
    virtualResourceTypes.remove(virtualResourceType.getResourceType());
  }


  /**
   * @param resourceReference
   * @return
   */
  public static String getParentReference(String resourceReference) {
    char[] ref = resourceReference.toCharArray();
    int i = ref.length - 1;
    while (i >= 0 && ref[i] == '/') {
      i--;
    }
    while (i >= 0 && ref[i] != '/') {
      i--;
    }
    while (i >= 0 && ref[i] == '/') {
      i--;
    }
    if (i == -1) {
      return "/";
    }
    return new String(ref, 0, i + 1);
  }


}
