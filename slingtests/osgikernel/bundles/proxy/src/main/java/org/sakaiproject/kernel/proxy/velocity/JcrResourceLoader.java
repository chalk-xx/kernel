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
package org.sakaiproject.kernel.proxy.velocity;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.jackrabbit.JcrConstants;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.sakaiproject.kernel.api.proxy.ProxyClientService;
import org.sakaiproject.kernel.api.proxy.ProxyResourceSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 *
 */
public class JcrResourceLoader extends ResourceLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(JcrResourceLoader.class);
  private ProxyResourceSource resourceSource;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#getLastModified(org.apache.velocity.runtime.resource.Resource)
   */
  @Override
  public long getLastModified(Resource resource) {
    long lastModified = System.currentTimeMillis();
    try {
      Node node = getNode(resource.getName());
      if (node.hasProperty(JcrConstants.JCR_LASTMODIFIED)) {
        lastModified = node.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate()
            .getTimeInMillis();
      }
    } catch (RepositoryException ex) {
      LOGGER.warn(ex.getMessage());
    }
    return lastModified;
  }

  /**
   * @param resource
   * @return
   * @throws RepositoryException
   * @throws PathNotFoundException
   */
  private Node getNode(String resource) throws RepositoryException {
    try {
      return (Node) resourceSource.getResource().adaptTo(Node.class);
    } catch (NullPointerException ex) {
      return null;
    }
  }


  /**
   * {@inheritDoc}
   * 
   * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#getResourceStream(java.lang.String)
   */
  @Override
  public InputStream getResourceStream(String source) throws ResourceNotFoundException {
    try {
      Node node = getNode(source);
      if (node != null && node.hasProperty(ProxyClientService.SAKAI_PROXY_REQUEST_TEMPLATE)) {
        return new ByteArrayInputStream(node.getProperty(
            ProxyClientService.SAKAI_PROXY_REQUEST_TEMPLATE).getString().getBytes("UTF-8"));

      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage());
    } catch (UnsupportedEncodingException e) {
      LOGGER.warn(e.getMessage());
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#init(org.apache.commons.collections.ExtendedProperties)
   */
  @Override
  public void init(ExtendedProperties configuration) {
    resourceSource = (ProxyResourceSource) configuration.get(ProxyResourceSource.JCR_RESOURCE_LOADER_RESOURCE_SOURCE);
    if ( resourceSource == null ) {
      throw new RuntimeException("Unable to find a suitable resource source in the extended properties "+configuration);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#isSourceModified(org.apache.velocity.runtime.resource.Resource)
   */
  @Override
  public boolean isSourceModified(Resource resource) {
    long lastModified = getLastModified(resource);
    return lastModified > resource.getLastModified();
  }


}
