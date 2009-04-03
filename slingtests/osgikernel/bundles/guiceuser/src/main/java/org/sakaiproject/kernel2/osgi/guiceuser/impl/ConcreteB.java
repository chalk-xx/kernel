package org.sakaiproject.kernel2.osgi.guiceuser.impl;

import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcreteB implements InterfaceB {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConcreteB.class);

  public void printString(String string) {
    Exception e = new Exception("Traceback, not an exception");
    LOGGER.info(string,e);
  }

}
