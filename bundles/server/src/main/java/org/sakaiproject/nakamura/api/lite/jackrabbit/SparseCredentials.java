package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Authenticator;

public class SparseCredentials extends CryptedSimpleCredentials {

	private Authenticator authenticator;

	public SparseCredentials(
			org.sakaiproject.nakamura.api.lite.authorizable.User sparseUser, Authenticator authenticator) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		super(sparseUser.getId(), "ignore");
		this.authenticator = authenticator;
	}

	@Override
	public boolean matches(SimpleCredentials credentials)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		if ( getUserID().equals(credentials.getUserID()) ) {
			return (authenticator.authenticate(credentials.getUserID(), new String(credentials.getPassword())) != null);
		}
		return false;
	}
	
}
