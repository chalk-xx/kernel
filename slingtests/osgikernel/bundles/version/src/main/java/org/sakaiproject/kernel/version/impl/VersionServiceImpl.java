package org.sakaiproject.kernel.version.impl;

import org.apache.jackrabbit.JcrConstants;
import org.sakaiproject.kernel.version.VersionService;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;

/**
 * Service for doing operations with versions.
 * 
 * @scr.component immediate="true" label="Sakai Versioning Service"
 *                description="Service for doing operations with versions."
 *                name="org.sakaiproject.kernel.version.VersionService"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.sakaiproject.kernel.version.VersionService"
 */
public class VersionServiceImpl implements VersionService {

  public Version saveNode(Node node, String savingUsername) throws RepositoryException {
    if (node.hasProperty(JcrConstants.JCR_PRIMARYTYPE) && node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString().equals(JcrConstants.NT_FILE)) {
      node.addMixin("sakai:propertiesmix");
    }
    node.setProperty(SAVED_BY, savingUsername);
    node.save();
    Version version = null;
    try {
      version = node.checkin();
    } catch ( UnsupportedRepositoryOperationException e) {
      node.addMixin(JcrConstants.MIX_VERSIONABLE);
      node.save();
      version = node.checkin();
    }
    node.checkout();
    if ( node.getSession().hasPendingChanges() ) {
      node.getSession().save();
    }
    return version;
  }

}
