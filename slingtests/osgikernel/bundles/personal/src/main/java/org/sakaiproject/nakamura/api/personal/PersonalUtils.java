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
package org.sakaiproject.nakamura.api.personal;

import static org.sakaiproject.nakamura.api.personal.PersonalConstants.AUTH_PROFILE;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants.PRIVATE;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants.PUBLIC;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants._GROUP;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants._USER;

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * 
 */
public class PersonalUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(PersonalUtils.class);

  /**
   * Writes userinfo out for a property in a node. Make sure that the resultNode has a
   * property with propertyName that contains a userid.
   * 
   * @param session
   *          The JCR Session
   * @param user
   *          The user name
   * @param write
   *          The writer to write to.
   * @param jsonName
   *          The json name that should be used.
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws RepositoryException
   * @throws JSONException
   */
  public static void writeUserInfo(Session session, String user, JSONWriter write,
      String jsonName) {
    try {
      Authorizable au = getAuthorizable(session, user);
      String path = PersonalUtils.getProfilePath(au);
      LOGGER.info("Hashing {} as {} ",au.getID(),path);
      String hash = getUserHashedPath(au);
      Node userNode = (Node) session.getItem(path);
      // We can't have anymore exceptions from now on.
      write.key(jsonName);
      write.object();
      write.key("hash");
      write.value(hash);
      ExtendedJSONWriter.writeNodeContentsToWriter(write, userNode);
      write.endObject();

    } catch (PathNotFoundException pnfe) {
      LOGGER.warn("Profile path not found for this user.");
    } catch (Exception ex) {
      LOGGER.warn(ex.getMessage());
    }
  }

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
      LOGGER.warn("Authorizable {} has no path property set on it, grabbing hash from ItemBasedPrincipal!", au);
      ItemBasedPrincipal principal = (ItemBasedPrincipal) au;
      hash = principal.getPath();
    }
    return hash;
  }

  /**
   * Write a small bit of information from an authprofile. userid, firstName, lastName,
   * picture.
   * 
   * @param session
   *          The {@link Session session} to access the authprofile.
   * @param user
   *          The userid to look up
   * @param write
   *          The {@link JSONWriter writer} to write to.
   */
  public static void writeCompactUserInfo(Session session, String user, JSONWriter write) {
    try {
      Authorizable au = getAuthorizable(session, user);
      String profilePath = PersonalUtils.getProfilePath(au);
      String hash = getUserHashedPath(au);
      write.object();
      write.key("userid");
      write.value(user);
      write.key("hash");
      write.value(hash);
      try {
        Node profileNode = (Node) session.getItem(profilePath);
        writeValue("firstName", profileNode, write);
        writeValue("lastName", profileNode, write);
        writeValue("picture", profileNode, write);
      } catch (RepositoryException e) {
        // The provided user-string is probably not a user id.
        LOGGER.error(e.getMessage(), e);
      }
      write.endObject();
    } catch (JSONException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  /**
   * Write the value of a property form the profileNode. If the property doesn't exist it
   * outputs "name": false.
   * 
   * @param string
   * @param profileNode
   * @throws RepositoryException
   * @throws JSONException
   */
  private static void writeValue(String name, Node profileNode, JSONWriter write)
      throws RepositoryException, JSONException {
    write.key(name);
    if (profileNode.hasProperty(name)) {
      write.value(profileNode.getProperty(name).getString());
    } else {
      write.value(false);
    }
  }

  /**
   * Writes userinfo out for a property in a node. Make sure that the resultNode has a
   * property with propertyName that contains a userid.
   * 
   * @param resultNode
   *          The node to look on
   * @param write
   *          The writer to write to.
   * @param propertyName
   *          The propertyname that contains the userid.
   * @param jsonName
   *          The json name that should be used.
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws RepositoryException
   * @throws JSONException
   */
  public static void writeUserInfo(Node resultNode, JSONWriter write,
      String propertyName, String jsonName) throws ValueFormatException,
      PathNotFoundException, RepositoryException, JSONException {
    String user = resultNode.getProperty(propertyName).getString();
    writeUserInfo(resultNode.getSession(), user, write, jsonName);
  }


  public static String getPrimaryEmailAddress(Node profileNode)
      throws RepositoryException {
    String addr = null;
    if (profileNode.hasProperty(PersonalConstants.EMAIL_ADDRESS)) {
      Value[] addrs = JcrUtils.getValues(profileNode, PersonalConstants.EMAIL_ADDRESS);
      if (addrs.length > 0) {
        addr = addrs[0].getString();
      }
    }
    return addr;
  }

  public static String[] getEmailAddresses(Node profileNode) throws RepositoryException {
    String[] addrs = null;
    if (profileNode.hasProperty(PersonalConstants.EMAIL_ADDRESS)) {
      Value[] vaddrs = JcrUtils.getValues(profileNode, PersonalConstants.EMAIL_ADDRESS);
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
    if (profileNode.hasProperty(PersonalConstants.PREFERRED_MESSAGE_TRANSPORT)) {
      transport = profileNode.getProperty(PersonalConstants.PREFERRED_MESSAGE_TRANSPORT)
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
    StringBuilder sb = new StringBuilder();
    sb.append(getPublicPath(au)).append("/").append(AUTH_PROFILE);
    return sb.toString();
  }

  /**
   * @param au
   *          The authorizable to get the private path for.
   * @return The absolute path in JCR to the private folder in the user his home folder.
   */
  public static String getPrivatePath(Authorizable au) {
    return getHomeFolder(au) + "/" + PRIVATE;
  }

  /**
   * @param au
   *          The authorizable to get the public path for.
   * @return The absolute path in JCR to the public folder in the user his home folder.
   */
  public static String getPublicPath(Authorizable au) {
    return getHomeFolder(au) + "/" + PUBLIC;
  }

  /**
   * Get the home folder for an authorizable. If the authorizable is a user, this might
   * return: /_user/t/te/tes/test/testuser
   * 
   * @param au
   *          The authorizable to get the home folder for.
   * @return The absolute path in JCR to the home folder for an authorizable.
   */
  public static String getHomeFolder(Authorizable au) {
    String folder = PathUtils.getSubPath(au);
    if (au.isGroup()) {
      folder = _GROUP + folder;
    } else {
      // Assume this is a user.
      folder = _USER + folder;
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
