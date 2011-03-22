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

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

@Component(name = "org.sakaiproject.nakamura.privacy.HomeResourceProvider", immediate = true, metatype = true, description = "%homeprovider.description", label = "%homeprovider.name")
@Service(value = ResourceProvider.class)
@Property(name = ResourceProvider.ROOTS, value = { "/", "/group" })
public class HomeResourceProvider implements ResourceProvider {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(HomeResourceProvider.class);
  public static final String HOME_RESOURCE_PROVIDER = HomeResourceProvider.class
      .getName();

  public Resource getResource(ResourceResolver resourceResolver,
      HttpServletRequest request, String path) {
    LOGGER.info("Got Resource URI [{}]  Path [{}] ", request.getRequestURI(), path);
    return getResource(resourceResolver, path);
  }

  public Resource getResource(ResourceResolver resourceResolver, String path) {
    if (path == null || path.length() < 2) {
      return null;
    }
    char c = path.charAt(1);
    if (!(c == '~' || c == 'u' || c == 'g')) {
      return null;
    }
    if ("/~".equals(path) || "/user".equals(path) || "/group".equals(path)) {
      return null;
    }
    try {
      return resolveMappedResource(resourceResolver, path);
    } catch (AccessDeniedException e) {
      if ( LOGGER.isDebugEnabled()) {
        LOGGER.debug(e.getMessage(),e);
      } else {
        LOGGER.warn(e.getMessage());
      }
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return null;
  }

  private Resource resolveMappedResource(ResourceResolver resourceResolver, String path)
      throws AccessDeniedException, StorageClientException {
    String subPath = null;
    if (path.startsWith("/~")) {
      subPath = path.substring("/~".length());
    } else if (path.startsWith("/user/")) {
      subPath = path.substring("/user/".length());
    } else if (path.startsWith("/group/")) {
      subPath = path.substring("/group/".length());
    }
    if (subPath != null) {
      String[] elements = StringUtils.split(subPath, "/", 2);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Got Elements Path [{}] ", Arrays.toString(elements));
      }
      if (elements.length >= 1) {
        Session session = StorageClientUtils.adaptToSession(resourceResolver.adaptTo(javax.jcr.Session.class));
        AuthorizableManager um = session.getAuthorizableManager();
        Authorizable a = um.findAuthorizable(elements[0]);
        if (a != null) {
          String userPath = LitePersonalUtils.getHomePath(a.getId());
          if (elements.length == 2) {
            userPath = userPath + "/" + elements[1];
          }
          ContentManager contentManager = session.getContentManager();
          Content content = contentManager.get(userPath);
          LOGGER.debug("Resolving [{}] to [{}] ", userPath, content);
          if (content != null) {
            SparseContentResource cpr = new SparseContentResource(content, session,
                resourceResolver, path);
            cpr.getResourceMetadata().put(HOME_RESOURCE_PROVIDER, this);
            return cpr;
          }
        }
      }
    }
    return null;
  }

  public Iterator<Resource> listChildren(Resource parent) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("List Children [{}] ", parent.getPath());
    }
    return null;
  }

}