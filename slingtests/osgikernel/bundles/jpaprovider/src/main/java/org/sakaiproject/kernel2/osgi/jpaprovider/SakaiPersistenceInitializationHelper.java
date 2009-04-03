package org.sakaiproject.kernel2.osgi.jpaprovider;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.internal.jpa.deployment.PersistenceInitializationHelper;

import java.util.Map;

public class SakaiPersistenceInitializationHelper extends PersistenceInitializationHelper {

  private ClassLoader amalgamatedClassloader;

  public SakaiPersistenceInitializationHelper(ClassLoader amalgamatedClassloader)
  {
    this.amalgamatedClassloader = amalgamatedClassloader;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public ClassLoader getClassLoader(String emName, Map properties) {
    if (properties != null) {
        return (ClassLoader)properties.get(PersistenceUnitProperties.CLASSLOADER);
    }
    else
    {
      return amalgamatedClassloader;
    }
  }


}
