package org.sakaiproject.kernel2.osgi.jpaprovider;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

public class SakaiPersistenceProvider extends PersistenceProvider {

  private static Logger LOG = LoggerFactory.getLogger(SakaiPersistenceProvider.class);
  
  @SuppressWarnings("unchecked")
  @Override
  protected EntityManagerFactory createEntityManagerFactory(String emName, Map properties,
      ClassLoader classLoader) {
    LOG.info("Creating entity manager factory");
    Map nonNullProperties = (properties == null) ? new HashMap() : properties;
    String name = emName;
    if (name == null){
        name = "";
    }
    try {
      nonNullProperties.put(PersistenceUnitProperties.CLASSLOADER, PersistenceBundleMonitor.getAmalgamatedClassloader(emName));
    } catch (IOException e) {
      LOG.error("Unable to create amalgamated classloader", e);
      return null;
    }

    return new PersistenceProvider().createEntityManagerFactory(emName, properties);
  }

}
