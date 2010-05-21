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
package org.sakaiproject.nakamura.api.user;

/**
 * 
 */
public interface UserConstants {
  public static final String SYSTEM_USER_MANAGER_PATH = "/system/userManager";

  public static final String SYSTEM_USER_MANAGER_USER_PATH = SYSTEM_USER_MANAGER_PATH
      + "/user";
  public static final String SYSTEM_USER_MANAGER_GROUP_PATH = SYSTEM_USER_MANAGER_PATH
      + "/group";

  public static final String SYSTEM_USER_MANAGER_USER_PREFIX = SYSTEM_USER_MANAGER_USER_PATH
      + "/";
  public static final String SYSTEM_USER_MANAGER_GROUP_PREFIX = SYSTEM_USER_MANAGER_GROUP_PATH
      + "/";

  public static final String USER_PROFILE_RESOURCE_TYPE = "sakai/user-profile";
  public static final String GROUP_PROFILE_RESOURCE_TYPE = "sakai/group-profile";

  /**
   * A list of private properties that will not be copied from the authorizable.
   */
  public static final String PRIVATE_PROPERTIES = "sakai:privateproperties";

  /**
   * The number of hash levels applied to user paths, this is system wide and can't be
   * changed once an instance has been loaded with users. 4 will give upto 2E9 users.
   */
  public static final int DEFAULT_HASH_LEVELS = 4;
  
  
  /**
   * A node property that indicates which use the node was created by, for ownership.
   */
  public static final String JCR_CREATED_BY = "jcr:createdBy";
  
  
  /**
   * The ID of an anon user.
   */
  public static final String ANON_USERID = "anonymous";
  
  public static final String ADMIN_USERID = "admin";
  
  public static final String PROP_GROUP_MANAGERS = "rep:group-managers";

  public static final String PROP_GROUP_VIEWERS = "rep:group-viewers";

  public static final String[] DEFAULT_USERS = { ANON_USERID, ADMIN_USERID};

  public static final String USER_REPO_LOCATION = "/rep:security/rep:authorizables/rep:users";
  public static final String GROUP_REPO_LOCATION = "/rep:security/rep:authorizables/rep:groups";


  
}
