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

import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.sakaiproject.kernel.api.configuration.ConfigurationService;
import org.sakaiproject.kernel.guice.AbstractOsgiModule;
import org.sakaiproject.kernel.guice.GuiceActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.ServletException;

public class Activator extends GuiceActivator {
  protected static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);


  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.guice.GuiceActivator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext bundleContext) throws Exception {
    super.start(bundleContext);
    init(); //this could be delayed if required by making this a lifecycle component

  }

  /**
   * 
   */
  private void init() {
    ConfigurationService config = injector.getInstance(ConfigurationService.class);
    HttpService httpService = injector.getInstance(HttpService.class);
    HttpContext httpContext = httpService.createDefaultHttpContext();
    List<URLFileMapping> out = new ArrayList<URLFileMapping>();
    String raw_config = config.getProperty("uxloader.config");
    for (URLFileMapping mapping : createMapping(raw_config)) {
      out.add(new URLFileMapping(mapping.getURL(), mapping.getFileSystem()));
    }
    for (URLFileMapping m : out) {
      try {
        map(httpService, httpContext, m.getURL(), m.getFileSystem());
      } catch (ServletException e) {
        LOGGER.error("Failed to register mapping for servlet "+m+" cause:"+e.getMessage(),e);
      } catch (NamespaceException e) {
        LOGGER.error("Failed to register mapping for servlet "+m+" cause:"+e.getMessage(),e);
      }
    }
  }

  private void map(HttpService httpService, HttpContext httpContext, String url,
      String filesystem) throws ServletException, NamespaceException {
    LOGGER.info("Mapping url=" + url + " to filesystem=" + filesystem);
    Dictionary<String, String> uxLoaderParams = new Hashtable<String, String>();
    uxLoaderParams.put(FileServlet.BASE_FILE, filesystem);
    uxLoaderParams.put(FileServlet.MAX_CACHE_SIZE, "102400");
    uxLoaderParams.put(FileServlet.WELCOME_FILE, "index.html");

    httpService.registerServlet(url, injector.getInstance(FileServlet.class),
        uxLoaderParams, httpContext);
  }

  private URLFileMapping[] createMapping(String raw) {
    LOGGER.info("raw ux config is " + raw);
    List<URLFileMapping> out = new ArrayList<URLFileMapping>();
    for (String part : raw.split(";")) {
      String[] parts = part.split(":");
      if (parts.length != 2)
        continue;
      out.add(new URLFileMapping(parts[0].trim(), parts[1].trim()));
    }
    return out.toArray(new URLFileMapping[0]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.guice.GuiceActivator#getModule(org.osgi.framework.BundleContext)
   */
  @Override
  protected AbstractOsgiModule getModule(BundleContext bundleContext) {
    return new ActivatorModule(bundleContext);
  }
}
