package org.sakaiproject.kernel2.osgi.guiceuser.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;

import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceE;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceF;

public class FProvider implements Provider<InterfaceF> {

  private InterfaceE e;

  @Inject
  public FProvider(InterfaceE e)
  {
    this.e = e;
  }
  
  public InterfaceF get() {
    return new InterfaceF() 
    {

      public void printViaE() {
        System.err.println("Provided F printing via e via d");
        FProvider.this.getE().printHelloViaD();
      }
    };
  }

  protected InterfaceE getE() {
    return e;
  }

}
