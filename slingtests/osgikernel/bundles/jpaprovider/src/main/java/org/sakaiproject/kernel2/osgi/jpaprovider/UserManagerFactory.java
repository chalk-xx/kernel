package org.sakaiproject.kernel2.osgi.jpaprovider;

import org.eclipse.persistence.jpa.osgi.PersistenceProvider;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class UserManagerFactory {

  public static EntityManager getUserManager()
  {
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("eclipselink.ddl-generation", "drop-and-create-tables");
    properties.put("eclipselink.ddl-generation.output-mode", "database");
    properties.put("eclipselink.classloader", EntityManager.class.getClassLoader());
    EntityManagerFactory factory = new PersistenceProvider().createEntityManagerFactory("user", properties);
    return factory.createEntityManager();
  }
}
