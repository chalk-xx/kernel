package org.sakaiproject.nakamura.api.lite.jackrabbit;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.lite.jackrabbit.SparseMapUserManager;

import javax.jcr.RepositoryException;

public class JackrabbitSparseUtils {
  
  public static Session getSparseSession(javax.jcr.Session session) throws RepositoryException {
    UserManager  userManager = AccessControlUtil.getUserManager(session);
    if ( userManager instanceof SparseMapUserManager ) {
      SparseMapUserManager sparseMapUserManager = (SparseMapUserManager) userManager;
      return sparseMapUserManager.getSession();
    }
    return null;
  }

}
