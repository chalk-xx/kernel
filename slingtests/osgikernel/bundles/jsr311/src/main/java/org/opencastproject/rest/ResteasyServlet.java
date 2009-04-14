/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.rest;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.ThreadLocalResteasyProviderFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class ResteasyServlet extends HttpServletDispatcher {
  private static final long serialVersionUID = 1L;

  public static final String SERVLET_PATH = "/rest";
  public static final String SERVLET_URL_MAPPING = SERVLET_PATH + "/*";
  
  ServletConfig servletConfig;
  ServletContext servletContext;
  Registry registry;
  private ResteasyProviderFactory factory = new ResteasyProviderFactory();

  public void init(ServletConfig servletConfig) throws ServletException {
    // Wrap the servlet config and context objects, since some http service impls (e.g. pax) don't
    // handle these parts of the servlet spec correctly
    this.servletConfig = new ServletConfigWrapper(servletConfig);
    this.servletContext = this.servletConfig.getServletContext();

    // Handle the bootstrapping that Resteasy normally handles in its context listener
    bootstrap();
    
    super.init(this.servletConfig);
  }

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
  
  public ServletContext getServletContext() {
    return servletContext;
  }
  
  public Registry getRegistry() {
    return registry;
  }
}

class ServletConfigWrapper implements ServletConfig {
  private ServletConfig delegate;
  private ServletContext servletContext;
  public String getInitParameter(String arg0) {
    return delegate.getInitParameter(arg0);
  }
  @SuppressWarnings("unchecked")
  @Deprecated
  public Enumeration getInitParameterNames() {
    return delegate.getInitParameterNames();
  }
  public ServletContext getServletContext() {
    return servletContext;
  }
  public String getServletName() {
    return delegate.getServletName();
  }
  ServletConfigWrapper(ServletConfig delegate) {
    this.delegate = delegate;
    servletContext = new ServletContextWrapper(delegate.getServletContext());
  }
}

class ServletContextWrapper implements ServletContext {
  private ServletContext delegate;
  public ServletContextWrapper(ServletContext delegate) {
    this.delegate = delegate;
  }
  public Object getAttribute(String arg0) {
    return delegate.getAttribute(arg0);
  }
  @SuppressWarnings("unchecked")
  public Enumeration getAttributeNames() {
    return delegate.getAttributeNames();
  }
  public ServletContext getContext(String arg0) {
    return delegate.getContext(arg0);
  }
  public String getContextPath() {
    return delegate.getContextPath();
  }
  public String getInitParameter(String key) {
    if ("resteasy.servlet.mapping.prefix".equalsIgnoreCase(key)) {
      return ResteasyServlet.SERVLET_PATH;
    } else {
      return delegate.getInitParameter(key);
    }
  }
  @SuppressWarnings("unchecked")
  public Enumeration getInitParameterNames() {
    return delegate.getInitParameterNames();
  }
  public int getMajorVersion() {
    return delegate.getMajorVersion();
  }
  public String getMimeType(String arg0) {
    return delegate.getMimeType(arg0);
  }
  public int getMinorVersion() {
    return delegate.getMinorVersion();
  }
  public RequestDispatcher getNamedDispatcher(String arg0) {
    return delegate.getNamedDispatcher(arg0);
  }
  public String getRealPath(String arg0) {
    return delegate.getRealPath(arg0);
  }
  public RequestDispatcher getRequestDispatcher(String arg0) {
    return delegate.getRequestDispatcher(arg0);
  }
  public URL getResource(String arg0) throws MalformedURLException {
    return delegate.getResource(arg0);
  }
  public InputStream getResourceAsStream(String arg0) {
    return delegate.getResourceAsStream(arg0);
  }
  public Set getResourcePaths(String arg0) {
    return delegate.getResourcePaths(arg0);
  }
  public String getServerInfo() {
    return delegate.getServerInfo();
  }
  public Servlet getServlet(String arg0) throws ServletException {
    return delegate.getServlet(arg0);
  }
  public String getServletContextName() {
    return delegate.getServletContextName();
  }
  public Enumeration getServletNames() {
    return delegate.getServletNames();
  }
  public Enumeration getServlets() {
    return delegate.getServlets();
  }
  public void log(Exception arg0, String arg1) {
    delegate.log(arg0, arg1);
  }
  public void log(String arg0, Throwable arg1) {
    delegate.log(arg0, arg1);
  }
  public void log(String arg0) {
    delegate.log(arg0);
  }
  public void removeAttribute(String arg0) {
    delegate.removeAttribute(arg0);
  }
  public void setAttribute(String arg0, Object arg1) {
    delegate.setAttribute(arg0, arg1);
  }
}