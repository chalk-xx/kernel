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
package org.sakaiproject.nakamura.personal;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;

@Component(immediate = true, metatype = true)
@Service(value = AuthorizablePostProcessor.class)
@Properties(value = {
    @org.apache.felix.scr.annotations.Property(name = "service.vendor", value = "The Sakai Foundation"),
    @org.apache.felix.scr.annotations.Property(name = "service.description", value = "Post Processes profiles for User and Group operations"),
    @org.apache.felix.scr.annotations.Property(name = "service.ranking", intValue = 1) })
public class ProfilePostProcessor implements AuthorizablePostProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ProfilePostProcessor.class);

  @Reference(policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.OPTIONAL_UNARY)
  protected ContentImporter contentImporter;

  public static final String PROFILE_IMPORT_TEMPLATE_DEFAULT = "{'basic':{'elements':{'firstName':{'value':'@@firstName@@'},'lastName':{'value':'@@lastName@@'},'email':{'value':'@@email@@'}},'access':'everybody'}}";
  @org.apache.felix.scr.annotations.Property
  static final String PROFILE_IMPORT_TEMPLATE = "sakai.user.profile.template.default";
  private String defaultProfileTemplate;

  private ArrayList<String> profileParams = new ArrayList<String>();

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#process(org.apache.jackrabbit.api.security.user.Authorizable,
   *      javax.jcr.Session, org.apache.sling.servlets.post.Modification, java.util.Map)
   */
  public void process(Authorizable authorizable, Session session, Modification change,
      Map<String, Object[]> parameters) throws Exception {
    try {
      if (ModificationType.CREATE.equals(change.getType())) {
        Node profileNode = createProfile(session, authorizable);

        // Update the values on the profile node.
        updateProfileProperties(session, profileNode, authorizable, change, parameters);
      } else {
        if (!parameters.containsKey(":sakai:update-profile")
            || !"false".equals(parameters.get(":sakai:update-profile")[0])) {
          updateProfileProperties(session, getProfileNode(session, authorizable),
              authorizable, change, parameters);
        }
      }
      LOGGER.debug("DoneProcessing  {} ", authorizable.getID());
    } catch (Exception ex) {
      LOGGER.error("Post Processing failed " + ex.getMessage(), ex);
    }
  }

  /**
   * Decide whether post-processing this user or group would be redundant because it has
   * already been done. The current logic uses the existence of a profile node of the
   * correct type as a marker.
   * 
   * @param session
   * @param authorizable
   * @return true if there is evidence that post-processing has already occurred for this
   *         user or group
   * @throws RepositoryException
   */
  private boolean isPostProcessingDone(Session session, Authorizable authorizable)
      throws RepositoryException {
    boolean isProfileCreated = false;
    Node node = getProfileNode(session, authorizable);
    if (node != null) {
      String type = nodeTypeForAuthorizable(authorizable.isGroup());
      if (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
        if (node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString().equals(type)) {
          isProfileCreated = true;
        }
      }
    }
    return isProfileCreated;
  }

  private String nodeTypeForAuthorizable(boolean isGroup) {
    if (isGroup) {
      return UserConstants.GROUP_PROFILE_RESOURCE_TYPE;
    } else {
      return UserConstants.USER_PROFILE_RESOURCE_TYPE;
    }
  }

  private Node getProfileNode(Session session, Authorizable authorizable)
      throws RepositoryException {
    Node profileNode;
    String path = PersonalUtils.getProfilePath(authorizable);
    if (session.nodeExists(path)) {
      profileNode = session.getNode(path);
    } else {
      profileNode = null;
    }
    return profileNode;
  }

  /**
   * @param request
   * @param authorizable
   * @return
   * @throws RepositoryException
   */
  private Node createProfile(Session session, Authorizable authorizable)
      throws RepositoryException {
    String path = PersonalUtils.getProfilePath(authorizable);
    Node profileNode = null;
    if (!isPostProcessingDone(session, authorizable)) {
      String type = nodeTypeForAuthorizable(authorizable.isGroup());
      LOGGER.debug("Creating or resetting Profile Node {} for authorizable {} ", path,
          authorizable.getID());
      profileNode = JcrUtils.deepGetOrCreateNode(session, path);
      profileNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, type);
      // Make sure we can place references to this profile node in the future.
      // This will make it easier to search on it later on.
      if (profileNode.canAddMixin(JcrConstants.MIX_REFERENCEABLE)) {
        profileNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
      }
    } else {
      profileNode = session.getNode(path);
    }
    return profileNode;
  }

  /**
   * @param authorizable
   * @param changes
   * @throws RepositoryException
   * @throws ConstraintViolationException
   * @throws LockException
   * @throws VersionException
   * @throws PathNotFoundException
   */
  private void updateProfileProperties(Session session, Node profileNode,
      Authorizable authorizable, Modification change, Map<String, Object[]> parameters)
      throws RepositoryException {
    if (profileNode == null) {
      return;
    }

    // The current session does not necessarily have write access to
    // the Profile.
    if (isAbleToModify(session, profileNode.getPath())) {
      // If the client sent a parameter specifying new Profile content,
      // apply it now.
      String defaultProfile = processProfileParameters(defaultProfileTemplate,
          authorizable, parameters);
      ProfileImporter.importFromParameters(profileNode, parameters, contentImporter,
          session, defaultProfile);

      // build a blacklist set of properties that should be kept private
      Set<String> privateProperties = new HashSet<String>();
      if (profileNode.hasProperty(UserConstants.PRIVATE_PROPERTIES)) {
        Value[] pp = profileNode.getProperty(UserConstants.PRIVATE_PROPERTIES)
            .getValues();
        for (Value v : pp) {
          privateProperties.add(v.getString());
        }
      }
      // copy the non blacklist set of properties into the users profile.
      if (authorizable != null) {
        // explicitly add protected properties form the user authorizable
        if (!authorizable.isGroup() && !profileNode.hasProperty("rep:userId")) {
          profileNode.setProperty("rep:userId", authorizable.getID());
        }
        Iterator<?> inames = authorizable.getPropertyNames();
        while (inames.hasNext()) {
          String propertyName = (String) inames.next();
          // No need to copy in jcr:* properties, otherwise we would copy over the uuid
          // which could lead to a lot of confusion.
          if (!propertyName.startsWith("jcr:") && !propertyName.startsWith("rep:")) {
            if (!privateProperties.contains(propertyName)) {
              Value[] v = authorizable.getProperty(propertyName);
              if (!(profileNode.hasProperty(propertyName) && profileNode
                  .getProperty(propertyName).getDefinition().isProtected())) {
                if (v.length == 1) {
                  try {
                    profileNode.setProperty(propertyName, v[0]);
                  } catch (ValueFormatException vfe) {
                    profileNode.setProperty(propertyName, v);
                  }
                } else {
                  profileNode.setProperty(propertyName, v);
                }
              }
            }
          } else {
            LOGGER.debug("Not Updating {}", propertyName);
          }
        }
      }
    }
  }

  private String processProfileParameters(final String defaultProfileTemplate,
      final Authorizable auth, final Map<String, Object[]> parameters)
      throws RepositoryException {
    String retval = defaultProfileTemplate;
    for (String param : profileParams) {
      String val = "unknown";
      if (parameters.containsKey(param)) {
        val = (String) parameters.get(param)[0];
      } else if (auth.hasProperty(param)) {
        val = auth.getProperty(param)[0].getString();
      }
      retval = StringUtils.replace(retval, "@@" + param + "@@", val);
    }
    return retval;
  }

  private boolean isAbleToModify(Session session, String path) throws RepositoryException {
    AccessControlManager accessControlManager = AccessControlUtil.getAccessControlManager(session);
    Privilege[] modifyPrivileges = {
        accessControlManager.privilegeFromName(Privilege.JCR_MODIFY_PROPERTIES),
        accessControlManager.privilegeFromName(Privilege.JCR_ADD_CHILD_NODES),
        accessControlManager.privilegeFromName(Privilege.JCR_REMOVE_CHILD_NODES)
        };
    return accessControlManager.hasPrivileges(path, modifyPrivileges);
  }

  @Activate @Modified
  protected void modified(Map<?, ?> props) {
    defaultProfileTemplate = OsgiUtil.toString(props.get(PROFILE_IMPORT_TEMPLATE),
        PROFILE_IMPORT_TEMPLATE_DEFAULT);

    int startPos = defaultProfileTemplate.indexOf("@@");
    while (startPos > -1) {
      int endPos = defaultProfileTemplate.indexOf("@@", startPos + 2);
      if (endPos > -1) {
        String param = defaultProfileTemplate.substring(startPos + 2, endPos);
        profileParams.add(param);

        endPos = defaultProfileTemplate.indexOf("@@", endPos + 2);
      }
      startPos = endPos;
    }
  }

}
