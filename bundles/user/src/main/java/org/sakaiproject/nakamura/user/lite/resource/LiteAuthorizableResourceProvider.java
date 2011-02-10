/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.user.lite.resource;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

@Component(immediate = true, metatype = true)
@Service(value = ResourceProvider.class)
@Properties(value = {
    @Property(name = "provider.roots", value = "/system/userManager/"),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class LiteAuthorizableResourceProvider implements ResourceProvider {

  /**
   * default log
   */
  private final Logger log = LoggerFactory.getLogger(getClass());

  public static final String SYSTEM_USER_MANAGER_PATH = "/system/userManager";

  public static final String SYSTEM_USER_MANAGER_USER_PATH = SYSTEM_USER_MANAGER_PATH
      + "/user";

  public static final String SYSTEM_USER_MANAGER_GROUP_PATH = SYSTEM_USER_MANAGER_PATH
      + "/group";

  public static final String SYSTEM_USER_MANAGER_USER_PREFIX = SYSTEM_USER_MANAGER_USER_PATH
      + "/";

  public static final String SYSTEM_USER_MANAGER_GROUP_PREFIX = SYSTEM_USER_MANAGER_GROUP_PATH
      + "/";

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache
   * .sling.api.resource.ResourceResolver, javax.servlet.http.HttpServletRequest,
   * java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver,
      HttpServletRequest request, String path) {
    return getResource(resourceResolver, path);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache
   * .sling.api.resource.ResourceResolver, java.lang.String)
   */
  public Resource getResource(ResourceResolver resourceResolver, String path) {

    // handle resources for the virtual container resources
    if (path.equals(SYSTEM_USER_MANAGER_PATH)) {
      return new SyntheticResource(resourceResolver, path, "sparse/userManager");
    } else if (path.equals(SYSTEM_USER_MANAGER_USER_PATH)) {
      return new SyntheticResource(resourceResolver, path, "sparse/users");
    } else if (path.equals(SYSTEM_USER_MANAGER_GROUP_PATH)) {
      return new SyntheticResource(resourceResolver, path, "sparse/groups");
    }

    // the principalId should be the first segment after the prefix
    String pid = null;
    if (path.startsWith(SYSTEM_USER_MANAGER_USER_PREFIX)) {
      pid = path.substring(SYSTEM_USER_MANAGER_USER_PREFIX.length());
    } else if (path.startsWith(SYSTEM_USER_MANAGER_GROUP_PREFIX)) {
      pid = path.substring(SYSTEM_USER_MANAGER_GROUP_PREFIX.length());
    }

    if (pid != null) {
      if (pid.indexOf('/') != -1) {
        return null; // something bogus on the end of the path so bail
                     // out now.
      }
      if ("jcr:content".equals(pid)) {
        return null; // dont try and resolve the content subnode and waste 6ms
      }
      long start = System.currentTimeMillis();
      try {
        Session session = StorageClientUtils.adaptToSession(resourceResolver
            .adaptTo(javax.jcr.Session.class));
        AuthorizableManager authorizableManager = session.getAuthorizableManager();
        Authorizable authorizable = authorizableManager.findAuthorizable(pid);
        if (authorizable != null) {
          return new LiteAuthorizableResource(authorizable, resourceResolver, path);
        }
        log.debug("Failed to resolve {} ", path);
      } catch (StorageClientException e) {
        throw new SlingException("Error looking up Authorizable for principal: " + pid, e);
      } catch (AccessDeniedException e) {
        throw new SlingException("Error looking up Authorizable for principal: " + pid, e);
      } finally {
        log.debug("Resolution took {} ms {} ", System.currentTimeMillis() - start, path);
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.sling.api.resource.ResourceProvider#listChildren(org.apache
   * .sling.api.resource.Resource)
   */
  public Iterator<Resource> listChildren(Resource parent) {
    if (parent == null) {
      throw new NullPointerException("parent is null");
    }
    String path = parent.getPath();
    ResourceResolver resourceResolver = parent.getResourceResolver();

    // handle children of /system/userManager
    if (SYSTEM_USER_MANAGER_PATH.equals(path)) {
      List<Resource> resources = new ArrayList<Resource>();
      if (resourceResolver != null) {
        resources.add(getResource(resourceResolver, SYSTEM_USER_MANAGER_USER_PATH));
        resources.add(getResource(resourceResolver, SYSTEM_USER_MANAGER_GROUP_PATH));
      }
      return resources.iterator();
    }
    return null;
  }

}
