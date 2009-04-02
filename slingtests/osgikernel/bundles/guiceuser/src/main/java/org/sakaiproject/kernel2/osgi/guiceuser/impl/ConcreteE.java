package org.sakaiproject.kernel2.osgi.guiceuser.impl;

import com.google.inject.Inject;

import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceA;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceB;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceD;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcreteE implements InterfaceE {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(ConcreteE.class);
  private InterfaceD d;
  
  @Inject
  public ConcreteE(InterfaceD d)
  {
    this.d = d;
  }
  
  public void printHelloViaD()
  {
    this.d.printHello();
  }
  
  public void doPrint(InterfaceA a, InterfaceB b)
  {
    LOGGER.info("Printing from e");
    b.printString(a.getHelloWorld());
  }


}
