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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.ldap.api.LdapConnectionLivenessValidator;

import java.util.ArrayList;
import java.util.List;

public class ConsensusLdapConnectionLivenessValidatorTest {

	private ConsensusLdapConnectionLivenessValidator validator;
	private LdapConnectionLivenessValidator delegateValidator1;
	private LdapConnectionLivenessValidator delegateValidator2;
	private LdapConnectionLivenessValidator delegateValidator3;

  @Before
  public void setUp() {
		validator = new ConsensusLdapConnectionLivenessValidator();
    delegateValidator1 = createMock(LdapConnectionLivenessValidator.class);
    delegateValidator2 = createMock(LdapConnectionLivenessValidator.class);
    delegateValidator3 = createMock(LdapConnectionLivenessValidator.class);
	}

  @Test
	public void testValidatesConnectionIfNoDelegatesRegistered() {
		PooledLDAPConnection conn = new PooledLDAPConnection();
    assertTrue(validator.isConnectionAlive(conn));
	}

	public void testValidatesConnectionIfAllDelegatesValidateConnection() {
		PooledLDAPConnection conn = new PooledLDAPConnection();
    expect(delegateValidator1.isConnectionAlive(conn)).andReturn(true);
    expect(delegateValidator2.isConnectionAlive(conn)).andReturn(true);
    expect(delegateValidator3.isConnectionAlive(conn)).andReturn(true);
    replay(delegateValidator1, delegateValidator2, delegateValidator3);
		List<LdapConnectionLivenessValidator> delegates =
			new ArrayList<LdapConnectionLivenessValidator>(3);
		delegates.add(delegateValidator1);
		delegates.add(delegateValidator2);
		delegates.add(delegateValidator3);
		validator.setDelegates(delegates);
		assertTrue(validator.isConnectionAlive(conn));
	}

  @Test
	public void testInvalidatesConnectionIfOneDelegateInvalidatesConnection() {
		PooledLDAPConnection conn = new PooledLDAPConnection();
    expect(delegateValidator1.isConnectionAlive(conn)).andReturn(true);
    expect(delegateValidator2.isConnectionAlive(conn)).andReturn(false);
    replay(delegateValidator1, delegateValidator2, delegateValidator3);
		List<LdapConnectionLivenessValidator> delegates =
			new ArrayList<LdapConnectionLivenessValidator>(3);
		delegates.add(delegateValidator1);
		delegates.add(delegateValidator2);
		delegates.add(delegateValidator3); // expects iteration to short-circuit
		validator.setDelegates(delegates);
		assertFalse(validator.isConnectionAlive(conn));
	}

  @Test
	public void testTreatsNullDelegateListInjectionAsEmptyList() {
		validator.setDelegates(null);
		testValidatesConnectionIfNoDelegatesRegistered();
	}

}
