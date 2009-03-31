package org.sakaiproject.kernel2.osgi.jpaprovider.model;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.sakaiproject.kernel2.osgi.jpaprovider.UserManagerFactory;

import javax.persistence.EntityManager;

public class Simple implements BundleActivator {

  public void start(BundleContext arg0) throws Exception {
    System.err.println("Doing some JPA");
    EntityManager manager = UserManagerFactory.getUserManager();
    System.err.println("EM: " + manager);
  }

  public void stop(BundleContext arg0) throws Exception {
    // TODO Auto-generated method stub
    
  }

}
