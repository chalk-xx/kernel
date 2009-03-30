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
package org.sakaiproject.kernel2.uxloader;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * 
 */
public class Activator implements BundleActivator {

  
  private static final Logger logger = LoggerFactory.getLogger(Activator.class);
  /**
   * HttpService reference.
   */
  private ServiceReference m_httpServiceRef;
  private ServiceReference m_cacheManager;

  /**
   * Called when the OSGi framework starts our bundle
   */
  @SuppressWarnings("unchecked")
  public void start(BundleContext bc) throws Exception {
    m_httpServiceRef = bc.getServiceReference(HttpService.class.getName());
    logger.info("Locating CacheManagerService as "+CacheManagerService.class.getName()); 
    m_cacheManager = bc.getServiceReference(CacheManagerService.class.getName());
    logger.info("Service Reference is  "+m_cacheManager); 
    CacheManagerService cacheManagerService = (CacheManagerService) bc.getService(m_cacheManager);
    logger.info("Service is  "+cacheManagerService); 
    if (m_httpServiceRef != null) {
      final HttpService httpService = (HttpService) bc.getService(m_httpServiceRef);
      if (httpService != null) {
        // create a default context to share between registrations
        final HttpContext httpContext = httpService.createDefaultHttpContext();
        // register the hello world servlet
        final Dictionary initParams = new Hashtable();
        initParams.put("from", "HttpService");
        httpService.registerServlet("/helloworld/hs", // alias
            new HelloWorldServlet("/helloworld/hs"), // registered servlet
            initParams, // init params
            httpContext // http context
            );

        Dictionary uxLoaderParams = new Hashtable();
        File working = new File(".");
        File uxdev = new File(working, "uxdev");
        String location = uxdev.getAbsolutePath();
        uxLoaderParams.put(FileServlet.BASE_FILE, location);
        uxLoaderParams.put(FileServlet.MAX_CACHE_SIZE, "102400");
        uxLoaderParams.put(FileServlet.WELCOME_FILE, "index.html");

        httpService.registerServlet("/dev", new FileServlet(cacheManagerService), uxLoaderParams,
            httpContext);

        Dictionary uxwidgetsLoaderParams = new Hashtable();
        File uxwidgets = new File(working, "uxwidgets");
        String widgetsLocation = uxwidgets.getAbsolutePath();
        uxwidgetsLoaderParams.put(FileServlet.BASE_FILE, widgetsLocation);
        uxwidgetsLoaderParams.put(FileServlet.MAX_CACHE_SIZE, "102400");
        uxwidgetsLoaderParams.put(FileServlet.WELCOME_FILE, "index.html");

        httpService.registerServlet("/devwidgets", new FileServlet(cacheManagerService), uxLoaderParams,
            httpContext);

        // register images as resources
        httpService.registerResources("/images", "/images", httpContext);
      }
    }
  }

  /**
   * Called when the OSGi framework stops our bundle
   */
  public void stop(BundleContext bc) throws Exception {
    if (m_httpServiceRef != null) {
      bc.ungetService(m_httpServiceRef);
      m_httpServiceRef = null;
    }
  }

}
