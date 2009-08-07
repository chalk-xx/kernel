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
package org.sakaiproject.kernel.ldap;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;

import org.apache.commons.pool.ObjectPool;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.ldap.api.LdapConnectionManagerConfig;

public class PoolingLdapConnectionManagerTest {

  private ObjectPool pool;
  private LdapConnectionManagerConfig config;
  private PoolingLdapConnectionManager poolingConnMgr;

  @Before
  public void setUp() {
    pool = createMock(ObjectPool.class);
    config = createMock(LdapConnectionManagerConfig.class);
    poolingConnMgr = new PoolingLdapConnectionManager();
    poolingConnMgr.setConfig(config);
    poolingConnMgr.setPool(pool);
    // some white box awkwardness
    expect(config.isSecureConnection()).andReturn(false);
    replay(pool, config);
    poolingConnMgr.init();
  }

  @Test
  public void testDoesNotReturnNullReferencesToPool() {
    // mockPool will throw a fit if any method called
    poolingConnMgr.returnConnection(null);
  }
}
