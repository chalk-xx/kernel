package org.sakaiproject.kernel2.osgi.jpaprovider;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class UserManagerFactory {

  public static EntityManager getUserManager()
  {
    EntityManagerFactory factory = Persistence.createEntityManagerFactory("user");
    return factory.createEntityManager();
  }
}
