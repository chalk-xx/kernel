package org.sakaiproject.kernel2.osgi.jpaprovider;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class BundleGatheringResourceFinder {

  private static final Logger LOG = LoggerFactory.getLogger(BundleGatheringResourceFinder.class);
  private Collection<Bundle> bundles;

  public BundleGatheringResourceFinder(Collection<Bundle> bundles) {
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
  
  public Class<?> loadClass(String name) {
    for (Bundle bundle : bundles) {
      try
      {
        return bundle.loadClass(name);
      }
      catch (Exception e)
      {
        /* Try another bundle */
      }
    }
    LOG.warn("Unable to find class '" + name + "' in bundles");
    return null;
  }

}
