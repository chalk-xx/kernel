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
package org.sakaiproject.kernel.jaxrs;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.ThreadLocalResteasyProviderFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * 
 */
/**
 * 
 */
public class ResteasyServlet extends HttpServletDispatcher {
  /**
   *
   */
  private static final long serialVersionUID = 1L;

  /**
   *
   */
  public static final String SERVLET_PATH = "/_rest";
  /**
   *
   */
  public static final String SERVLET_URL_MAPPING = SERVLET_PATH + "/*";
  
  /**
   *
   */
  private ServletConfig servletConfig;
  /**
   *
   */
  private ServletContext servletContext;
  /**
   *
   */
  private Registry registry;
  /**
   *
   */
  private ResteasyProviderFactory factory = new ResteasyProviderFactory();

  /**
   * {@inheritDoc}
   * @see org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher#init(javax.servlet.ServletConfig)
   */
  public void init(ServletConfig servletConfig) throws ServletException {
    // Wrap the servlet config and context objects, since some http service impls (e.g. pax) don't
    // handle these parts of the servlet spec correctly
    this.servletConfig = new ServletConfigWrapper(servletConfig);
    this.servletContext = this.servletConfig.getServletContext();

    // Handle the bootstrapping that Resteasy normally handles in its context listener
    bootstrap();
    
    super.init(this.servletConfig);
  }

  /**
   * 
   */
  private void bootstrap() {
    ResteasyProviderFactory defaultInstance = ResteasyProviderFactory.getInstance();
    ResteasyProviderFactory.setInstance(new ThreadLocalResteasyProviderFactory(defaultInstance));

    servletContext.setAttribute(ResteasyProviderFactory.class.getName(), factory);
    dispatcher = new SynchronousDispatcher(factory);
    registry = dispatcher.getRegistry();
    servletContext.setAttribute(Dispatcher.class.getName(), dispatcher);
    servletContext.setAttribute(Registry.class.getName(), registry);
    RegisterBuiltin.register(factory);
    // Can't seem to get the resteasy input stream provider to work
    factory.addMessageBodyWriter(new InputStreamProvider());
  }
  
  public ServletConfig getServletConfig() {
    return servletConfig;
  }
  
  /**
   * {@inheritDoc}
   * @see javax.servlet.GenericServlet#getServletContext()
   */
  public ServletContext getServletContext() {
    return servletContext;
  }
  
  /**
   * @return
   */
  public Registry getRegistry() {
    return registry;
  }
}


