package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.apache.jackrabbit.util.Text;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Authenticator;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparseCredentialsAuthentication implements Authentication {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(SparseCredentialsAuthentication.class);
	private Authenticator authenticator;
	private User user;

	public SparseCredentialsAuthentication(User user,
			Authenticator authenticator) {
		this.user = user;
		this.authenticator = authenticator;
	}

	public boolean canHandle(Credentials credentials) {
		return (credentials instanceof SimpleCredentials);
	}

	public boolean authenticate(Credentials credentials)
			throws RepositoryException {
		if (credentials instanceof SimpleCredentials) {
			SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
			String testUserId = simpleCredentials.getUserID();
			if (testUserId != null && testUserId.equals(user.getId())) {
				String password = new String(simpleCredentials.getPassword());
				User user = authenticator.authenticate(
						simpleCredentials.getUserID(), digestPassword(password));
				if (user == null) {
					user = authenticator.authenticate(
							simpleCredentials.getUserID(), password);

				}
				LOGGER.debug("+++++++++++ Login to {} {}", simpleCredentials
						.getUserID(), user == null ? "FAILED Password was " : "OK " + user.getId());
				return user != null;
			}
		}
		LOGGER.info("--------- LOGIN FAILED Credentials: {} ", credentials);
		return false;
	}

	protected String digestPassword(String pwd)
			throws IllegalArgumentException {
		try {
			StringBuffer password = new StringBuffer();
			password.append("{sha1}");
			password.append(Text.digest("sha1", pwd.getBytes("UTF-8")));
			return password.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e.toString());
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.toString());
		}
	}

}
