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
package org.sakaiproject.nakamura.user;

import javax.jcr.RepositoryException;

public class NameSanitizer {

  private String name;
  private boolean isUser;

  public NameSanitizer(String name, boolean isUser) {
    this.name = name;
    this.isUser = isUser;
  }

  public void validate() throws RepositoryException {
    String name = this.name;

    // User names can't start with g-
    if (isUser() && name.startsWith("g-")) {
      throw new RepositoryException("User name must not begin 'g-'");
    }

    // Group names HAVE to start with a g-
    if (isGroup() && !name.startsWith("g-")) {
      throw new RepositoryException("Group names must begin with 'g-'");
    }

    if (isGroup() && name.startsWith("g-")) {
      name = name.substring(2);
    }

    // At least 3 chars.
    if (name.length() < 3) {
      throw new RepositoryException("Name must be bigger than 3 chars.");
    }

    // KERN-271
    // Only allowing 0-9a-Z-_
    for (int i = 0; i < name.length(); i++) {
      if (!Character.isLetterOrDigit(name.charAt(i)) && '_' != name.charAt(i)
          && '-' != name.charAt(i)) {
        throw new RepositoryException("Invalid characters in name: " + name);
      }
    }
    
    // KERN-763 - UserIDs starting with g-contacts- are reserved for the contact groups.
    if (name.startsWith("g-contacts-")) {
      throw new RepositoryException("'g-contacts-' is a reserved prefix.");
    }
  }

  /**
   * @return true if the sanitizer should handle the name as a group id.
   */
  public boolean isGroup() {
    return !isUser;
  }

  /**
   * @return true if the sanitizer should handle the name as a username.
   */
  public boolean isUser() {
    return isUser;
  }
}
