package org.sakaiproject.kernel2.osgi.jpaprovider;

import org.osgi.framework.Bundle;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class BundleGatheringResourceFinder {

  private List<Bundle> bundles;

  public BundleGatheringResourceFinder(List<Bundle> bundles) {
    this.bundles = bundles;
  }

  public List<URL> getResources(String string) {
    List<URL> result = new LinkedList<URL>();
    for (Bundle bundle : bundles) {
      URL resource = bundle.getResource(string);
      if (resource != null) {
        result.add(resource);
      }
    }
    return result;
  }

}
