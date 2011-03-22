package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import org.apache.sling.jcr.jackrabbit.server.impl.SlingServerRepository;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.lite.jackrabbit.SparseRepositoryHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SakaiSlingServerRepository extends SlingServerRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(SakaiSlingServerRepository.class);

  public void bindRepository(Repository repository) {
    SparseRepositoryHolder.setSparseRespository(repository);
  }
  
  public void unbindRepository(Repository repository) {
    SparseRepositoryHolder.setSparseRespository(null);
  }
  
}
