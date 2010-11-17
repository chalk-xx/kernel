package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.jcr.Credentials;

import org.sakaiproject.nakamura.api.lite.authorizable.User;

public class SparseCredentials implements Credentials {


	/**
	 * 
	 */
	private static final long serialVersionUID = -4605339370727057403L;
	private User user;

	public SparseCredentials(
			User user) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.user = user;
	}
	

	public String getUserId() {
		return user.getId();
	}
	
}
