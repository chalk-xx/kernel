/**
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
package org.sakaiproject.util;

import java.security.SecureRandom;
import java.security.SignatureException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.nakamura.util.Signature;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;

/**
 * Utility class for dealing with x-sakai-token semantics.
 */
public class XSakaiToken {
	private final static Log LOG = LogFactory.getLog(XSakaiToken.class);
	public static final String X_SAKAI_TOKEN_HEADER = "x-sakai-token";
	public static final String CONFIG_PREFIX = "x.sakai.token";
	public static final String CONFIG_SHARED_SECRET_SUFFIX = "sharedSecret";
	public static final String TOKEN_SEPARATOR = ";";

	private static final SecureRandom SRANDOM = new SecureRandom();

	/**
	 * Simply grab the x-sakai-token from the request.
	 * 
	 * @param request
	 * @return
	 */
	public static String getToken(final HttpServletRequest request) {
		return request.getHeader(X_SAKAI_TOKEN_HEADER);
	}

	/**
	 * Validate the token using the passed sharedSecret and return username.
	 * 
	 * @param request
	 * @param sharedSecret
	 * @return
	 */
	public static String getValidatedEid(final HttpServletRequest request,
			final String sharedSecret) {
		return getValidatedEid(getToken(request), sharedSecret);
	}

	/**
	 * Validate the token using the passed sharedSecret and return username.
	 * 
	 * @param token
	 * @param sharedSecret
	 * @return
	 */
	public static String getValidatedEid(final String token,
			final String sharedSecret) {
		String userId = null;
		final String[] parts = token.split(TOKEN_SEPARATOR);
		if (parts.length == 3) {
			try {
				final String hash = parts[0];
				final String user = parts[1];
				final String nonce = parts[2];
				final String message = user + TOKEN_SEPARATOR + nonce;
				final String hmac = Signature.calculateRFC2104HMAC(message,
						sharedSecret);
				if (hmac.equals(hash)) {
					// the user is Ok, we will trust it.
					userId = user;
				}
			} catch (SignatureException e) {
				LOG.error("Failed to validate server token: " + token, e);
			}
		} else {
			LOG.error("Illegal number of elements in trusted server token: "
					+ token);
		}
		return userId;
	}

	/**
	 * This is the preferred signature for the createToken methods as it looks
	 * up the current userId from the current session. Therefore it is a little
	 * safer.
	 * 
	 * @param hostname
	 *            Fully qualified domain name or an IP address. See:
	 *            {@link #getSharedSecret(String)}.
	 * @return
	 */
	public static String createToken(final String hostname) {
		final SessionManager sm = (SessionManager) ComponentManager
				.get(SessionManager.class);
		final Session session = sm.getCurrentSession();
		if (session != null) {
			return createToken(hostname, session.getUserEid());
		} else {
			return createToken(hostname, "anonymous");
		}
	}

	/**
	 * Perform sharedSecret lookup from {@link #getSharedSecret(String)},
	 * compute hash based on eid and return token. If possible, you should use
	 * the {@link #createToken(String)} method signature as it is less error
	 * prone.
	 * 
	 * @param hostname
	 *            Fully qualified domain name or an IP address. See:
	 *            {@link #getSharedSecret(String)}.
	 * @param eid
	 *            Enterprise user id; usually a username.
	 * @return
	 * @throws Error
	 *             Wrapped exception if there is any unexpected trouble.
	 */
	public static String createToken(final String hostname, final String eid)
			throws Error {
		final String sharedSecret = getSharedSecret(hostname);
		final String token = signMessage(sharedSecret, eid);
		return token;
	}

	/**
	 * Compute hash based on sharedSecret and eid.
	 * 
	 * @param sharedSecret
	 * @param eid
	 *            Enterprise user id; usually a username.
	 * @return Fully computed token.
	 * @throws Error
	 *             Wrapped exception if there is any unexpected trouble.
	 */
	public static String signMessage(final String sharedSecret, final String eid) {
		String token = null;
		final String message = eid + TOKEN_SEPARATOR + SRANDOM.nextLong();
		try {
			final String hash = Signature.calculateRFC2104HMAC(message,
					sharedSecret);
			token = hash + TOKEN_SEPARATOR + message;
		} catch (SignatureException e) {
			throw new Error(e);
		}
		return token;
	}

	/**
	 * Look up a sharedSecret from sakai.properties. For example:<br/>
	 * <code>x.sakai.token.server.domain.name.sharedSecret=yourSecret</code> or <br/>
	 * <code>x.sakai.token.127.0.0.1.sharedSecret=yourSecret</code>
	 * 
	 * @param hostname
	 *            Fully qualified domain name or an IP address.
	 * @return null if not found.
	 */
	public static String getSharedSecret(final String hostname) {
		final ServerConfigurationService config = (ServerConfigurationService) ComponentManager
				.get(ServerConfigurationService.class);
		final String key = CONFIG_PREFIX + "." + hostname + "."
				+ CONFIG_SHARED_SECRET_SUFFIX;
		final String sharedSecret = config.getString(key, null);
		return sharedSecret;
	}

}
