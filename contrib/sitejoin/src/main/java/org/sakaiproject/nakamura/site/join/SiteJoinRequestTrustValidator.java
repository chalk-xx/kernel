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
package org.sakaiproject.nakamura.site.join;

import java.security.SignatureException;
import java.util.Date;
import java.util.Dictionary;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidator;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.util.Signature;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

@Service
@Component
@Properties(value = {
		@Property(name = "service.vendor", value = "The Sakai Foundation"),
		@Property(name = "service.description", value = "Validates a request to join a Sakai site."),
		@Property(name = RequestTrustValidator.VALIDATOR_NAME, value = "site-join-request"),
		@Property(name = "site.join.secret", value = "secretsecret") })
public class SiteJoinRequestTrustValidator implements RequestTrustValidator {
	
	@Reference
	private SiteService siteService;

	private static final String DEFAULT_SITE_JOIN_SECRET = "secretsecret";
	private String siteJoinSecret = DEFAULT_SITE_JOIN_SECRET;
	
	private static final int ONE_DAY = 24 * 60 * 60 * 1000;
	private static final int DEFAULT_TOKEN_TTL = ONE_DAY;
	private Integer tokenTTL = null;

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidator#getLevel()
	 */
	public int getLevel() {
		return RequestTrustValidator.CREATE_USER;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidator#isTrusted(javax.servlet.http.HttpServletRequest)
	 */
	public boolean isTrusted(HttpServletRequest request) {
		try {
			return currentUserIsSiteMaintainer(request)
					|| (currentUserIsAddingSelf(request)
					&& requestHasValidAuthToken(request));
		} catch (RepositoryException e) {
			return false;
		}
	}

	private boolean requestHasValidAuthToken(HttpServletRequest request) {
		String userName = request.getParameter("user");
		String siteId = request.getParameter("siteId");
		String timestamp = request.getParameter("timestamp");
		String token = request.getParameter("authToken");
		String tokenData = userName + ";" + siteId + ";" + timestamp;
		try {
			return tokenIsNotExpired(timestamp) && Signature.calculateRFC2104HMACWithEncoding(tokenData, siteJoinSecret, true)
					.equals(token);
		} catch (SignatureException e) {
			return false;
		}
	}

	private boolean tokenIsNotExpired(String timestampString) {
		Date now = new Date();
		int timestamp = Integer.parseInt(timestampString);
		if (tokenTTL != null) {
			return now.before(new Date(timestamp + tokenTTL));
		} else {
			return now.before(new Date(timestamp + DEFAULT_TOKEN_TTL));
		}
	}

	private boolean currentUserIsAddingSelf(HttpServletRequest request) {
		return request.getRemoteUser().equals(request.getParameter("user"));
	}

	private boolean currentUserIsSiteMaintainer(HttpServletRequest request) throws RepositoryException {
		Node requestedSiteNode = (Node) request.getAttribute("requestedNode");
		return siteService.isUserSiteMaintainer(requestedSiteNode);
	}

	protected void activate(ComponentContext componentContext) {
		Dictionary<?, ?> props = componentContext.getProperties();
		Object propValue = props.get("site.join.secret");
		if (propValue instanceof String) {
			siteJoinSecret = (String) propValue;
		} else {
			siteJoinSecret = DEFAULT_SITE_JOIN_SECRET;
		}
	}

}
