package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.security.Principal;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.jackrabbit.core.security.authentication.AbstractLoginModule;
import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Authenticator;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.lite.storage.ConnectionPoolException;

public class SparseLoginModule extends AbstractLoginModule {

	private Repository repository;
	private Authenticator authenticator;
	private User user;

	@Override
	protected void doInit(CallbackHandler callbackHandler, Session session,
			@SuppressWarnings("rawtypes") Map options) throws LoginException {
		repository = SparseComponentHolder.getSparseRepositoryInstance();
		try {
			authenticator = repository.getAuthenticator();
		} catch (ConnectionPoolException e) {
			throw new LoginException(e.getMessage());
		}
	}

	@Override
	protected boolean impersonate(Principal principal, Credentials credentials)
			throws RepositoryException, LoginException {
		User user = authenticator.systemAuthenticate(principal.getName());
		if (user != null) {
			Subject impersSubject = getImpersonatorSubject(credentials);
			if (user.allowsImpersonactionBy(impersSubject)) {
				return true;
			} else {
				throw new FailedLoginException(
						"attempt to impersonate denied for "
								+ principal.getName());
			}
		}
		return false;
	}

	@Override
	protected Authentication getAuthentication(Principal principal,
			Credentials creds) throws RepositoryException {
		if (user != null) {
			Authentication authentication = new SparseCredentialsAuthentication(
					user, authenticator);
			if (authentication.canHandle(creds)) {
				return authentication;
			}
		}
		return null;
	}

	@Override
	protected Principal getPrincipal(Credentials credentials) {
		String userId = getUserID(credentials);
		User auser = authenticator.systemAuthenticate(userId);
		if (auser != null) {
			this.user = auser;
			return new SparsePrincipal(userId);
		}
		return null;
	}

}
