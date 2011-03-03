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


import org.sakaiproject.nakamura.api.lite.content.Content;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 */
public class LitePersonalUtils {

  /**
   * Property name for the e-mail property of a user's profile
   */
  public static final String PROP_EMAIL_ADDRESS = "email";
  /**
   * The base location of the user's or group's space in storage.
   */
  public static final String PATH_AUTHORIZABLE = "a:";
  /**
   * The base location of the user's or group's space as a Sling Resource.
   */
  public static final String PATH_RESOURCE_AUTHORIZABLE = "/~";

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



  public static String getPrimaryEmailAddress(Content profileNode){
    if (profileNode.hasProperty(PROP_EMAIL_ADDRESS)) {
      return (String) profileNode.getProperty(PROP_EMAIL_ADDRESS);
    }
    return null;
  }

  public static String[] getEmailAddresses(Content profileNode) {
    if (profileNode.hasProperty(PROP_EMAIL_ADDRESS)) {
      return (String[]) profileNode.getProperty(PROP_EMAIL_ADDRESS);
    }
    return null;
  }

  public static String getPreferredMessageTransport(Content profileNode) {
    if (profileNode.hasProperty(PROP_PREFERRED_MESSAGE_TRANSPORT)) {
      return (String) profileNode.getProperty(PROP_PREFERRED_MESSAGE_TRANSPORT);
    }
    return null;
  }

  /**
   * @param au
   *          The authorizable to get the authprofile path for.
   * @return The absolute path in Sparse storage to the authprofile node that contains all the
   *         profile information.
   */
  public static String getProfilePath(String id) {
    return getPublicPath(id) + "/" + PATH_AUTH_PROFILE;
  }

  /**
   * @param au
   *          The authorizable to get the private path for.
   * @return The absolute path in Sparse storage to the private folder in the user his home folder.
   */
  public static String getPrivatePath(String id) {
    return getHomePath(id) + "/" + PATH_PRIVATE;
  }

  /**
   * @param au
   *          The authorizable to get the public path for.
   * @return The absolute path in Sparse storage to the public folder in the user his home folder.
   */
  public static String getPublicPath(String id) {
    return getHomePath(id) + "/" + PATH_PUBLIC;
  }



  /**
   * Get the home folder for an authorizable. If the authorizable is a user, this might
   * return: a:testuser
   *
   * @param au
   *          The authorizable to get the home folder for.
   * @return The absolute path in Sparse storage to the home folder for an authorizable.
   */
  public static String getHomePath(String id) {
    return PATH_AUTHORIZABLE+id;
  }


  /**
   * Get the Resource path of an Authorizable's. If the authorizable is a user, this might
   * return: /~testuser
   *
   * @param au
   *          The authorizable to get the home folder for.
   * @return The absolute Resource path to the home folder for an authorizable.
   */
  public static String getHomeResourcePath(String id) {
    return PATH_RESOURCE_AUTHORIZABLE+id;
  }

  private static Pattern homePathPattern = Pattern.compile("^(.*)(~([\\w-]*?))/");

  /**
   * Expand home directory resource path into a content path.
   * ~user => a:user
   *
   * @param session
   * @param path
   * @return
   */
  public static String expandHomeDirectory(String path) {
    Matcher homePathMatcher = homePathPattern.matcher(path);
    if (homePathMatcher.find()) {
      String username = homePathMatcher.group(3);
      String homePrefix = homePathMatcher.group(1);
      String userHome = LitePersonalUtils.getHomePath(username);
      String homePath = homePrefix + userHome + "/";
      String prefix = "";
      if (homePathMatcher.start() > 0) {
        prefix = path.substring(0, homePathMatcher.start());
      }
      String suffix = path.substring(homePathMatcher.end());
      path = prefix + homePath + suffix;
    }
    return path;
  }
}
