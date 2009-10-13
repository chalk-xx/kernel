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
package org.sakaiproject.kernel.batch;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.PathResourceTypeProvider;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.util.JcrUtils;
import org.sakaiproject.kernel.util.PathUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @scr.component immediate="true" label="URIExpanderImpl"
 *                description="Service to expand jcr paths."
 * @scr.service interface="org.sakaiproject.kernel.batch.URIExpander"
 * @scr.property name="service.description" value="Service to expand URI's to JCR paths.."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.reference name="PathResourceTypeProvider"
 *                interface="org.apache.sling.jcr.resource.PathResourceTypeProvider"
 *                cardinality="0..n" policy="dynamic"
 */
public class URIExpanderImpl implements URIExpander {

  private ComponentContext osgiComponentContext;
  private List<ServiceReference> delayedReferences = new ArrayList<ServiceReference>();
  private Map<Long, PathResourceTypeProvider> pathResourceTypeProviders = new HashMap<Long, PathResourceTypeProvider>();

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.batch.URIExpander#getJCRPathFromURI(javax.jcr.Session,
   *      java.lang.String)
   */
  public String getJCRPathFromURI(Session session, ResourceResolver resourceResolver,
      String uri) {
    String absPath = uri;
    try {
      // Get the first existing item.
      Node first = JcrUtils.getFirstExistingNode(session, uri);

      if (first != null && !first.getPath().equals(uri)) {
        absPath = first.getPath();
        String relPath = uri.substring(absPath.length(), uri.length());
        String prevPath = "";
        while (session.itemExists(absPath) && !absPath.equals(prevPath)) {
          prevPath = absPath;
          first = JcrUtils.getFirstExistingNode(session, absPath);
          if (first.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
            String type = first.getProperty(
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString();
            boolean isBigStore = false;
            // Iterate over all the path resource type providers and check if we have this
            // resource type.
            Iterator<PathResourceTypeProvider> it = pathResourceTypeProviders.values()
                .iterator();
            while (it.hasNext()) {
              PathResourceTypeProvider provider = it.next();
              String providerType = provider.getResourceTypeFromPath(resourceResolver,
                  uri);
              if (providerType != null && providerType.equals(type)) {
                isBigStore = true;
                break;
              }
            }

            if (isBigStore) {
              absPath = PathUtils.toInternalHashedPath(absPath, relPath, "");
            }

          }
        }
      }
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
    return absPath;
  }

  protected void bindPathResourceTypeProvider(ServiceReference serviceReference) {
    if (osgiComponentContext == null) {
      delayedReferences.add(serviceReference);
    } else {
      addProvider(serviceReference);
    }
  }

  protected void unbindPathResourceTypeProvider(ServiceReference serviceReference) {
    if (osgiComponentContext == null) {
      delayedReferences.remove(serviceReference);
    } else {
      removeProvider(serviceReference);
    }
  }

  private void removeProvider(ServiceReference serviceReference) {
    final Long id = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
    pathResourceTypeProviders.remove(id);

  }

  private void addProvider(ServiceReference serviceReference) {
    PathResourceTypeProvider provider = (PathResourceTypeProvider) osgiComponentContext
        .locateService("PathResourceTypeProvider", serviceReference);
    final Long id = (Long) serviceReference.getProperty(Constants.SERVICE_ID);

    pathResourceTypeProviders.put(id, provider);
  }

  protected void activate(ComponentContext componentContext) {
    synchronized (delayedReferences) {
      osgiComponentContext = componentContext;
      for (ServiceReference ref : delayedReferences) {
        addProvider(ref);
      }
      delayedReferences.clear();
    }
  }

  protected void deactivate(ComponentContext componentContext) {
    synchronized (delayedReferences) {
      osgiComponentContext = componentContext;
      for (ServiceReference ref : delayedReferences) {
        removeProvider(ref);
      }
      delayedReferences.clear();
      osgiComponentContext = null;
    }
  }

}
