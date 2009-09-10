package org.sakaiproject.kernel.version;

import org.apache.jackrabbit.api.security.user.Authorizable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

public interface VersionService {

  public static final String SAVED_BY = "sakai:savedBy";

  public Version saveNode(Node node, String savingUsername) throws RepositoryException;
}
