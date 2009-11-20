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

import java.util.Set;

/**
 * Provider interface for looking up people ({@link Person}) and attributes
 * associated to them.
 *
 * @author Carl Hall
 */
public interface PersonProvider {
  /**
   * Get all attributes associated to a person.
   *
   * @param uid
   *          The user ID to lookup.
   * @return A {@link Person} with all found associated attributes. null if the
   *         UID is not found.
   */
  Person getPerson(String uid) throws PersonProviderException;

  /**
   * Get specific attributes associated to a person.
   * 
   * @param uid
   *          The user ID to lookup.
   * @param attributes
   *          The names of attributes to retrieve.
   * @return A {@link Person} with all found associated attributes limited by
   *         the list specified. null if the UID is not found.
   */
  Person getPerson(String uid, String... attributes) throws PersonProviderException;

  /**
   * Get all attributes associated to a .
   *
   * @param uid
   *          The user ID to lookup.
   * @return A {@link Person} with all found associated attributes. null if none
   *         of the UIDs were found.
   */
  Set<Person> getPeople(Set<String> uids) throws PersonProviderException;

  /**
   * Get specific attributes associated to a person.
   *
   * @param uid
   *          The user ID to lookup.
   * @param attributes
   *          The names of attributes to retrieve.
   * @return A {@link Person} with all found associated attributes limited by
   *         the list specified. null if none of the UIDs were found.
   */
  Set<Person> getPeople(Set<String> uids, String... attributes) throws PersonProviderException;
}
