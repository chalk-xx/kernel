package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.util.Iterables;
import org.sakaiproject.nakamura.lite.storage.StorageClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparseAuthorizable implements Authorizable {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(SparseAuthorizable.class);
	protected org.sakaiproject.nakamura.api.lite.authorizable.Authorizable sparseAuthorizable;
	AuthorizableManager authorizableManager;
	private Principal principal;
	ValueFactory valueFactory;

	public SparseAuthorizable(
			org.sakaiproject.nakamura.api.lite.authorizable.Authorizable authorizable,
			AuthorizableManager authorizableManager, ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
		this.sparseAuthorizable = authorizable;
		this.authorizableManager = authorizableManager;
		this.principal = new SparsePrincipal(authorizable.getId());	
	}

	public String getID() throws RepositoryException {
		return sparseAuthorizable.getId();
	}

	public boolean isGroup() {
		return false;
	}

	public Principal getPrincipal() throws RepositoryException {
		return principal;
	}

	public Iterator<Group> declaredMemberOf() throws RepositoryException {
		final Iterator<String> memberIterator = Iterables.of(
				sparseAuthorizable.getPrincipals()).iterator();
		return new Iterator<Group>() {

			private SparseGroup group;

			public boolean hasNext() {
				while (memberIterator.hasNext()) {
					try {
						String id = memberIterator.next();
						org.sakaiproject.nakamura.api.lite.authorizable.Authorizable a = authorizableManager
								.findAuthorizable(id);
						if (a instanceof org.sakaiproject.nakamura.api.lite.authorizable.Group) {
							group = new SparseGroup(
									(org.sakaiproject.nakamura.api.lite.authorizable.Group) a, authorizableManager, valueFactory);
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

			public Group next() {
				return group;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public Iterator<Group> memberOf() throws RepositoryException {
		final List<String> memberIds = new ArrayList<String>();
		Collections.addAll(memberIds, sparseAuthorizable.getPrincipals());
		return new Iterator<Group>() {

			private int p;
			private SparseGroup group;

			public boolean hasNext() {
				while (p < memberIds.size()) {
					String id = memberIds.get(p);
					p++;
					try {
						org.sakaiproject.nakamura.api.lite.authorizable.Authorizable a = authorizableManager
								.findAuthorizable(id);
						if (a instanceof org.sakaiproject.nakamura.api.lite.authorizable.Group) {
							group = new SparseGroup(
									(org.sakaiproject.nakamura.api.lite.authorizable.Group) a, authorizableManager, valueFactory);
							for (String pid : a.getPrincipals()) {
								if (!memberIds.contains(pid)) {
									memberIds.add(pid);
								}
							}
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

			public Group next() {
				return group;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public void remove() throws RepositoryException {
		try {
			authorizableManager.delete(sparseAuthorizable.getId());
		} catch (AccessDeniedException e) {
			throw new javax.jcr.AccessDeniedException(e.getMessage(), e);
		} catch (StorageClientException e) {
			throw new RepositoryException(e.getMessage(), e);
		}
	}

	public Iterator<String> getPropertyNames() throws RepositoryException {
		return sparseAuthorizable.getSafeProperties().keySet().iterator();
	}

	public boolean hasProperty(String name) throws RepositoryException {
		return sparseAuthorizable.hasProperty(name);
	}

	public void setProperty(String name, Value value)
			throws RepositoryException {
		sparseAuthorizable.setProperty(name, value.getString());
		save();
	}

	public void setProperty(String name, Value[] value)
			throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		for (Value v : value) {
			sb.append(StorageClientUtils.arrayEscape(v.getString()))
					.append(",");
		}
		sparseAuthorizable.setProperty(name, sb.toString());
		save();
	}

	public Value[] getProperty(String name) throws RepositoryException {
		String s = StorageClientUtils.toString(sparseAuthorizable
				.getProperty(name));
		if (s != null) {
			String[] parts = StringUtils.split(s, ',');
			Value[] v = new Value[parts.length];
			for (int i = 0; i < parts.length; i++) {
				v[i] = valueFactory.createValue(parts[i]);
			}
			return v;
		}
		return null;
	}

	public boolean removeProperty(String name) throws RepositoryException {
		sparseAuthorizable.removeProperty(name);
		save();
		return true;
	}

	protected void save() {
		// dumb impl at the moment, would be better if there was a hook on the
		// JCR session save.
		try {
			authorizableManager.updateAuthorizable(sparseAuthorizable);
			sparseAuthorizable = authorizableManager
					.findAuthorizable(sparseAuthorizable.getId());
		} catch (Exception e) {
			LOGGER.error("Failed to save authorizable", e);
		}
	}

}
