package org.sakaiproject.nakamura.api.lite.jackrabbit;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.util.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

public class SparseGroup extends SparseAuthorizable implements Group {

	protected static final Logger LOGGER = LoggerFactory
			.getLogger(SparseGroup.class);

	public SparseGroup(
			org.sakaiproject.nakamura.api.lite.authorizable.Group group,
			AuthorizableManager authorizableManager,
			AccessControlManager accessControlManager, ValueFactory valueFactory) {
		super(group, authorizableManager, accessControlManager, valueFactory);
		this.principal = new SparsePrincipal(group.getId(), this.getClass().getName(), SparseMapUserManager.USERS_PATH);
	}

	public Iterator<Authorizable> getDeclaredMembers()
			throws RepositoryException {
		final Iterator<String> memberIterator = Iterables.of(
				getSparseGroup().getMembers()).iterator();
		return new Iterator<Authorizable>() {

			private SparseAuthorizable authorizble;

			public boolean hasNext() {
				while (memberIterator.hasNext()) {
					try {
						String id = memberIterator.next();
						org.sakaiproject.nakamura.api.lite.authorizable.Authorizable a = authorizableManager
								.findAuthorizable(id);
						if (a instanceof org.sakaiproject.nakamura.api.lite.authorizable.Group) {
							authorizble = new SparseGroup(
									(org.sakaiproject.nakamura.api.lite.authorizable.Group) a,
									authorizableManager, accessControlManager,
									valueFactory);
							return true;
						} else if (a instanceof org.sakaiproject.nakamura.api.lite.authorizable.User) {
							authorizble = new SparseUser(
									(org.sakaiproject.nakamura.api.lite.authorizable.User) a,
									authorizableManager, accessControlManager,
									valueFactory);
							return true;
						}
					} catch (AccessDeniedException e) {
						LOGGER.debug(e.getMessage(), e);
					} catch (StorageClientException e) {
						LOGGER.debug(e.getMessage(), e);
					}

				}
				return false;
			}

			public Authorizable next() {
				return authorizble;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public Iterator<Authorizable> getMembers() throws RepositoryException {
		final List<String> memberIds = new ArrayList<String>();
		Collections.addAll(memberIds, getSparseGroup().getMembers());
		return new Iterator<Authorizable>() {

			private int p;
			private SparseAuthorizable authorizable;

			public boolean hasNext() {
				while (p < memberIds.size()) {
					String id = memberIds.get(p);
					p++;
					try {
						org.sakaiproject.nakamura.api.lite.authorizable.Authorizable a = authorizableManager
								.findAuthorizable(id);
						if (a instanceof org.sakaiproject.nakamura.api.lite.authorizable.Group) {
							authorizable = new SparseGroup(
									(org.sakaiproject.nakamura.api.lite.authorizable.Group) a,
									authorizableManager, accessControlManager,
									valueFactory);
							for (String pid : a.getPrincipals()) {
								if (!memberIds.contains(pid)) {
									memberIds.add(pid);
								}
							}
							return true;
						} else if (a instanceof org.sakaiproject.nakamura.api.lite.authorizable.User) {
							authorizable = new SparseUser(
									(org.sakaiproject.nakamura.api.lite.authorizable.User) a,
									authorizableManager, accessControlManager,
									valueFactory);
							return true;
						}
					} catch (AccessDeniedException e) {
						LOGGER.debug(e.getMessage(), e);
					} catch (StorageClientException e) {
						LOGGER.debug(e.getMessage(), e);
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
		};
	}

	public boolean isMember(Authorizable authorizable)
			throws RepositoryException {
		String id = authorizable.getID();
		for (String s : getSparseGroup().getMembers()) {
			if (id.equals(s)) {
				return true;
			}
		}
		final List<String> memberIds = new ArrayList<String>();
		Collections.addAll(memberIds, getSparseGroup().getMembers());
		int p = 0;
		while (p < memberIds.size()) {
			String s = memberIds.get(p);
			p++;
			if (id.equals(s)) {
				return true;
			}

			try {
				org.sakaiproject.nakamura.api.lite.authorizable.Authorizable a = authorizableManager
						.findAuthorizable(s);
				if (a instanceof org.sakaiproject.nakamura.api.lite.authorizable.Group) {
					for (String mid : ((org.sakaiproject.nakamura.api.lite.authorizable.Group) a)
							.getMembers()) {
						if (!memberIds.contains(mid)) {
							memberIds.add(mid);
						}
					}
				}
			} catch (AccessDeniedException e) {
				LOGGER.debug(e.getMessage(), e);
			} catch (StorageClientException e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
		return false;
	}

	public boolean addMember(Authorizable authorizable)
			throws RepositoryException {
		String id = authorizable.getID();
		for (String member : getSparseGroup().getMembers()) {
			if (id.equals(member)) {
				return false;
			}
		}
		getSparseGroup().addMember(id);
		save();
		return false;
	}

	public boolean removeMember(Authorizable authorizable)
			throws RepositoryException {
		String id = authorizable.getID();
		for (String member : getSparseGroup().getMembers()) {
			if (id.equals(member)) {
				getSparseGroup().removeMember(id);
				save();
				return true;
			}
		}
		return false;
	}

	private org.sakaiproject.nakamura.api.lite.authorizable.Group getSparseGroup() {
		return (org.sakaiproject.nakamura.api.lite.authorizable.Group) sparseAuthorizable;
	}
	
	@Override
	public boolean isGroup() {
		return true;
	}

}
