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
package org.sakaiproject.nakamura.basiclti;

import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_ADMIN_NODE_NAME;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.TOPIC_BASICLTI_ADDED;
import static org.sakaiproject.nakamura.basiclti.LiteBasicLTIServletUtils.getInvalidUserPrivileges;
import static org.sakaiproject.nakamura.basiclti.LiteBasicLTIServletUtils.isAdminUser;
import static org.sakaiproject.nakamura.basiclti.LiteBasicLTIServletUtils.removeProperty;
import static org.sakaiproject.nakamura.basiclti.LiteBasicLTIServletUtils.sensitiveKeys;
import static org.sakaiproject.nakamura.basiclti.LiteBasicLTIServletUtils.unsupportedKeys;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permission;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.AbstractSparsePostOperation;
import org.sakaiproject.nakamura.api.resource.lite.SparseNonExistingResource;
import org.sakaiproject.nakamura.api.resource.lite.SparsePostOperation;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;

@ServiceDocumentation(name = "Basic LTI Post Operation", okForVersion = "0.11",
  shortDescription = "Adds properties to a node for use with Basic LTI.",
  description = "Sets up a node to be used as configuration for integration with Basic LTI",
  methods = {
    @ServiceMethod(name = "POST",
      parameters = {
        @ServiceParameter(name = ":operation=basiclti", description = "The operation to specify when posting to trigger this operation.")
      },
      description = {
        "Adds any provided properties to the node being posted to for use in BasicLTI integration. Properties ending with @Delete are removed."
      }
    )
  })
@Component(immediate = true)
@Service(value = SparsePostOperation.class)
@Properties(value = {
    @Property(name = "sling.post.operation", value = "basiclti"),
    @Property(name = "service.description", value = "Creates a sakai/basiclti settings node."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class LiteBasicLTIPostOperation extends AbstractSparsePostOperation {
  private static final Logger LOG = LoggerFactory
      .getLogger(LiteBasicLTIPostOperation.class);
  /**
   * Dependency injected from OSGi container.
   */
  @Reference
  private transient Repository repository;

  @Reference
  protected transient EventAdmin eventAdmin;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.servlets.post.AbstractSlingPostOperation#doRun(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      ContentManager contentManager, List<Modification> changes, String contentPath)
      throws StorageClientException, AccessDeniedException, IOException {
    if (request == null || response == null || contentManager == null
        || contentPath == null) {
      throw new IllegalArgumentException();
    }
    final Session session = StorageClientUtils.adaptToSession(request
        .getResourceResolver().adaptTo(javax.jcr.Session.class));
    final Resource resource = request.getResource();
    String path = contentPath;
    Content node = resource.adaptTo(Content.class);
    if (node == null) { // create the node
      if (resource instanceof SparseNonExistingResource) {
        SparseNonExistingResource nonExistingResource = (SparseNonExistingResource) resource;
        path = nonExistingResource.getTargetContentPath();
      }
      if (contentManager.exists(path)) {
        // I don't think we should end up here often if at all
        node = contentManager.get(path);
      } else {
        node = new Content(path, new HashMap<String, Object>());
      }
    }
    try {
      final Map<String, String> sensitiveData = new HashMap<String, String>(
          sensitiveKeys.size());
      // loop through request parameters
      final RequestParameterMap requestParameterMap = request.getRequestParameterMap();
      for (final Entry<String, RequestParameter[]> entry : requestParameterMap.entrySet()) {
        final String key = entry.getKey();
        if (key.endsWith("@TypeHint")) {
          continue;
        }
        final RequestParameter[] requestParameterArray = entry.getValue();
        if (requestParameterArray == null || requestParameterArray.length == 0) {
          // maybe this should be removed if not needed or breaks contract
          // removeProperty(node, key);
        } else {
          if (requestParameterArray.length > 1) {
            throw new ServletException("Multi-valued parameters are not supported");
          } else {
            final String value = requestParameterArray[0].getString("UTF-8");
            if ("".equals(value)) {
              removeProperty(node, key);
            } else { // has a valid value
              if (sensitiveKeys.contains(key)) {
                sensitiveData.put(key, value);
              } else {
                if (!unsupportedKeys.contains(key)) {
                  final String typeHint = key + "@TypeHint";
                  if (requestParameterMap.containsKey(typeHint)
                      && "Boolean".equals(requestParameterMap.get(typeHint)[0]
                          .getString())) {
                    node.setProperty(key,
                        Boolean.valueOf(value));
                  } else {
                    node.setProperty(key, value);
                  }
                }
              }
            }
          }
        }
      } // end request parameters loop
      // safety precaution - just to be safe
      for (String skey : sensitiveKeys) {
        removeProperty(node, skey);
      }
      contentManager.update(node);
      createSensitiveNode(node, session, sensitiveData);

      // Send an OSGi event.
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(UserConstants.EVENT_PROP_USERID, request.getRemoteUser());
      EventUtils.sendOsgiEvent(properties, TOPIC_BASICLTI_ADDED, eventAdmin);
    } catch (Throwable e) {
      throw new StorageClientException(contentPath, e);
    }
  }

  private void createSensitiveNode(final Content parent, final Session userSession,
      Map<String, String> sensitiveData) {
    if (parent == null) {
      throw new IllegalArgumentException("Node parent==null");
    }
    // if (!"sakai/basiclti".equals(parent.getProperty("sling:resourceType"))) {
    // throw new
    // IllegalArgumentException("sling:resourceType != sakai/basiclti");
    // }
    if (userSession == null) {
      throw new IllegalArgumentException("userSession == null");
    }
    if (sensitiveData == null || sensitiveData.isEmpty()) {
      // do nothing - virtual tool use case
      return;
    }
    final String adminNodePath = parent.getPath() + "/" + LTI_ADMIN_NODE_NAME;
    // now let's elevate Privileges and do some admin modifications
    Session adminSession = null;
    try {
      adminSession = repository.loginAdministrative();
      final Content adminNode = new Content(adminNodePath, new HashMap<String, Object>());
      // final Content adminNode = JcrUtils.deepGetOrCreateNode(adminSession,
      // adminNodePath);
      for (final Entry<String, String> entry : sensitiveData.entrySet()) {
        adminNode.setProperty(entry.getKey(),
            entry.getValue());
      }
      adminSession.getContentManager().update(adminNode);
      // ensure only admins can read the node
      accessControlSensitiveNode(adminNodePath, adminSession, userSession.getUserId());
      // if (adminSession.hasPendingChanges()) {
      // adminSession.save();
      // }
    } catch (AccessDeniedException e) {
      LOG.error(e.getLocalizedMessage(), e);
      throw new IllegalStateException(e);
    } catch (StorageClientException e) {
      LOG.error(e.getLocalizedMessage(), e);
      throw new IllegalStateException(e);
    } finally {
      if (adminSession != null) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOG.error(e.getLocalizedMessage(), e);
          throw new IllegalStateException(e);
        }
      }
    } // end admin elevation
    // sanity check to verify user does not have permissions to sensitive node
    boolean invalidPrivileges = false;
    if (!isAdminUser(userSession)) { // i.e. normal user
      try {
        final AccessControlManager acm = userSession.getAccessControlManager();
        Permission[] userPrivs = acm.getPermissions(Security.ZONE_CONTENT, adminNodePath);
        if (userPrivs != null && userPrivs.length > 0) {
          Set<Permission> invalidUserPrivileges = getInvalidUserPrivileges(acm);
          for (Permission privilege : userPrivs) {
            if (invalidUserPrivileges.contains(privilege)) {
              invalidPrivileges = true;
              break;
            }
          }
        }
      } catch (StorageClientException e) {
        LOG.debug("The node does not exist or the user does not have permission(?): {}",
            adminNodePath);
      }
    }
    if (invalidPrivileges) {
      LOG.error("{} has invalid privileges: {}", userSession.getUserId(), adminNodePath);
      throw new IllegalStateException(userSession.getUserId()
          + " has invalid privileges: " + adminNodePath);
    }
  }

  /**
   * Apply the necessary access control entries so that only admin users can read/write
   * the sensitive node.
   * 
   * @param sensitiveNodePath
   * @param adminSession
   * @throws StorageClientException
   * @throws AccessDeniedException
   */
  private void accessControlSensitiveNode(final String sensitiveNodePath,
      final Session adminSession, String currentUserId) throws StorageClientException,
      AccessDeniedException {

    adminSession.getAccessControlManager().setAcl(
        Security.ZONE_CONTENT,
        sensitiveNodePath,
        new AclModification[] {
            new AclModification(AclModification.denyKey(User.ANON_USER), Permissions.ALL
                .getPermission(), Operation.OP_REPLACE),
            new AclModification(AclModification.denyKey(Group.EVERYONE), Permissions.ALL
                .getPermission(), Operation.OP_REPLACE),
            new AclModification(AclModification.denyKey(currentUserId), Permissions.ALL
                .getPermission(), Operation.OP_REPLACE) });
  }

  /**
   * @param slingRepository
   */
  protected void bindRepository(Repository repository) {
    this.repository = repository;
  }

  /**
   * @param slingRepository
   */
  protected void unbindRepository(Repository repository) {
    this.repository = null;
  }

}
