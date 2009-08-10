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

import com.novell.ldap.LDAPConnection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.sakaiproject.kernel.ldap.api.LdapConnectionLivenessValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A "federating" {@link LdapConnectionLivenessValidator} implemenation
 * which requires all delegates to validate a given connection. Considers
 * a connection invalid if any one member returns <code>false</code>
 * from {@link LdapConnectionLivenessValidator#isConnectionAlive(com.novell.ldap.LDAPConnection)}.
 *
 * <p>Intended for chaining together multiple liveness strategies. E.g.,
 * to assist with logging, an institution may choose to require both
 * {@link MaxLifetimeLdapConnectionLivenessValidator} and
 * {@link SearchExecutingLdapConnectionLivenessValidator} to validate
 * connection liveness.</p>
 *
 *
 * @author dmccallum
 */
@Component
public class ConsensusLdapConnectionLivenessValidator implements LdapConnectionLivenessValidator {

	/** Class-specific logger */
  private static Logger log = LoggerFactory
      .getLogger(ConsensusLdapConnectionLivenessValidator.class);

  @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
	private List<LdapConnectionLivenessValidator> delegates =
		new ArrayList<LdapConnectionLivenessValidator>(0);

	/**
	 * Tests the given {@link LDAPConnection} with each registered
	 * {@link LdapConnectionLivenessValidator} until one such delegate
	 * returns <code>false></code>, in which case this method also
	 * returns <code>false</code>. Otherwise returns <code>true</code>,
	 * even if the registered delegate list is empty.
	 */
	public boolean isConnectionAlive(LDAPConnection connectionToTest) {
		if ( delegates.isEmpty() ) {
			if ( log.isDebugEnabled() ) {
				log.debug("isConnectionAlive(): no delegates to consult, returning true");
			}
			return true;
		}
		for ( LdapConnectionLivenessValidator delegate : delegates ) {
			if ( log.isDebugEnabled() ) {
				log.debug("isConnectionAlive(): delegating to instance of [" + delegate.getClass().getName() + "]");
			}
			if ( delegate.isConnectionAlive(connectionToTest) ) {
				if ( log.isDebugEnabled() ) {
					log.debug("isConnectionAlive(): delegate indicated live connection [delegate = " + delegate.getClass().getName() + "], testing with next delegate, if any");
				}
				continue;
			}
			if ( log.isDebugEnabled() ) {
				log.debug("isConnectionAlive(): delegate indicated stale connection [delegate = " + delegate.getClass().getName() + "]");
			}
			return false;
		}
		if ( log.isDebugEnabled() ) {
			log.debug("isConnectionAlive(): reached consensus on connection liveness, returning true");
		}
		return true;
	}

	/**
	 * Assign the list of {@link LdapConnectionLivenessValidator}s to
	 * consult when validating any given connection. Will not
	 * return <code>null</code>.
	 *
	 * @return a direct reference to the internal list
	 */
	public List<LdapConnectionLivenessValidator> getDelegates() {
		return delegates;
	}

	/**
	 * Assign the list of {@link LdapConnectionLivenessValidator}s to
	 * consult when validating any given connection. If <code>null</code>,
	 * will result in this object caching an empty list.
	 *
	 * @param delegates
	 */
	public void setDelegates(List<LdapConnectionLivenessValidator> delegates) {
		if ( delegates == null ) {
			this.delegates = new ArrayList<LdapConnectionLivenessValidator>(0);
		} else {
			this.delegates = delegates;
		}
	}

}
