package org.sakaiproject.nakamura.files.pool;

import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.profile.LiteProfileServiceImpl;

public class TLiteProfileServiceImpl extends LiteProfileServiceImpl {

  public void setSparseRepository(Repository sparseRepository) {
    this.sparseRepository = sparseRepository;    
  }

}
