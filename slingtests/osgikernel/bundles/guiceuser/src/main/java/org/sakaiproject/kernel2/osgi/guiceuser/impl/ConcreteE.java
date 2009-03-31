package org.sakaiproject.kernel2.osgi.guiceuser.impl;

import com.google.inject.Inject;

import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceA;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceB;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceD;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceE;

public class ConcreteE implements InterfaceE {
  
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
    System.err.println("Printing from e");
    b.printString(a.getHelloWorld());
  }


}
