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

package org.sakaiproject.nakamura.files.pool;

import static javax.jcr.security.Privilege.JCR_ALL;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.cluster.ClusterServer;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

@Component(name = "org.sakaiproject.nakamura.files.pool.ContentPoolProvider", immediate = true, metatype = true, description = "%contentpool.description", label = "%contentpool.name")
@Service(value = ResourceProvider.class)
@Property(name = ResourceProvider.ROOTS, value = { "/", "/p" })
public class ContentPoolProvider implements ResourceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContentPoolProvider.class);
  public static final String CONTENT_RESOURCE_PROVIDER = ContentPoolProvider.class.getName();
  public static final char[] ENCODING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
  public static final char[] HASHENCODING = "abcdefghijklmnopqrstuvwxyz1234567890".toCharArray();
  // this 36*36 = 1296, so /a/aa/aa/aa will have 36 at the first level, then 46656 at the second and then 60M, then 7e10 items at the last level.
  
  
  @Reference
  protected ClusterTrackingService clusterTrackingService;
  @Reference
  protected SlingRepository slingRepository;
  private String serverId;
  private long startingPoint;
  private Object lock = new Object();
  
  public void activate(ComponentContext componentContext) {
    synchronized (lock) {
      serverId = clusterTrackingService.getCurrentServerId();
      startingPoint = System.currentTimeMillis();      
    }
  }
  

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
    if (!(c == 'p')) {
      return null;
    }
    try {
      return resolveMappedResource(resourceResolver, path);
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return null;
  }

  private Resource resolveMappedResource(ResourceResolver resourceResolver, String path)
      throws RepositoryException {
    String poolId = null;
   
    if (path.startsWith("/p/")) {
      poolId = path.substring("/p/".length());
    } else if ( path.length() == 2) {
      try {
        poolId = generatePoolId();
        // we also need to create the node.
        Session adminSession = null;
        
        Session userSession = resourceResolver.adaptTo(Session.class);
        try {
          adminSession = slingRepository.loginAdministrative(null);
          
          String userId = userSession.getUserID();
          PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(userSession);
          Principal userPrincipal = principalManager.getPrincipal(userId);
          
          Node node = JcrUtils.deepGetOrCreateNode(adminSession, hash(poolId));
          // make the node inherit the repository defaults for content, but admin for the user.
          
          String nodePath = node.getPath();
          AccessControlUtil.replaceAccessControlEntry(adminSession, nodePath, userPrincipal, new String[] { JCR_ALL }, null, null, null);
         
          // set some properties to make it possible to locate this pool file without having to use the path.
          node.setProperty("sakai:pool-file", "1");
          node.setProperty("sakai:pool-file-owner", userId);

          
          // save so the resolver further down will find this file.
          if ( adminSession.hasPendingChanges() ) {
            adminSession.save();
          }
        } finally {
          adminSession.logout();
        }
      } catch (Exception e) {
        throw new RepositoryException("Unable to generate new pool ID "+e.getMessage(),e );
      }
    }
    if (poolId != null && poolId.length() > 0) {
      int i = poolId.indexOf('/');
      if (i > 0) {
        poolId = poolId.substring(0, i);
      }
      i = poolId.indexOf('.');
      String selectors = "";
      if ( i > 0 ) {
        selectors = poolId.substring(i);
        poolId = poolId.substring(0, i);
      }
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Pool ID is [{}]", poolId);
      }
      String poolPath = null;
      try {
        poolPath = hash(poolId)+selectors;
      } catch (Exception e) {
        throw new RepositoryException("Unable to hash pool ID "+e.getMessage(),e );
      }
      Resource r = resourceResolver.resolve(poolPath);
      if ( r instanceof NonExistingResource ) {
        LOGGER.info("Pool ID does not exist, reject and dont allow creation on POST {} ", poolPath);
        throw new SlingException("Resources may not be created at /p by the user", new AccessDeniedException("Cant create user specified pool resoruce"));
      }
      LOGGER.info("Resolving [{}] to [{}] ", poolPath, r);
      if (r != null) {
        // are the last elements the same ?
        if (getLastElement(r.getPath()).equals("/"+poolId)) {
          r.getResourceMetadata().put(CONTENT_RESOURCE_PROVIDER,
              this);
          return r;
        } else {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Rejected [{}] != [{}] ", getLastElement(r.getPath()),
                "/"+poolId);
          }
        }
      }

    }
    return null;
  }
  
  private String hash(String poolId) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    String encodedId = StringUtils.encode(md.digest(poolId.getBytes("UTF-8")), HASHENCODING);
    LOGGER.info("Hashing [{}] gave [{}] ",poolId,encodedId);
    return "/_p/"+encodedId.charAt(0)+"/"+encodedId.substring(1,3)+"/"+encodedId.substring(3,5)+"/"+encodedId.substring(5,7)+"/"+poolId;
  }


  private String getLastElement(String path) {
    for (int i = path.length() - 1; i >= 0; i--) {
      if (path.charAt(i) == '/') {
        return path.substring(i);
      }
    }
    return "/" + path;
  }

  private String generatePoolId() throws UnsupportedEncodingException, NoSuchAlgorithmException {
    synchronized (lock) {
      String newId = String.valueOf(startingPoint++) + "-" + serverId;
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      return StringUtils.encode(md.digest(newId.getBytes("UTF-8")), ENCODING);      
    }
  }

  

  public Iterator<Resource> listChildren(Resource parent) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("List Children [{}] ", parent.getPath());
    }
    return null;
  }

}
