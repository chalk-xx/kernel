package org.sakaiproject.nakamura.user.lite.resource;

import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;

import java.io.IOException;

public class RepositoryHelper {

  public static Repository getRepository(String[] users, String groups[]) throws ClientPoolException, StorageClientException, AccessDeniedException, ClassNotFoundException, IOException {
    BaseMemoryRepository baseMemoryRepository = new BaseMemoryRepository();
    Session session = baseMemoryRepository.getRepository().loginAdministrative();
    AuthorizableManager authorizableManager = session.getAuthorizableManager();
    for ( String user : users ) {
      authorizableManager.createUser(user, user, "test", null);
    }
    for ( String group : groups ) {
      authorizableManager.createGroup(group,group, null);
    }
    session.logout();
    return baseMemoryRepository.getRepository();
  }
  
  

}
