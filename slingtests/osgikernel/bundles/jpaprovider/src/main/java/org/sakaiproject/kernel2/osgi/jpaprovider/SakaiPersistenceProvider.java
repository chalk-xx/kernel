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
  
  private ClassLoader amalgamatedClassloader;
  
  public SakaiPersistenceProvider() {
    try {
      amalgamatedClassloader = PersistenceBundleMonitor.getAmalgamatedClassloader();
    } catch (IOException e) {
      LOG.error("Unable to create amalgamated classloader", e);
    }
    initializationHelper = new SakaiPersistenceInitializationHelper(amalgamatedClassloader);
  }
  
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
    nonNullProperties.put(PersistenceUnitProperties.CLASSLOADER, amalgamatedClassloader);
    LOG.info("Persistence Initialization Helper is: " + amalgamatedClassloader);

    ClassLoader saved = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(amalgamatedClassloader);
    EntityManagerFactory result = super.createEntityManagerFactory(emName, properties, amalgamatedClassloader);
    Thread.currentThread().setContextClassLoader(saved);
    return result;
  }

}
