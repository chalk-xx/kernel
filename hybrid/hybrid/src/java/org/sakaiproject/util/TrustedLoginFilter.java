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

import java.io.IOException;
import java.security.SignatureException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.nakamura.util.Signature;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * <pre>
 *  A filter to come after the standard sakai request filter to allow services
 *  to encode a token containing the user id accessing the service. 
 *  
 *  The filter must be configured with a shared secret and requests contain a 
 *  header "x-sakai-token". This token is used to validate the Request and
 *  associate a user with the request.
 *  
 *  The token contains:
 *  hash;user
 *  
 *  hash is a Base64 encoded HMAC hash, user is the username to associate with the request.
 *  
 *  The shared secret must be known by both ends of the conversation, and must not be distributed outside a trusted zone.
 *  
 *  To use this filter add it AFTER the Sakai Request Filter in you web.xml like
 *  
 *  
 *  	&lt;!-- 
 * 	The Sakai Request Hander 
 * 	--&gt;
 * 	&lt;filter&gt;
 * 		&lt;filter-name&gt;sakai.request&lt;/filter-name&gt;
 * 		&lt;filter-class&gt;org.sakaiproject.util.RequestFilter&lt;/filter-class&gt;
 * 	&lt;/filter&gt;
 * 	&lt;filter&gt;
 * 		&lt;filter-name&gt;sakai.trusted&lt;/filter-name&gt;
 * 		&lt;filter-class&gt;org.sakaiproject.util.TrustedLoginFilter&lt;/filter-class&gt;
 *       &lt;init-param&gt;
 *       	&lt;param-name&gt;shared.secret&lt;/param-name&gt;
 *           &lt;param-value&gt;The Snow on the Volga falls only under the bridges&lt;/param-value&gt;
 *       &lt;/init-param&gt;
 * 	&lt;/filter&gt;
 * 	
 * 	&lt;!--
 * 	Mapped onto Handler
 * 	--&gt;
 * 	&lt;filter-mapping&gt;
 * 		&lt;filter-name&gt;sakai.request&lt;/filter-name&gt;
 * 		&lt;servlet-name&gt;sakai.mytoolservlet&lt;/servlet-name&gt;
 * 		&lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 * 		&lt;dispatcher&gt;FORWARD&lt;/dispatcher&gt;
 * 		&lt;dispatcher&gt;INCLUDE&lt;/dispatcher&gt;
 * 	&lt;/filter-mapping&gt; 
 * 
 * 	&lt;filter-mapping&gt;
 * 		&lt;filter-name&gt;sakai.trusted&lt;/filter-name&gt;
 * 		&lt;servlet-name&gt;sakai.mytoolservlet&lt;/servlet-name&gt;
 * 		&lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 * 		&lt;dispatcher&gt;FORWARD&lt;/dispatcher&gt;
 * 		&lt;dispatcher&gt;INCLUDE&lt;/dispatcher&gt;
 * 	&lt;/filter-mapping&gt;
 * 
 * </pre>
 * 
 */
public class TrustedLoginFilter implements Filter {
	private final static Log LOG = LogFactory.getLog(TrustedLoginFilter.class);
	private static final String ORG_SAKAIPROJECT_UTIL_TRUSTED_LOGIN_FILTER_SHARED_SECRET = "org.sakaiproject.util.TrustedLoginFilter.sharedSecret";

	private SessionManager sessionManager;
	private String sharedSecret;

	/**
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest hreq = (HttpServletRequest) req;
		String token = hreq.getHeader("x-sakai-token");
		Session currentSession = null;
		Session requestSession = null;
		if (token != null) {
			String user = decodeToken(token);
			if (user != null) {
				currentSession = sessionManager.getCurrentSession();
				if (!user.equals(currentSession.getUserEid())) {
					requestSession = sessionManager.startSession();
					org.sakaiproject.user.api.User usr;
					try {
						usr = org.sakaiproject.user.cover.UserDirectoryService
								.getUserByEid(user);
						requestSession.setUserEid(usr.getEid());
						requestSession.setUserId(usr.getId());
						requestSession.setActive();
					} catch (UserNotDefinedException e) {
						LOG.error(e.getLocalizedMessage(), e);
						throw new Error(e);
					}
					sessionManager.setCurrentSession(requestSession);
					// wrap the request so that we can get the user via
					// getRemoteUser() in other places.
					if (!(hreq instanceof ToolRequestWrapper)) {
						hreq = new ToolRequestWrapper(hreq, user);
					}
				}
			}
		}
		try {
			chain.doFilter(hreq, resp);
		} finally {
			if (requestSession != null) {
				if (currentSession != null) {
					sessionManager.setCurrentSession(currentSession);
				}
				requestSession.invalidate();
			}
		}
	}

	/**
	 * @param token
	 * @return
	 */
	protected String decodeToken(String token) {
		String userId = null;
		String[] parts = token.split(";");
		if (parts.length == 2) {
			try {
				String hash = parts[0];
				String user = parts[1];
				String hmac = Signature
						.calculateRFC2104HMAC(user, sharedSecret);
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
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	public void init(FilterConfig config) throws ServletException {
		sessionManager = org.sakaiproject.tool.cover.SessionManager
				.getInstance();
		// get sharedSecret from web.xml first
		sharedSecret = config.getInitParameter("sharedSecret");
		if (sharedSecret == null) {
			// not in web.xml may be specified in sakai.properties
			sharedSecret = ServerConfigurationService
					.getString(ORG_SAKAIPROJECT_UTIL_TRUSTED_LOGIN_FILTER_SHARED_SECRET);
		}
	}

	/**
	 * @see javax.servlet.Filter#destroy()
	 */
	public void destroy() {
		// nothing to do here
	}

	/**
	 * @param sharedSecret
	 *            the sharedSecret to set
	 */
	protected void setSharedSecret(String sharedSecret) {
		this.sharedSecret = sharedSecret;
	}

}