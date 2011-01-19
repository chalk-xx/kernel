package org.sakaiproject.nakamura.message;

import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.MessageConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MessageUtils {

  // TODO BL120 this method provides a temporary shim for establishing a user home if it doesn't exist
  // To be replaced by the port of user and group homes.
  public static void establishHomeFolder(String userId, String somePath, Repository repo)
      throws ClientPoolException, StorageClientException, AccessDeniedException {
    if (destinationIsUsersHome(userId, somePath)) {
      Session adminSession = null;
      try {
        adminSession = repo.loginAdministrative();
        ContentManager adminContentManager = adminSession.getContentManager();
        if (!adminContentManager.exists(somePath)) {
          adminContentManager.update(new Content(somePath, null));
        }

        AccessControlManager adminAccessControl = adminSession.getAccessControlManager();
        AuthorizableManager adminAuthManager = adminSession.getAuthorizableManager();
        Authorizable user = adminAuthManager.findAuthorizable(userId);
        if (user == null) {
          adminAuthManager.createUser(userId, userId, "testuser", new HashMap<String, Object>());
          user = adminAuthManager.findAuthorizable(userId);
        }
        if (!adminAccessControl.can(user, Security.ZONE_CONTENT, somePath,
            Permissions.CAN_READ.combine(Permissions.CAN_WRITE))) {
          List<AclModification> aclModifications = new ArrayList<AclModification>();
          AclModification.addAcl(Boolean.TRUE,
              Permissions.CAN_READ.combine(Permissions.CAN_WRITE), userId,
              aclModifications);
          AclModification[] arrayOfModifications = aclModifications
              .toArray(new AclModification[aclModifications.size()]);
          adminSession.getAccessControlManager().setAcl(Security.ZONE_CONTENT, somePath,
              arrayOfModifications);
        }

      } finally {
        if (adminSession != null) {
          try {
            adminSession.logout();
          } catch (ClientPoolException e) {
            throw new RuntimeException("Failed to logout session.", e);
          }
        }
      }

    }
  }

  private static boolean destinationIsUsersHome(String userId, String path) {
    return path != null && path.startsWith(MessageConstants.SAKAI_MESSAGE_PATH_PREFIX + userId);
  }

}
