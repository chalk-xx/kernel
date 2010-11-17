package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.api.security.user.User;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Authenticator;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.lite.storage.StorageClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparseUser extends SparseAuthorizable implements User {

	private static final Logger LOGGER = LoggerFactory.getLogger(SparseUser.class);
	private Authenticator authenticator;

	public SparseUser(
			org.sakaiproject.nakamura.api.lite.authorizable.User user,
			AuthorizableManager authorizableManager, ValueFactory valueFactory, Authenticator authenticator) {
		super(user, authorizableManager, valueFactory);
		this.authenticator = authenticator;
	}

	public boolean isAdmin() {
		return getSparseUser().isAdmin();
	}

	private org.sakaiproject.nakamura.api.lite.authorizable.User getSparseUser() {
		return (org.sakaiproject.nakamura.api.lite.authorizable.User) sparseAuthorizable;
	}

	public Credentials getCredentials() throws RepositoryException {
		try {
			return new SparseCredentials(getSparseUser(), authenticator);
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error(e.getMessage(),e);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(),e);
		}
		return null;
	}

	public Impersonation getImpersonation() throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	public void changePassword(String password) throws RepositoryException {
		try {
			authorizableManager.changePassword(getSparseUser(),password);
		} catch (StorageClientException e) {
			throw new RepositoryException(e.getMessage(), e);
		} catch (AccessDeniedException e) {
			throw new javax.jcr.AccessDeniedException(e.getMessage(), e);
		}
	}

}
