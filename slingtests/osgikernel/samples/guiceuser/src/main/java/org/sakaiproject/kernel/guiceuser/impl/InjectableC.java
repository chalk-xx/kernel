package org.sakaiproject.kernel.guiceuser.impl;

import com.google.inject.Inject;

import org.sakaiproject.kernel.guiceuser.api.InterfaceA;
import org.sakaiproject.kernel.guiceuser.api.InterfaceB;

public class InjectableC {

  @Inject
  public InjectableC(InterfaceA a, InterfaceB b)
  {
    b.printString(a.getHelloWorld());
  }
}
