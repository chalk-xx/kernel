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
package org.apache.sling.jcr.resource;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.PathResourceTypeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * This class checks resource paths to see if there is a prefered resoruce type, where the
 * path is not a jcr path.
 * 
 * @scr.component immediate="true" label="MessagePathResourceTypeProvider"
 *                description="Message Service path resource type provider"
 * @scr.property name="service.description" value="Handles requests for Message resources"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.apache.sling.jcr.resource.PathResourceTypeProvider"
 */
public abstract class AbstractPathResourceTypeProvider implements PathResourceTypeProvider {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AbstractPathResourceTypeProvider.class);
  private String resourceType;
  
  /**
   * 
   */
  public AbstractPathResourceTypeProvider() {
    resourceType = getResourceType();
    // TODO Auto-generated constructor stub
  }

  /**
   * @return
   */
  protected abstract String getResourceType();

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.resource.PathResourceTypeProvider#getResourceTypeFromPath(java.lang.String)
   */
  public String getResourceTypeFromPath(ResourceResolver resolver, String absRealPath) {
    Session session = resolver.adaptTo(Session.class);
    long s = System.currentTimeMillis();
    try {
      Item item = null;
      try {
        item = session.getItem(absRealPath);
      } catch (PathNotFoundException ex) {
      }
      String parentPath = absRealPath;
      while (item == null && !"/".equals(parentPath)) {
        parentPath = getParentReference(parentPath);
        try {
          item = session.getItem(parentPath);
        } catch (PathNotFoundException ex) {
        }
      }
      if ( item == null ) {
        return null;
      }
      // convert first item to a node.
      if (!item.isNode()) {
        item = item.getParent();
      }

      Node n = (Node) item;
      while (!"/".equals(n.getPath())) {
        if (n.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            && resourceType.equals(n.getProperty(
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
          LOGGER.info(" {} is a messagestore file, base is {}  ", absRealPath, n
              .getPath());
          return resourceType;
        }
        n = n.getParent();
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    } finally {
      LOGGER.info(" Type resolution added {}ms",(System.currentTimeMillis()-s));
    }
    LOGGER.debug(" {} is not a messagestore file ", absRealPath);
    
    return null;
  }
  

  /**
   * @param resourceReference
   * @return
   */
  public static String getParentReference(String resourceReference) {
    char[] ref = resourceReference.toCharArray();
    int i = ref.length - 1;
    while (i >= 0 && ref[i] == '/') {
      i--;
    }
    while (i >= 0 && ref[i] != '/') {
      i--;
    }
    while (i >= 0 && ref[i] == '/') {
      i--;
    }
    if (i == -1) {
      return "/";
    }
    return new String(ref, 0, i + 1);
  }


}
