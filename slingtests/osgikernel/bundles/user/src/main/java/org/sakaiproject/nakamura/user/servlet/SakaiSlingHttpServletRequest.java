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
package org.sakaiproject.nakamura.user.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * NOTE: this is just a quick mock of the
 * <code>SlingHttpServletRequest<code> that can be used in the {@link AuthorizablePostProcessor post processors} when this bundle comes up.
 * This class only implements the bare necessary things.
 */
public class SakaiSlingHttpServletRequest implements SlingHttpServletRequest {

  private final Session session;
  private final String path;

  public SakaiSlingHttpServletRequest(Session session, String path) {
    this.session = session;
    this.path = path;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getCookie(java.lang.String)
   */
  public Cookie getCookie(String name) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getRequestDispatcher(org.apache.sling.api.resource.Resource)
   */
  public RequestDispatcher getRequestDispatcher(Resource resource) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getRequestDispatcher(java.lang.String,
   *      org.apache.sling.api.request.RequestDispatcherOptions)
   */
  public RequestDispatcher getRequestDispatcher(String path,
      RequestDispatcherOptions options) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getRequestDispatcher(org.apache.sling.api.resource.Resource,
   *      org.apache.sling.api.request.RequestDispatcherOptions)
   */
  public RequestDispatcher getRequestDispatcher(Resource resource,
      RequestDispatcherOptions options) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameter(java.lang.String)
   */
  public RequestParameter getRequestParameter(String name) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameterMap()
   */
  public RequestParameterMap getRequestParameterMap() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameters(java.lang.String)
   */
  public RequestParameter[] getRequestParameters(String name) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getRequestPathInfo()
   */
  public RequestPathInfo getRequestPathInfo() {
    RequestPathInfo info = new RequestPathInfo() {

      public String getSuffix() {
        return null;
      }

      public String[] getSelectors() {
        return null;
      }

      public String getSelectorString() {
        return null;
      }

      public String getResourcePath() {
        return path;
      }

      public String getExtension() {
        return null;
      }
    };
    return info;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getRequestProgressTracker()
   */
  public RequestProgressTracker getRequestProgressTracker() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getResource()
   */
  public Resource getResource() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getResourceBundle(java.util.Locale)
   */
  public ResourceBundle getResourceBundle(Locale locale) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getResourceBundle(java.lang.String,
   *      java.util.Locale)
   */
  public ResourceBundle getResourceBundle(String baseName, Locale locale) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getResourceResolver()
   */
  public ResourceResolver getResourceResolver() {
    ResourceResolver resolver = new ResourceResolver() {

      @SuppressWarnings("unchecked")
      public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return (AdapterType) session;
      }

      public Resource resolve(HttpServletRequest request, String absPath) {
        return null;
      }

      public Resource resolve(HttpServletRequest request) {
        return null;
      }

      public Resource resolve(String absPath) {
        return null;
      }

      public Iterator<Map<String, Object>> queryResources(String query, String language) {
        return null;
      }

      public String map(HttpServletRequest request, String resourcePath) {
        return null;
      }

      public String map(String resourcePath) {
        return null;
      }

      public Iterator<Resource> listChildren(Resource parent) {
        return null;
      }

      public String[] getSearchPath() {
        return null;
      }

      public Resource getResource(Resource base, String path) {
        return null;
      }

      public Resource getResource(String path) {
        return null;
      }

      public Iterator<Resource> findResources(String query, String language) {
        return null;
      }
    };
    return resolver;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getResponseContentType()
   */
  public String getResponseContentType() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getResponseContentTypes()
   */
  public Enumeration<String> getResponseContentTypes() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getAuthType()
   */
  public String getAuthType() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getContextPath()
   */
  public String getContextPath() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getCookies()
   */
  public Cookie[] getCookies() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
   */
  public long getDateHeader(String name) {
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getHeader(java.lang.String)
   */
  public String getHeader(String name) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
   */
  @SuppressWarnings("unchecked")
  public Enumeration getHeaderNames() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getHeaders(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public Enumeration getHeaders(String name) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
   */
  public int getIntHeader(String name) {
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getMethod()
   */
  public String getMethod() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getPathInfo()
   */
  public String getPathInfo() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
   */
  public String getPathTranslated() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getQueryString()
   */
  public String getQueryString() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
   */
  public String getRemoteUser() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getRequestURI()
   */
  public String getRequestURI() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getRequestURL()
   */
  public StringBuffer getRequestURL() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
   */
  public String getRequestedSessionId() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getServletPath()
   */
  public String getServletPath() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getSession()
   */
  public HttpSession getSession() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
   */
  public HttpSession getSession(boolean create) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
   */
  public Principal getUserPrincipal() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
   */
  public boolean isRequestedSessionIdFromCookie() {
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
   */
  public boolean isRequestedSessionIdFromURL() {
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
   */
  public boolean isRequestedSessionIdFromUrl() {
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
   */
  public boolean isRequestedSessionIdValid() {
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
   */
  public boolean isUserInRole(String role) {
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
   */
  public Object getAttribute(String name) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getAttributeNames()
   */
  @SuppressWarnings("unchecked")
  public Enumeration getAttributeNames() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getCharacterEncoding()
   */
  public String getCharacterEncoding() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getContentLength()
   */
  public int getContentLength() {
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getContentType()
   */
  public String getContentType() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getInputStream()
   */
  public ServletInputStream getInputStream() throws IOException {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getLocalAddr()
   */
  public String getLocalAddr() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getLocalName()
   */
  public String getLocalName() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getLocalPort()
   */
  public int getLocalPort() {
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getLocale()
   */
  public Locale getLocale() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getLocales()
   */
  @SuppressWarnings("unchecked")
  public Enumeration getLocales() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
   */
  public String getParameter(String name) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getParameterMap()
   */
  @SuppressWarnings("unchecked")
  public Map getParameterMap() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getParameterNames()
   */
  @SuppressWarnings("unchecked")
  public Enumeration getParameterNames() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
   */
  public String[] getParameterValues(String name) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getProtocol()
   */
  public String getProtocol() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getReader()
   */
  public BufferedReader getReader() throws IOException {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
   */
  public String getRealPath(String path) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getRemoteAddr()
   */
  public String getRemoteAddr() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getRemoteHost()
   */
  public String getRemoteHost() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getRemotePort()
   */
  public int getRemotePort() {
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
   */
  public RequestDispatcher getRequestDispatcher(String path) {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getScheme()
   */
  public String getScheme() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getServerName()
   */
  public String getServerName() {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getServerPort()
   */
  public int getServerPort() {
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#isSecure()
   */
  public boolean isSecure() {
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
   */
  public void removeAttribute(String name) {

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
   */
  public void setAttribute(String name, Object o) {

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
   */
  public void setCharacterEncoding(String env) throws UnsupportedEncodingException {

  }

}
