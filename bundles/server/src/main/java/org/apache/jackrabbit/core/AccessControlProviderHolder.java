package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class AccessControlProviderHolder {

  /**
   * This provides the lookup on thread, ensurign that when the thread dissapears the
   * AccessProvider dissapears from the map.
   */
  private WeakHashMap<Thread, AccessControlProvider> threadMap = new WeakHashMap<Thread, AccessControlProvider>();

  /**
   * Keep hold of references to the access control providers so that we can close them.
   */
  private List<AccessControlProvider> accessControlProviders = new ArrayList<AccessControlProvider>();

  public void close() {
    synchronized (accessControlProviders) {
      for (AccessControlProvider acp : accessControlProviders) {
        acp.close();
      }
      accessControlProviders.clear();
      threadMap.clear();
    }
  }

  public AccessControlProvider get() {
    synchronized (accessControlProviders) {
      clean();
      return threadMap.get(Thread.currentThread());
    }
  }

  public void set(AccessControlProvider provider) {
    synchronized (accessControlProviders) {
      clean();
      threadMap.put(Thread.currentThread(), provider);
    }
  }

  /**
   * Iteditfy ACPs that were bound to threads that have gone, and clean them
   */
  private void clean() {
    List<AccessControlProvider> toDelete = new ArrayList<AccessControlProvider>();
    for (AccessControlProvider acp : accessControlProviders) {
      if (!threadMap.containsValue(acp)) {
        toDelete.add(acp);
      }
    }
    accessControlProviders.removeAll(toDelete);
  }

}
