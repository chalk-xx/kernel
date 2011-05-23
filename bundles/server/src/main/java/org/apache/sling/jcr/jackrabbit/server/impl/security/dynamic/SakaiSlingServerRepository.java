package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import org.apache.sling.jcr.jackrabbit.server.impl.SlingServerRepository;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.lite.jackrabbit.SparseRepositoryHolder;


public class SakaiSlingServerRepository extends SlingServerRepository {

  public void bindRepository(Repository repository) {
    SparseRepositoryHolder.setSparseRespository(repository);
  }
  
  public void unbindRepository(Repository repository) {
    SparseRepositoryHolder.setSparseRespository(null);
  }
  
}
