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
package org.sakaiproject.kernel.persondirectory.providers;

import static junit.framework.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.api.ldap.LdapConstants;
import org.sakaiproject.kernel.api.persondirectory.Person;
import org.sakaiproject.kernel.ldap.PoolingLdapConnectionBroker;

import java.util.Properties;

public class LdapPersonProviderTest {
  private PoolingLdapConnectionBroker broker;
  private LdapPersonProvider provider;

  @Before
  public void setUp() {
    Properties props = new Properties();
    props.setProperty(LdapConstants.AUTO_BIND, Boolean.TRUE.toString());
    props.setProperty(LdapConstants.FOLLOW_REFERRALS, Boolean.TRUE.toString());
    props.setProperty(LdapConstants.KEYSTORE_LOCATION, "");
    props.setProperty(LdapConstants.KEYSTORE_PASSWORD, "");
    props.setProperty(LdapConstants.SECURE_CONNECTION, Boolean.TRUE.toString());
    props.setProperty(LdapConstants.HOST, "gted");
    props.setProperty(LdapConstants.PORT, "");
    props.setProperty(LdapConstants.USER, "gted2");
    props.setProperty(LdapConstants.PASSWORD, "cool");
//    props.setProperty(LdapConstants.OPERATION_TIMEOUT);
//    props.setProperty(LdapConstants.POOLING);
//    props.setProperty(LdapConstants.POOLING_MAX_CONNS);
//    props.setProperty(LdapConstants.TLS);
    broker = new PoolingLdapConnectionBroker();
    broker.update(props);
    provider = new LdapPersonProvider(broker);
  }

  @Test
  public void testGetPerson() throws Exception {
    Person person = provider.getPerson("chall39", null);
    assertNotNull(person);
  }
}
