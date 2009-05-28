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
package org.sakaiproject.kernel.site;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;

import java.util.Map;

/**
 * 
 */
public class MembershipTree {

  private Map<Group, Membership> groups;
  private Map<User, Membership> users;

  /**
   * @param groups
   * @param users
   */
  public MembershipTree(Map<Group, Membership> groups, Map<User, Membership> users) {
    this.groups = groups;
    this.users = users;
  }
  /**
   * @return the groups
   */
  public Map<Group, Membership> getGroups() {
    return groups;
  }
  /**
   * @return the users
   */
  public Map<User, Membership> getUsers() {
    return users;
  }

}
