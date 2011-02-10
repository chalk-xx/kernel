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
package org.sakaiproject.nakamura.lite.jackrabbit;

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.principal.JackrabbitPrincipal;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;

import java.security.Principal;

import javax.jcr.RepositoryException;

public class SparsePrincipal implements ItemBasedPrincipal {

  private String principalId;
  private String path;
  private String location;
  public static final String USER_REPO_LOCATION = "/rep:security/rep:authorizables/rep:users";
  public static final String GROUP_REPO_LOCATION = "/rep:security/rep:authorizables/rep:groups";

  public SparsePrincipal(String principalId, String location, String basePath) {
    this.principalId = principalId;
    // TODO: check that this calculation of path is correct, I susepct its not.
    this.path = basePath + "/" + StorageClientUtils.shardPath(principalId);
    this.location = location;

  }

  public SparsePrincipal(Authorizable ath, String location) {
    this(ath.getId(), location, (ath instanceof Group) ? GROUP_REPO_LOCATION
                                                      : USER_REPO_LOCATION);
  }

  public String getName() {
    return principalId;
  }

  public String getPath() throws RepositoryException {
    return path;
  }

  @Override
  public String toString() {
    return "sparse:" + principalId + " from " + location;
  }

  // -------------------------------------------------------------< Object >---
  /**
   * Two principals are equal, if their names are.
   * 
   * @see Object#equals(Object)
   */
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof JackrabbitPrincipal) {
      return principalId.equals(((Principal) obj).getName());
    }
    return false;
  }

  /**
   * @return the hash code of the principals name.
   * @see Object#hashCode()
   */
  public int hashCode() {
    return principalId.hashCode();
  }

}
