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
package org.apache.sling.jcr.resource.internal;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.PathResourceTypeProvider;
import org.apache.sling.jcr.resource.internal.helper.MapEntries;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderEntry;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Extends the JcrResourceResolverFactoryImpl to allow ResourceType resolution.
 * 
 * @scr.component immediate="true" label="%resource.resolver.name"
 *                description="%resource.resolver.description"
 * @scr.property name="service.description"
 *               value="Sling JcrResourceResolverFactory Implementation"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service interface="org.apache.sling.jcr.resource.JcrResourceResolverFactory"
 * @scr.reference name="ResourceProvider"
 *                interface="org.apache.sling.api.resource.ResourceProvider"
 *                cardinality="0..n" policy="dynamic"
 * @scr.reference name="JcrResourceTypeProvider"
 *                interface="org.apache.sling.jcr.resource.JcrResourceTypeProvider"
 *                cardinality="0..n" policy="dynamic"
 * @scr.reference name="PathResourceTypeProvider"
 *                interface="org.apache.sling.jcr.resource.PathResourceTypeProvider"
 *                cardinality="0..n" policy="dynamic"
 * @scr.reference name="Repository" interface="org.apache.sling.jcr.api.SlingRepository"
 * 
 * 
 *                NOTE: Although th SCR Statements are here, we manually maintain the
 *                mappings so that we can extend. The Manifest comes from the Sling Jar
 *                which we extend, and so we cant run the scr plugin as that would miss
 *                some statements.
 * 
 * 
 */
public class SakaiJcrResourceResolverFactoryImpl extends JcrResourceResolverFactoryImpl {

  public static final String SAKAI_EXTENSION_BUNDLE = "sakai.extension";

  private static final Logger log = LoggerFactory
      .getLogger(SakaiJcrResourceResolverFactoryImpl.class);

  protected final List<PathResourceTypeProviderEntry> pathResourceTypeProviders = new ArrayList<PathResourceTypeProviderEntry>();

  private PathResourceTypeProvider[] pathResourceTypeProvidersArray = new PathResourceTypeProvider[0];

  /**
   * List of PathResourceTypeProvider services bound before activation of the component.
   */
  protected List<ServiceReference> delayedPathResourceTypeProviders = new LinkedList<ServiceReference>();

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl#getResourceResolver(org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderEntry,
   *      org.apache.sling.jcr.resource.internal.helper.MapEntries)
   */
  @Override
  protected ResourceResolver getResourceResolver(JcrResourceProviderEntry sessionRoot,
      MapEntries mapEntries2) {
    return new SakaiJcrResourceResolver(sessionRoot, this, mapEntries2);
  }

  /**
   * @return
   */
  public PathResourceTypeProvider[] getPathResourceTypeProviders() {
    return pathResourceTypeProvidersArray;
  }

  // ------------------------------------------- resource type providers
  // ----------------------------------

  protected void processDelayedPathResourceTypeProviders() {
    synchronized (this.pathResourceTypeProviders) {
      for (ServiceReference reference : delayedPathResourceTypeProviders) {
        this.addPathResourceTypeProvider(reference);
      }
      delayedPathResourceTypeProviders.clear();
      updateResourceTypeProvidersArray();
    }
  }

  protected void addPathResourceTypeProvider(final ServiceReference reference) {
    final Long id = (Long) reference.getProperty(Constants.SERVICE_ID);
    long ranking = -1;
    if (reference.getProperty(Constants.SERVICE_RANKING) != null) {
      ranking = (Long) reference.getProperty(Constants.SERVICE_RANKING);
    }
    this.pathResourceTypeProviders.add(new PathResourceTypeProviderEntry(id, ranking,
        (PathResourceTypeProvider) this.componentContext.locateService(
            "PathResourceTypeProvider", reference)));
    Collections.sort(this.pathResourceTypeProviders,
        new Comparator<PathResourceTypeProviderEntry>() {

          public int compare(PathResourceTypeProviderEntry o1,
              PathResourceTypeProviderEntry o2) {
            if (o1.ranking < o2.ranking) {
              return 1;
            } else if (o1.ranking > o2.ranking) {
              return -1;
            } else {
              if (o1.serviceId < o2.serviceId) {
                return -1;
              } else if (o1.serviceId > o2.serviceId) {
                return 1;
              }
            }
            return 0;
          }
        });

  }

  /**
       * 
       */
  private void updateResourceTypeProvidersArray() {
    PathResourceTypeProvider[] providers = null;
    log.info("Resource Type Providers is : {} {} ", pathResourceTypeProviders.size(),
        Arrays.toString(pathResourceTypeProviders.toArray()));
    if (this.pathResourceTypeProviders.size() > 0) {
      providers = new PathResourceTypeProvider[this.pathResourceTypeProviders.size()];
      int index = 0;
      Iterator<PathResourceTypeProviderEntry> i = this.pathResourceTypeProviders
          .iterator();
      log.info("Got Iterator {} from Entries {}  ", i, pathResourceTypeProviders);
      while (i.hasNext()) {
        providers[index] = i.next().provider;
        log.info("Added {} at {} ", providers[index], index);
        index++;
        
      }
    }
    log.info("Loaded Path Resource Type Providers: {} ", Arrays.toString(providers));
    pathResourceTypeProvidersArray = providers;
  }

  protected void bindPathResourceTypeProvider(ServiceReference reference) {
    synchronized (this.pathResourceTypeProviders) {
      if (componentContext == null) {
        delayedPathResourceTypeProviders.add(reference);
      } else {
        this.addPathResourceTypeProvider(reference);
        updateResourceTypeProvidersArray();

      }
    }
  }

  protected void unbindPathResourceTypeProvider(ServiceReference reference) {
    synchronized (this.pathResourceTypeProviders) {
      delayedPathResourceTypeProviders.remove(reference);
      final long id = (Long) reference.getProperty(Constants.SERVICE_ID);
      final Iterator<PathResourceTypeProviderEntry> i = this.pathResourceTypeProviders
          .iterator();
      while (i.hasNext()) {
        final PathResourceTypeProviderEntry current = i.next();
        if (current.serviceId == id) {
          i.remove();
        }
      }
      updateResourceTypeProvidersArray();
    }
  }

  protected static final class PathResourceTypeProviderEntry {
    final long serviceId;

    final long ranking;

    final PathResourceTypeProvider provider;

    public PathResourceTypeProviderEntry(final long id, final long ranking,
        final PathResourceTypeProvider p) {
      this.serviceId = id;
      this.ranking = ranking;
      this.provider = p;
    }
  }

}
