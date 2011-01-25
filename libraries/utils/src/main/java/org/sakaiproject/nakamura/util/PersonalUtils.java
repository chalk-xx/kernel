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
package org.sakaiproject.nakamura.util;


import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * @deprecated This class is deprecated pending a move to Sparse storage. LitePersonalUtils is the replacement.
 */

@Deprecated
public class PersonalUtils {

  /**
   * Property name for the e-mail property of a user's profile
   */
  public static final String PROP_EMAIL_ADDRESS = "email";
  /**
   * The base location of the user space.
   */
  public static final String PATH_USER = "/_user";
  /**
   * The base location of the group space.
   */
  public static final String PATH_GROUP = "/_group";
  
  /**
   * The node name of the authentication profile in public space.
   */
  public static final String PATH_AUTH_PROFILE = "authprofile";
  /**
   * The name of the private folder
   */
  public static final String PATH_PRIVATE = "private";
  /**
   * The name of the public folder
   */
  public static final String PATH_PUBLIC = "public";

  /**
   * Property name for the user's preferred means of message delivery
   */
  public static final String PROP_PREFERRED_MESSAGE_TRANSPORT = "preferredMessageTransport";


  private static final Logger LOGGER = LoggerFactory.getLogger(PersonalUtils.class);

  /**
   * @param au
   *          The authorizable to get the hashed path for.
   * @return The hashed path (ex: a/ad/adm/admi/admin/)
   * @throws RepositoryException
   */
  public static String getUserHashedPath(Authorizable au) throws RepositoryException {
    String hash = null;
    if (au.hasProperty("path")) {
      hash = au.getProperty("path")[0].getString();
    } else {
      LOGGER
          .debug(
              "Authorizable {} has no path property set on it, grabbing hash from ItemBasedPrincipal!",
              au);
      Principal p = au.getPrincipal();
      if (p instanceof ItemBasedPrincipal) {
        ItemBasedPrincipal principal = (ItemBasedPrincipal) p;
        hash = principal.getPath();
      } else if (p.getName().equals("admin")) {
        hash = "a/ad/admin/";
      } else if (p.getName().equals("anonymous")) {
        hash = "a/an/anonymous/";
      } else {
        String n = org.apache.commons.lang.StringUtils.leftPad(p.getName(), 5, '_');
        hash = n.substring(0, 1) + "/" + n.substring(0, 2) + "/" + n.substring(0, 3)
            + "/" + n + "/";
      }
    }
    return hash;
  }

  public static String getPrimaryEmailAddress(Node profileNode)
      throws RepositoryException {
    String addr = null;
    if (profileNode.hasProperty(PROP_EMAIL_ADDRESS)) {
      Value[] addrs = JcrUtils.getValues(profileNode, PROP_EMAIL_ADDRESS);
      if (addrs.length > 0) {
        addr = addrs[0].getString();
      }
    }
    return addr;
  }

  public static String[] getEmailAddresses(Node profileNode) throws RepositoryException {
    String[] addrs = null;
    if (profileNode.hasProperty(PROP_EMAIL_ADDRESS)) {
      Value[] vaddrs = JcrUtils.getValues(profileNode, PROP_EMAIL_ADDRESS);
      addrs = new String[vaddrs.length];
      for (int i = 0; i < addrs.length; i++) {
        addrs[i] = vaddrs[i].getString();
      }
    }
    return addrs;
  }

  public static String getPreferredMessageTransport(Node profileNode)
      throws RepositoryException {
    String transport = null;
    if (profileNode.hasProperty(PROP_PREFERRED_MESSAGE_TRANSPORT)) {
      transport = profileNode.getProperty(PROP_PREFERRED_MESSAGE_TRANSPORT)
          .getString();
    }
    return transport;
  }

  /**
   * @param au
   *          The authorizable to get the authprofile path for.
   * @return The absolute path in JCR to the authprofile node that contains all the
   *         profile information.
   */
  public static String getProfilePath(Authorizable au) {
    return getPublicPath(au) + "/" + PATH_AUTH_PROFILE;
  }

  /**
   * @param au
   *          The authorizable to get the private path for.
   * @return The absolute path in JCR to the private folder in the user his home folder.
   */
  public static String getPrivatePath(Authorizable au) {
    return getHomePath(au) + "/" + PATH_PRIVATE;
  }

  /**
   * @param au
   *          The authorizable to get the public path for.
   * @return The absolute path in JCR to the public folder in the user his home folder.
   */
  public static String getPublicPath(Authorizable au) {
    return getHomePath(au) + "/" + PATH_PUBLIC;
  }



  /**
   * Get the home folder for an authorizable. If the authorizable is a user, this might
   * return: /_user/t/te/tes/test/testuser
   * 
   * @param au
   *          The authorizable to get the home folder for.
   * @return The absolute path in JCR to the home folder for an authorizable.
   */
  public static String getHomePath(Authorizable au) {
    String folder = PathUtils.getSubPath(au);
    if (au != null && au.isGroup()) {
      folder = PATH_GROUP + folder;
    } else {
      // Assume this is a user.
      folder = PATH_USER + folder;
    }
    return PathUtils.normalizePath(folder);
  }


  /**
   * @param session
   *          The Jackrabbit session.
   * @param id
   *          The id of an authorizable.
   * @return An authorizable that represents a person.
   * @throws RepositoryException
   */
  public static Authorizable getAuthorizable(Session session, String id)
      throws RepositoryException {
    UserManager um = AccessControlUtil.getUserManager(session);
    return um.getAuthorizable(id);
  }



}
