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
import org.sakaiproject.nakamura.api.user.UserPostProcessor;

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
 * <code>SlingHttpServletRequest<code> that can be used in the {@link UserPostProcessor post processors} when this bundle comes up.
 * This class only implements the bare nescecary things.
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
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getRequestDispatcher(org.apache.sling.api.resource.Resource)
   */
  public RequestDispatcher getRequestDispatcher(Resource resource) {
    // TODO Auto-generated method stub
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
    // TODO Auto-generated method stub
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
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameter(java.lang.String)
   */
  public RequestParameter getRequestParameter(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameterMap()
   */
  public RequestParameterMap getRequestParameterMap() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getRequestParameters(java.lang.String)
   */
  public RequestParameter[] getRequestParameters(String name) {
    // TODO Auto-generated method stub
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
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getResource()
   */
  public Resource getResource() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getResourceBundle(java.util.Locale)
   */
  public ResourceBundle getResourceBundle(Locale locale) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getResourceBundle(java.lang.String,
   *      java.util.Locale)
   */
  public ResourceBundle getResourceBundle(String baseName, Locale locale) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getResourceResolver()
   */
  public ResourceResolver getResourceResolver() {
    ResourceResolver resolver = new ResourceResolver() {

      public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return (AdapterType) session;
      }

      public Resource resolve(HttpServletRequest request, String absPath) {
        // TODO Auto-generated method stub
        return null;
      }

      public Resource resolve(HttpServletRequest request) {
        // TODO Auto-generated method stub
        return null;
      }

      public Resource resolve(String absPath) {
        // TODO Auto-generated method stub
        return null;
      }

      public Iterator<Map<String, Object>> queryResources(String query, String language) {
        // TODO Auto-generated method stub
        return null;
      }

      public String map(HttpServletRequest request, String resourcePath) {
        // TODO Auto-generated method stub
        return null;
      }

      public String map(String resourcePath) {
        // TODO Auto-generated method stub
        return null;
      }

      public Iterator<Resource> listChildren(Resource parent) {
        // TODO Auto-generated method stub
        return null;
      }

      public String[] getSearchPath() {
        // TODO Auto-generated method stub
        return null;
      }

      public Resource getResource(Resource base, String path) {
        // TODO Auto-generated method stub
        return null;
      }

      public Resource getResource(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      public Iterator<Resource> findResources(String query, String language) {
        // TODO Auto-generated method stub
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
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.SlingHttpServletRequest#getResponseContentTypes()
   */
  public Enumeration<String> getResponseContentTypes() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getAuthType()
   */
  public String getAuthType() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getContextPath()
   */
  public String getContextPath() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getCookies()
   */
  public Cookie[] getCookies() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
   */
  public long getDateHeader(String name) {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getHeader(java.lang.String)
   */
  public String getHeader(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
   */
  public Enumeration getHeaderNames() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getHeaders(java.lang.String)
   */
  public Enumeration getHeaders(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
   */
  public int getIntHeader(String name) {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getMethod()
   */
  public String getMethod() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getPathInfo()
   */
  public String getPathInfo() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
   */
  public String getPathTranslated() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getQueryString()
   */
  public String getQueryString() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
   */
  public String getRemoteUser() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getRequestURI()
   */
  public String getRequestURI() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getRequestURL()
   */
  public StringBuffer getRequestURL() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
   */
  public String getRequestedSessionId() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getServletPath()
   */
  public String getServletPath() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getSession()
   */
  public HttpSession getSession() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
   */
  public HttpSession getSession(boolean create) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
   */
  public Principal getUserPrincipal() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
   */
  public boolean isRequestedSessionIdFromCookie() {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
   */
  public boolean isRequestedSessionIdFromURL() {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
   */
  public boolean isRequestedSessionIdFromUrl() {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
   */
  public boolean isRequestedSessionIdValid() {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
   */
  public boolean isUserInRole(String role) {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
   */
  public Object getAttribute(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getAttributeNames()
   */
  public Enumeration getAttributeNames() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getCharacterEncoding()
   */
  public String getCharacterEncoding() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getContentLength()
   */
  public int getContentLength() {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getContentType()
   */
  public String getContentType() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getInputStream()
   */
  public ServletInputStream getInputStream() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getLocalAddr()
   */
  public String getLocalAddr() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getLocalName()
   */
  public String getLocalName() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getLocalPort()
   */
  public int getLocalPort() {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getLocale()
   */
  public Locale getLocale() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getLocales()
   */
  public Enumeration getLocales() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
   */
  public String getParameter(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getParameterMap()
   */
  public Map getParameterMap() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getParameterNames()
   */
  public Enumeration getParameterNames() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
   */
  public String[] getParameterValues(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getProtocol()
   */
  public String getProtocol() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getReader()
   */
  public BufferedReader getReader() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
   */
  public String getRealPath(String path) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getRemoteAddr()
   */
  public String getRemoteAddr() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getRemoteHost()
   */
  public String getRemoteHost() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getRemotePort()
   */
  public int getRemotePort() {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
   */
  public RequestDispatcher getRequestDispatcher(String path) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getScheme()
   */
  public String getScheme() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getServerName()
   */
  public String getServerName() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#getServerPort()
   */
  public int getServerPort() {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#isSecure()
   */
  public boolean isSecure() {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
   */
  public void removeAttribute(String name) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
   */
  public void setAttribute(String name, Object o) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
   */
  public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
    // TODO Auto-generated method stub

  }

}
