package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.security.Principal;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.principal.JackrabbitPrincipal;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;

public class SparsePrincipal implements ItemBasedPrincipal {

	private String principalId;
	private String path;
	private String location;
	public static final String USER_REPO_LOCATION = "/rep:security/rep:authorizables/rep:users";
	public static final String GROUP_REPO_LOCATION = "/rep:security/rep:authorizables/rep:groups";
	
	public SparsePrincipal(String principalId, String location, String basePath) {
		this.principalId = principalId;
		this.path = basePath +"/"+StorageClientUtils.shardPath(principalId);
		this.location = location;

	}

	public String getName() {
		return principalId;
	}

	public String getPath() throws RepositoryException {
		return path;
	}
	
	@Override
	public String toString() {
		return "sparse:"+principalId+" from "+location;
	}
	
	   //-------------------------------------------------------------< Object >---
    /**
     * Two principals are equal, if their names are.
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof JackrabbitPrincipal) {
            return principalId.equals(((Principal) obj).getName());
        }
        return false;
    }

    /**
     * @return the hash code of the principals name.
     * @see Object#hashCode()
     */
    public int hashCode() {
        return principalId.hashCode();
    }

}
