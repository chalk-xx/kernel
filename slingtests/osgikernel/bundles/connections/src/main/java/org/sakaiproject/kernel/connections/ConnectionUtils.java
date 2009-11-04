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

package org.sakaiproject.kernel.connections;

import org.sakaiproject.kernel.util.PathUtils;

/**
 * Simple utils which help us when working with connections
 * 
 */
public class ConnectionUtils {
  /**
   * The root of the personal connections path. Needed because several ConnectionManager
   * clients supply only a user ID from which to retrieve information.
   */
  public static final String CONNECTION_PATH_ROOT = "/_user/contacts";

  /**
   * Builds a path to the connection node.
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
  public static String getConnectionPath(String user, String targetUser,
      String remainderPath) {
    // /_user/contacts.invite.html
    // /_user/contacts/aaron.accept.html

    if (remainderPath == null) {
      remainderPath = "";
    }
    if (remainderPath.startsWith(targetUser)) {
      remainderPath = remainderPath.substring(targetUser.length());
    }
    String path = getConnectionPathBase(user);
    return PathUtils.toInternalHashedPath(path, targetUser, "") + remainderPath;
  }

  public static String getConnectionPath(String user, String targetUser) {
    return getConnectionPath(user, targetUser, "");
  }

  /**
   * @param path
   * @param user1
   * @return
   */
  public static String getConnectionPathBase(String user1) {
    return PathUtils.toInternalHashedPath(CONNECTION_PATH_ROOT, user1, "");
  }

}
