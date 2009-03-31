package org.sakaiproject.kernel2.osgi.guiceuser.impl;

import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceB;

public class ConcreteB implements InterfaceB {

  public void printString(String string) {
    new Exception().printStackTrace();
    System.out.println(string);
  }

}
