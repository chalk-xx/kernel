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
package org.sakaiproject.kernel.api.personal;

import static org.sakaiproject.kernel.api.personal.PersonalConstants._GROUP_PRIVATE;
import static org.sakaiproject.kernel.api.personal.PersonalConstants._GROUP_PUBLIC;
import static org.sakaiproject.kernel.api.personal.PersonalConstants._USER_PRIVATE;
import static org.sakaiproject.kernel.api.personal.PersonalConstants._USER_PUBLIC;

import org.sakaiproject.kernel.util.PathUtils;

/**
 * 
 */
public class PersonalUtils {

  public static String getProfilePath(String user)  {
    return getPublicPath(user, PersonalConstants.AUTH_PROFILE);
  }

  public static String getPublicPath(String user, String path) {
    String userS = String.valueOf(user);
    if (userS.startsWith("g-")) {
      return PathUtils.toInternalHashedPath(_GROUP_PUBLIC, userS, path);
    } else {
      return PathUtils.toInternalHashedPath(_USER_PUBLIC, userS, path);
    }
  }

  public static String getPrivatePath(String user, String path) {
    String userS = String.valueOf(user);
    if (userS.startsWith("g-")) {
      return PathUtils.toInternalHashedPath(_GROUP_PRIVATE, userS, path);
    } else {
      return PathUtils.toInternalHashedPath(_USER_PRIVATE, userS, path);
    }
  }
}
