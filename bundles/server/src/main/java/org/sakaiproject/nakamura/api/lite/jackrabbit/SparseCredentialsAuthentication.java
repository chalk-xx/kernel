package org.sakaiproject.nakamura.api.lite.jackrabbit;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Authenticator;
import org.sakaiproject.nakamura.api.lite.authorizable.User;

public class SparseCredentialsAuthentication implements Authentication {

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
		if ( credentials instanceof SimpleCredentials ) {
			SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
			String testUserId = simpleCredentials.getUserID();
			if ( testUserId != null && testUserId.equals(user.getId())) {
				User user = authenticator.authenticate(simpleCredentials.getUserID(), new String(simpleCredentials.getPassword()));
				return user != null;
			}
		}
		return false;
	}

}
