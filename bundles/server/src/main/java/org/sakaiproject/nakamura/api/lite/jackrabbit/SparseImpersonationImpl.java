package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.security.auth.Subject;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.core.security.principal.PrincipalIteratorAdapter;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.util.Iterables;


public class SparseImpersonationImpl implements Impersonation {

	private SparseUser sparseUser;

	public SparseImpersonationImpl(
			SparseUser sparseUser) {
		this.sparseUser = sparseUser;
	}

	@SuppressWarnings("unchecked")
	public PrincipalIterator getImpersonators() throws RepositoryException {
		User u = sparseUser.getSparseUser();
		String impersonators = StorageClientUtils.toString(u.getProperty(User.PRINCIPALS_FIELD));
		if ( impersonators == null ) {
			return new PrincipalIteratorAdapter(Collections.EMPTY_LIST);
		}
		final Iterator<String> imperson = Iterables.of(StringUtils.split(impersonators,';')).iterator();
		return new PrincipalIteratorAdapter(new Iterator<Principal>() {

			private Principal principal;

			public boolean hasNext() {
				if ( imperson.hasNext()) {
					String userId = imperson.next();
					if ( User.ADMIN_USER.equals(userId)) {
						principal = new AdminPrincipal(userId);
					} else if ( User.SYSTEM_USER.equals(userId)) {
						principal = new SystemPrincipal();
					} else {
						principal = new SparsePrincipal(imperson.next());
					}
					return true;
				}
				principal = null;
				return false;
			}

			public Principal next() {
				return principal;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		});
	}

	public boolean grantImpersonation(Principal principal)
			throws RepositoryException {
		if ( principal instanceof AdminPrincipal || principal instanceof SystemPrincipal ) {
			return false;
		}
		User u = sparseUser.getSparseUser();
		String impersonators = StorageClientUtils.toString(u.getProperty(User.PRINCIPALS_FIELD));
		Set<String> imp = new HashSet<String>();
		Collections.addAll(imp, StringUtils.split(impersonators,';'));
		String name = principal.getName();
		if ( !imp.contains(name) ) {
			imp.add(name);
			u.setProperty(User.PRINCIPALS_FIELD, StorageClientUtils.toStore(StringUtils.join(imp,';')));
			sparseUser.save();
			return true;
		}
		return false;
	}

	public boolean revokeImpersonation(Principal principal)
			throws RepositoryException {
		if ( principal instanceof AdminPrincipal || principal instanceof SystemPrincipal ) {
			return false;
		}
		User u = sparseUser.getSparseUser();
		String impersonators = StorageClientUtils.toString(u.getProperty(User.PRINCIPALS_FIELD));
		Set<String> imp = new HashSet<String>();
		Collections.addAll(imp, StringUtils.split(impersonators,';'));
		String name = principal.getName();
		if ( imp.contains(name) ) {
			imp.remove(name);
			u.setProperty(User.PRINCIPALS_FIELD, StorageClientUtils.toStore(StringUtils.join(imp,';')));
			sparseUser.save();
			return true;
		}
		return false;
	}

	public boolean allows(Subject subject) throws RepositoryException {
		if ( !subject.getPrincipals(AdminPrincipal.class).isEmpty() || !subject.getPrincipals(SystemPrincipal.class).isEmpty() ) {
			return true;
		}
		User u = sparseUser.getSparseUser();
		String impersonators = StorageClientUtils.toString(u.getProperty(User.PRINCIPALS_FIELD));
		Set<String> imp = new HashSet<String>();
		Collections.addAll(imp, StringUtils.split(impersonators,';'));
		for ( Principal p : subject.getPrincipals() ) {
			if ( imp.contains(p.getName()) ) {
				return true;
			}
		}
		return false;
	}

}
