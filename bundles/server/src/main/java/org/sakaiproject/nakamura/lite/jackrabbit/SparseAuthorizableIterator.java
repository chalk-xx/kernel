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

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.util.PreemptiveIterator;

import java.util.Iterator;

import javax.jcr.ValueFactory;

public class SparseAuthorizableIterator extends PreemptiveIterator<Authorizable> {

  private Authorizable authorizable;
  private Iterator<org.sakaiproject.nakamura.api.lite.authorizable.Authorizable> authorizableIterator;
  private AuthorizableManager authorizableManager;
  private AccessControlManager accessControlManager;
  private ValueFactory valueFactory;

  public SparseAuthorizableIterator(
      Iterator<org.sakaiproject.nakamura.api.lite.authorizable.Authorizable> authorizableIterator,
      AuthorizableManager authorizableManager, AccessControlManager accessControlManager,
      ValueFactory valueFactory) {
    this.authorizableIterator = authorizableIterator;
    this.authorizableManager = authorizableManager;
    this.accessControlManager = accessControlManager;
    this.valueFactory = valueFactory;
  }

  protected boolean internalHasNext() {
    while (authorizableIterator.hasNext()) {
      org.sakaiproject.nakamura.api.lite.authorizable.Authorizable a = authorizableIterator
          .next();
      if (a instanceof Group) {
        authorizable = new SparseGroup((Group) a, authorizableManager,
            accessControlManager, valueFactory);
        return true;
      } else if (a instanceof User) {
        authorizable = new SparseUser((User) a, authorizableManager,
            accessControlManager, valueFactory);
        return true;
      }
    }
    return false;
  }

  protected Authorizable internalNext() {
    return authorizable;
  }

}
