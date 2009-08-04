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
package org.sakaiproject.kernel.ldap;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.ldap.api.LdapConnectionBroker;
import org.sakaiproject.kernel.ldap.api.LdapConnectionManager;
import org.sakaiproject.kernel.ldap.api.LdapConnectionManagerConfig;
import org.sakaiproject.kernel.ldap.api.LdapException;

import java.util.Hashtable;

/**
 * Simple implementation of an {@link LdapConnectionBroker}. Maintains an
 * associative pairing of connection factories and names. As connections are
 * requested by name, the associated factory is used to create a pooled
 * connection.
 */
@Component
@Service
public class PoolingLdapConnectionBroker implements LdapConnectionBroker {
  private Hashtable<String, LdapConnectionManager> factories;

  protected void activate(ComponentContext ctx) {
    factories = new Hashtable<String, LdapConnectionManager>();
  }

  protected void deactivate(ComponentContext ctx) {
    for (LdapConnectionManager conns : factories.values()) {
      conns.destroy();
    }
    factories = null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.ldap.api.LdapConnectionBroker#getConnection(java.lang.String)
   */
  public LDAPConnection getConnection(String name) throws LdapException {
    return getConnection(name, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.ldap.api.LdapConnectionBroker#getConnection(java.lang.String,
   *      org.sakaiproject.kernel.ldap.api.LdapConnectionManagerConfig)
   */
  public LDAPConnection getConnection(String name, LdapConnectionManagerConfig config)
      throws LdapException {
    try {
      // get a connection manager from the local store. if not found, create and
      // store a new one
      LdapConnectionManager mgr = null;
      synchronized (factories) {
        if (factories.contains(name)) {
          mgr = factories.get(name);
        } else {
          // create a new connection manager, set the config and initialize it.
          PoolingLdapConnectionManager _mgr = new PoolingLdapConnectionManager();
          _mgr.setConfig(config);
          _mgr.init();

          // put the new connection manager in the store and set it to be
          // available outside of this block.
          factories.put(name, _mgr);
          mgr = _mgr;
        }
      }

      // get a connection from the manager and return it
      LDAPConnection conn = mgr.getConnection();
      return conn;
    } catch (LDAPException e) {
      throw new LdapException(e.getMessage(), e);
    }
  }
}
