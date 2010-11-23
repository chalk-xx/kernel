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
package org.sakaiproject.nakamura.resource.lite;

import com.google.common.collect.Iterators;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.sakaiproject.nakamura.api.lite.ConnectionPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
//@Component
//@Service
public class LiteResourceResolver implements ResourceResolver {
  private static final Logger logger = LoggerFactory.getLogger(LiteResourceResolver.class);

//  @Reference
  private Repository repository;
  private String userId;

  /**
   *
   */
  public LiteResourceResolver(Repository repository, String userId) {
    this.repository = repository;
    this.userId = userId;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.adapter.Adaptable#adaptTo(java.lang.Class)
   */
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#resolve(javax.servlet.http.HttpServletRequest, java.lang.String)
   */
  public Resource resolve(HttpServletRequest request, String absPath) {
    return getResource(null, absPath);
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#resolve(java.lang.String)
   */
  public Resource resolve(String absPath) {
    return getResource(null, absPath);
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#resolve(javax.servlet.http.HttpServletRequest)
   */
  @Deprecated
  public Resource resolve(HttpServletRequest request) {
    return getResource(null, request.getPathInfo());
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#map(java.lang.String)
   */
  public String map(String resourcePath) {
    return resourcePath;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#map(javax.servlet.http.HttpServletRequest, java.lang.String)
   */
  public String map(HttpServletRequest request, String resourcePath) {
    return map(resourcePath);
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#getResource(java.lang.String)
   */
  public Resource getResource(String path) {
    return getResource(null, path);
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#getResource(org.apache.sling.api.resource.Resource, java.lang.String)
   */
  public Resource getResource(Resource base, String path) {
    if (base != null) {
      path = base.getPath() + path;
    }
    Session session = null;
    try {
      session = repository.loginAdministrative(userId);
      ContentManager cm = session.getContentManager();
      Content content = cm.get(path);
      return new SparseContentResource(content, cm, this);
    } catch (ConnectionPoolException e) {
      logger.error(e.getMessage(), e);
    } catch (StorageClientException e) {
      logger.error(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      logger.error(e.getMessage(), e);
    } finally {
      if (session != null) {
        try {
          session.logout();
        } catch (ConnectionPoolException e) {
          logger.error(e.getMessage(), e);
        }
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#getSearchPath()
   */
  public String[] getSearchPath() {
    return new String[0];
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#listChildren(org.apache.sling.api.resource.Resource)
   */
  public Iterator<Resource> listChildren(Resource parent) {
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#findResources(java.lang.String, java.lang.String)
   */
  public Iterator<Resource> findResources(String query, String language) {
    return Iterators.emptyIterator();
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#queryResources(java.lang.String, java.lang.String)
   */
  public Iterator<Map<String, Object>> queryResources(String query, String language) {
    return Iterators.emptyIterator();
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#close()
   */
  public void close() {
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.api.resource.ResourceResolver#getUserID()
   */
  public String getUserID() {
    return userId;
  }
}
