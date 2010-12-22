package org.sakaiproject.nakamura.lite.jackrabbit;

import org.apache.sling.jcr.jackrabbit.server.impl.SlingServerRepository;

// @Component, manually configured in serviceComponents.xml
public class SparseSlingServerRepository extends SlingServerRepository {

  
  /**
   * We bind to this to ensure that its activated first.
   */
  @SuppressWarnings("unused")
  private JackrabbitRepositoryStartupService service;

  public SparseSlingServerRepository() {
  }

  protected void bindJackrabbitRepositoryStartupService(JackrabbitRepositoryStartupService service) {
    this.service = service;
  }
  
  protected void unbindJackrabbitRepositoryStartupService(JackrabbitRepositoryStartupService service) {
    this.service = null;
  }
  
}
