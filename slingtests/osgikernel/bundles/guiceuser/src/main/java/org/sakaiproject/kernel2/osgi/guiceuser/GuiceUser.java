package org.sakaiproject.kernel2.osgi.guiceuser;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceD;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceE;
import org.sakaiproject.kernel2.osgi.guiceuser.impl.InjectableC;

public class GuiceUser implements BundleActivator {

  public void start(BundleContext arg0) throws Exception {
    System.out.println("Fetching injector");
    Injector injector = Guice.createInjector(new TestModule());
    System.err.println("Fetching Injectable instance");
    injector.getInstance(InjectableC.class);
    System.err.println("Print test 1");
    InterfaceD d = injector.getInstance(InterfaceD.class);
    d.printHello();
    InterfaceE e = injector.getInstance(InterfaceE.class);
    System.err.println("Print test 2");
    e.printHelloViaD();
  }

  public void stop(BundleContext arg0) throws Exception {
    System.out.println("Stopping");
  }

}
