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

package org.sakaiproject.nakamura.connections;

import static org.sakaiproject.nakamura.api.connections.ConnectionConstants.CONTACT_STORE_NAME;

import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.util.LitePersonalUtils;

/**
 * Simple utils which help us when working with connections
 * 
 */
public class ConnectionUtils {



  /**
   * Builds the path to the connection node.
   * 
   * @param user
   *          the user who owns the connection
   * @param targetUser
   *          the target user of the connection
   * @param remainderPath
   *          any path after the name of the node, including selectors eg .accept.html
   *          would results in /_users/connect/xx/yy/zz/ieb/xz/xy/zy/nico.accept.html,
   *          this is not an absolute path fragment and may start half way through an
   *          element. / is not used to separate.
   * @return the path to the connection node or subtree node.
   */
  
  public static String getConnectionPath(Authorizable user, Authorizable targetUser, String remainderPath ) {
      return getConnectionPath(user.getId(), targetUser.getId(), remainderPath);
    }
  private static String getConnectionPath(String user, String targetUser, 
      String remainderPath) {
    StringBuilder sb = new StringBuilder();
    sb.append(getConnectionPathBase(user));
    if (remainderPath == null || remainderPath.trim().length() == 0) {
      sb.append("/").append(targetUser);
    } else {
      sb.append("/").append(targetUser).append(remainderPath);
    }
    return sb.toString();
  }

  /**
   * Builds the path to the connection node.
   * 
   * @param user
   *          the user who owns the connection
   * @param targetUser
   *          the target user of the connection
   * @return the path to the connection node or subtree node.
   */
  public static String getConnectionPath(Authorizable user, Authorizable targetUser) {
    return getConnectionPath(user, targetUser, null);
  }
  public static String getConnectionPath(String user, String targetUser) {
    return getConnectionPath(user, targetUser, null);
  }


  /**
   * @param au
   *          The <code>authorizable</code> to get the connection folder for.
   * @return The absolute path to the connection folder in a user his home folder. ex:
   *         /_user/j/jo/joh/john/johndoe/contacts
   */
  public static String getConnectionPathBase(Authorizable au) {
    return LitePersonalUtils.getHomePath(au.getId()) + "/" + CONTACT_STORE_NAME;
  }
  
  public static String getConnectionPathBase(String au) {
    return LitePersonalUtils.getHomePath(au) + "/" + CONTACT_STORE_NAME;
  }

}
