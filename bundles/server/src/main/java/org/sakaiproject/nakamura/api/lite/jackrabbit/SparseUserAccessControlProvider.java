package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.security.Principal;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlPolicy;

import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractAccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.spi.Path;

public class SparseUserAccessControlProvider extends AbstractAccessControlProvider  {

	public AccessControlPolicy[] getEffectivePolicies(Path absPath)
			throws ItemNotFoundException, RepositoryException {
		return null;
	}

	public AccessControlEditor getEditor(Session session)
			throws RepositoryException {
		return null;
	}

	public CompiledPermissions compilePermissions(Set<Principal> principals)
			throws RepositoryException {
		return new CompiledPermissions() {
			
			public boolean grants(Path absPath, int permissions)
					throws RepositoryException {
				return true;
			}
			
			public int getPrivileges(Path absPath) throws RepositoryException {
				return 127;
			}
			
			public void close() {
			}
			
			public boolean canReadAll() throws RepositoryException {
				return true;
			}
		};
	}

	public boolean canAccessRoot(Set<Principal> principals)
			throws RepositoryException {
		return true;
	}

	public boolean isAcItem(Path absPath) throws RepositoryException {
		return false;
	}

	public boolean isAcItem(ItemImpl item) throws RepositoryException {
		return false;
	}


}
