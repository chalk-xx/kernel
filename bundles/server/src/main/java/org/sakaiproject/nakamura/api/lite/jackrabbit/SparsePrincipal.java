package org.sakaiproject.nakamura.api.lite.jackrabbit;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;

public class SparsePrincipal implements ItemBasedPrincipal {

	private String principalId;
	private String path;

	public SparsePrincipal(String principalId) {
		this.principalId = principalId;
		this.path = StorageClientUtils.shardPath(principalId);

	}

	public String getName() {
		return principalId;
	}

	public String getPath() throws RepositoryException {
		return path;
	}

}
