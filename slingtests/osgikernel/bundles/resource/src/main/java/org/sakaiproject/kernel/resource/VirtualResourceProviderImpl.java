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
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
@Component(immediate = true, label = "VirtualResourceProvider", description = "Virtual resource provider")
@Services(value = { @Service(value = ResourceProvider.class),
    @Service(value = VirtualResourceProvider.class) })
@Properties(value = {
    @Property(name = "service.description", value = "Handles requests for Virtual resources"),
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "provider.roots", value = "") })
@Reference(name = "virtualResourceType", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = VirtualResourceType.class, policy = ReferencePolicy.DYNAMIC)
public class VirtualResourceProviderImpl implements ResourceProvider,
    VirtualResourceProvider {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(VirtualResourceProviderImpl.class);

  private Map<String, VirtualResourceType> virtualResourceTypes = new ConcurrentHashMap<String, VirtualResourceType>();

  private ThreadLocal<List<String>> lastPath = new ThreadLocal<List<String>>() {
    protected java.util.List<String> initialValue() {
      return new ArrayList<String>();
    };
  };

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver,
   *      java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver, String path) {
    Session session = resourceResolver.adaptTo(Session.class);
    if (isJcrNode(session, path)) {
      return null;
    }
    return getVirtualResource(resourceResolver, null, path);
  }

  /**
   * @param path
   * @return
   * @throws RepositoryException
   */
  private boolean isJcrNode(Session session, String path) {
    String resourcePath = path;
    if (resourcePath == null || resourcePath.length() == 0 || resourcePath.equals("/")) {
      return true;
    }
    while (!resourcePath.endsWith("/")) {
      try {
        if (session.itemExists(resourcePath)) {
          LOGGER.info("Is a JcrNode [{}]",resourcePath);
          return true;
        }
      } catch (RepositoryException e) {
      }
      resourcePath = getResourcePath(resourcePath);
    }
    return false;
  }

  /**
   * @param path
   * @return
   */
  protected String getResourcePath(String path) {
    int i = path.lastIndexOf('/');

    if (i == path.length() - 1) {
      return path;
    }
    if (i < 0) {
      return "/";
    }
    int j = path.lastIndexOf('.');
    if (j < 0 || j < i) {
      return path.substring(0, i + 1);
    }
    return path.substring(0, j);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver,
   *      javax.servlet.http.HttpServletRequest, java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver,
      HttpServletRequest request, String path) {
    Session session = resourceResolver.adaptTo(Session.class);
    if (isJcrNode(session, path)) {
      return null;
    }
    return getVirtualResource(resourceResolver, request, path);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#listChildren(org.apache.sling.api.resource.Resource)
   */
  public Iterator<Resource> listChildren(Resource parent) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @param request
   * 
   * @see org.apache.sling.jcr.resource.PathResourceTypeProvider#getResourceTypeFromPath(java.lang.String)
   */
  private Resource getVirtualResource(ResourceResolver resourceResolver,
      HttpServletRequest request, String absRealPath) {
    // prevent recursion
    try {
      // if resolution has already happened on this request, lastRealPath contains the
      // lastRealPath
      // resolved, which will prevent re-resolution of the same request again.

      String lastRealPath = getLastPath();
      LOGGER.debug("Resolving \n[{}] \nlast\n[{}] ", absRealPath, lastRealPath);
      Session session = resourceResolver.adaptTo(Session.class);

      Item item = null;

      try {
        item = session.getItem(absRealPath);
      } catch (PathNotFoundException ex) {
      }
      String parentPath = absRealPath;
      String lastElement = PathUtils.lastElement(parentPath);
      if ("jcr:content".equals(lastElement)) {
        // this is a property so we would return null anyway.
        // not certain if there is away to detect properties over nodes before they have
        // been created.
        return null;
      }
      String storeName = null;
      while (item == null && !"/".equals(parentPath)) {

        storeName = PathUtils.lastElement(parentPath);
        parentPath = getParentReference(parentPath);
        if (parentPath.equals(lastRealPath)) {
          return null;
        }
        try {
          item = session.getItem(parentPath);
        } catch (PathNotFoundException ex) {
        }
      }
      if (item == null) {
        return null;
      }
      // convert first item to a node.
      if (!item.isNode()) {
        storeName = item.getName();
        item = item.getParent();
      }

      Node n = (Node) item;
      Node firstRealNode = n;
      while (!"/".equals(n.getPath()) && !n.getPath().equals(lastRealPath)) {
        if (n.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
          String resourceType = n.getProperty(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString();
          if (virtualResourceTypes.containsKey(resourceType)) {
            VirtualResourceType virtualResourceType = virtualResourceTypes
                .get(resourceType);
            Resource resource = virtualResourceType.getResource(resourceResolver,
                request, n, firstRealNode, absRealPath);
            LOGGER.info("Is  a virtual resource [{}] {} {} ", new Object[] { storeName,
                resource, resourceType });
            return resource;
          } else {
            LOGGER
                .debug(
                    "Is  not a virtual resource [{}] [{}] no of resoruce types registered = {} ",
                    new Object[] { storeName, resourceType, virtualResourceTypes.size() });
          }
        }
        storeName = n.getName();
        n = n.getParent();
      }

    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return null;
  }

  protected void bindVirtualResourceType(VirtualResourceType virtualResourceType) {
    LOGGER
        .info(
            "\n\n\n\n=====================BOUND VIRTUAL RESOURCE TYPE {} ===============================",
            virtualResourceType.getResourceType());
    virtualResourceTypes.put(virtualResourceType.getResourceType(), virtualResourceType);
  }

  protected void unbindVirtualResourceType(VirtualResourceType virtualResourceType) {
    LOGGER
        .info(
            "\n\n\n\n=====================UnBOUND VIRTUAL RESOURCE TYPE {} ===============================",
            virtualResourceType.getResourceType());
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

  /**
   * @param realPath
   */
  public void pushLastPath(String realPath) {
    List<String> lastPathList = lastPath.get();
    lastPathList.add(realPath);
  }

  /**
   * @param realPath
   */
  public void popLastPath(String realPath) {
    List<String> lastPathList = lastPath.get();
    lastPathList.remove(lastPathList.size() - 1);
  }

  public String getLastPath() {
    List<String> lastPathList = lastPath.get();
    if (lastPathList.size() == 0) {
      return "";
    }
    return lastPathList.get(lastPathList.size() - 1);
  }

}
