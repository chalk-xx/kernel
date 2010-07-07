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

package org.sakaiproject.nakamura.privacy;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

@Component(name = "org.sakaiproject.nakamura.privacy.HomeResourceProvider", immediate = true, metatype = true, description = "%homeprovider.description", label = "%homeprovider.name")
@Service(value = ResourceProvider.class)
@Property(name = ResourceProvider.ROOTS, value = { "/user", "/group" })
public class HomeResourceProvider implements ResourceProvider {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(HomeResourceProvider.class);
  private Map<String, String[]> authorizableMap = new HashMap<String, String[]>();

  public HomeResourceProvider() {
    authorizableMap.put("/user/", new String[] { "/_user",
        "/rep:security/rep:authorizables/rep:users" });
    authorizableMap.put("/group/", new String[] { "/_group",
        "/rep:security/rep:authorizables/rep:groups" });
  }

  public Resource getResource(ResourceResolver resourceResolver,
      HttpServletRequest request, String path) {
    LOGGER.info("Got Resource URI [{}]  Path [{}] ", request.getRequestURI(), path);
    return getResource(resourceResolver, path);
  }

  public Resource getResource(ResourceResolver resourceResolver, String path) {
    LOGGER.debug("Got Resource Path [{}] ", path);
    if ( "/user".equals(path) || "/group".equals(path) ) {
      return null;
    }
    for (Entry<String, String[]> authorizableMapping : authorizableMap.entrySet()) {
      try {
        Resource r = resolveMappedResource(resourceResolver, path, authorizableMapping);
        if (r != null) {
          return r;
        }
      } catch (RepositoryException e) {
        LOGGER.warn(e.getMessage(), e);
      }
    }
    return null;
  }

  private Resource resolveMappedResource(ResourceResolver resourceResolver, String path,
      Entry<String, String[]> authorizableMapping) throws RepositoryException {
    String pathStart = authorizableMapping.getKey();
    String targetStart = authorizableMapping.getValue()[0];
    String principalPathStart = authorizableMapping.getValue()[1];
    if (path.startsWith(pathStart)) {
      String[] elements = StringUtils.split(path, "/", 3);
      LOGGER.debug("Got Elements Path [{}] ", Arrays.toString(elements));
      if (elements.length >= 2) {
        Session session = resourceResolver.adaptTo(Session.class);
        PrincipalManager pm = AccessControlUtil.getPrincipalManager(session);
        Principal p = pm.getPrincipal(elements[1]);
        if (p instanceof ItemBasedPrincipal) {

          ItemBasedPrincipal ibp = (ItemBasedPrincipal) p;

          String userPath = targetStart
              + ibp.getPath().substring(principalPathStart.length());
          if (elements.length == 3) {
            userPath = userPath + "/" + elements[2];
          }
          Resource r = resourceResolver.resolve(userPath);
          LOGGER.debug("Resolving [{}] to [{}] ", userPath, r);
          if (r != null) {
            // are the last elements the same ?
            if (getLastElement(r.getPath()).equals(getLastElement(path))) {
              return r;
            }
          }
        }
      }
    }
    return null;
  }

  private String getLastElement(String path) {
    for ( int i = path.length()-1; i >= 0; i-- ) {
      if ( path.charAt(i) == '/' ) {
        return path.substring(i);
      }
    }
    return path;
  }

  public Iterator<Resource> listChildren(Resource parent) {
    LOGGER.debug("List Children [{}] ", parent.getPath());
    return null;
  }

}
