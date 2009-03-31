package org.sakaiproject.kernel2.osgi.jpaexample;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.sakaiproject.kernel2.osgi.jpaprovider.UserManagerFactory;

import javax.persistence.EntityManager;

public class JpaExample implements BundleActivator {

  public void start(BundleContext arg0) throws Exception {
    System.err.println("Doing some JPA");
    EntityManager manager = UserManagerFactory.getUserManager();
    System.err.println("EM: " + manager);
  }

  public void stop(BundleContext arg0) throws Exception {
  }

}
