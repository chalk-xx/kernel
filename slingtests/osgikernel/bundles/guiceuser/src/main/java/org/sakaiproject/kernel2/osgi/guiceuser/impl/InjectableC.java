package org.sakaiproject.kernel2.osgi.guiceuser.impl;

import com.google.inject.Inject;

import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceA;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceB;

public class InjectableC {

  @Inject
  public InjectableC(InterfaceA a, InterfaceB b)
  {
    b.printString(a.getHelloWorld());
  }
}
