package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.util.Iterator;

import javax.jcr.ValueFactory;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;

public class SparseAuthorizableIterator implements Iterator<Authorizable> {

	private Authorizable authorizable;
	private Iterator<org.sakaiproject.nakamura.api.lite.authorizable.Authorizable> authorizableIterator;
	private AuthorizableManager authorizableManager;
	private AccessControlManager accessControlManager;
	private ValueFactory valueFactory;

	public SparseAuthorizableIterator(
			Iterator<org.sakaiproject.nakamura.api.lite.authorizable.Authorizable> authorizableIterator,
			AuthorizableManager authorizableManager,
			AccessControlManager accessControlManager, ValueFactory valueFactory) {
		this.authorizableIterator = authorizableIterator;
		this.authorizableManager = authorizableManager;
		this.accessControlManager = accessControlManager;
		this.valueFactory = valueFactory;
	}

	public boolean hasNext() {
		while (authorizableIterator.hasNext()) {
			org.sakaiproject.nakamura.api.lite.authorizable.Authorizable a = authorizableIterator
					.next();
			if (a instanceof Group) {
				authorizable = new SparseGroup((Group) a, authorizableManager,
						accessControlManager, valueFactory);
				return true;
			} else if (a instanceof User) {
				authorizable = new SparseUser((User) a, authorizableManager,
						accessControlManager, valueFactory);
				return true;
			}
		}
		return false;
	}

	public Authorizable next() {
		return authorizable;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
