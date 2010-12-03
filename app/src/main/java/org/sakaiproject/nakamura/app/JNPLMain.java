package org.sakaiproject.nakamura.app;

import org.apache.sling.launchpad.app.Main;

public class JNPLMain {
  public static void main(String[] args) {
    System.setSecurityManager(null);
    Main.main(args);
  }
}
