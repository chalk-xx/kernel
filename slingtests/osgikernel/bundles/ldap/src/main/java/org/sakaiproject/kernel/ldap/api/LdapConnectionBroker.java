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
package org.sakaiproject.kernel.ldap.api;

import com.novell.ldap.LDAPConnection;

/**
 * Central broker to handle connection managers.
 */
public interface LdapConnectionBroker {
  /**
   * Gets a connection from a named holder. If the holder does not already
   * exist, a new one is created using system level configuration defaults.
   *
   * @param name
   *          The name of the connection holder.
   * @return {@link LDAPConnection} from the named holder.
   * @throws LdapException
   */
  LDAPConnection getConnection(String name) throws LdapException;

  /**
   * Gets a connection from a named holder. If the holder does not already
   * exist, a new one is created using the provided configuration settings.
   *
   * @param name
   *          The name of the connection holder.
   * @return {@link LDAPConnection} from the named holder.
   * @param config
   * @return
   * @throws LdapException
   */
  LDAPConnection getConnection(String name, LdapConnectionManagerConfig config)
      throws LdapException;
}
