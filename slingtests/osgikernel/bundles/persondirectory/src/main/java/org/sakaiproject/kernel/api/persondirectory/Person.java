/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.kernel.api.persondirectory;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A person with associated attributes as found by querying all registered
 * person attribute DAOs.
 *
 * @author Carl Hall
 */
public interface Person extends Principal {
  /**
   * Get all attributes associated to a person.
   *
   * @return A {@link Map} of attributes. The key is the name, the value is a
   *         list of values associated to that key.
   */
  Map<String, String[]> getAttributes();

  /**
   * Get the names of all attribues associated to a person.
   *
   * @return A {@link List} of names as {@link String}s.
   */
  Set<String> getAttributeNames();

  /**
   * Get the value of an attribute. If multiple values are available, the first
   * one found is returned.
   *
   * @param attributeName
   *          The attribute name for which to find a value.
   * @return The first value found for the attribute name. null if none found.
   */
  String getAttributeValue(String attributeName);

  /**
   *
   * @param attributeName
   *          The attribute name for which to find values.
   * @return The values found for the attribute name. null if none found.
   */
  String[] getAttributeValues(String attributeName);
}
