package org.sakaiproject.kernel2.osgi.jpaprovider;

import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PersistenceSettings;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

public class DummyJpaBundle extends DummyBundle {

  private String puName;
  private Map<String, String> files;
  private int state;

  public DummyJpaBundle(String puName, Map<String, String> files, int state) throws IOException {
    this.state = state;
    this.files = files;
    PersistenceSettings settings = PersistenceSettings.parse(getClass().getClassLoader()
        .getResourceAsStream(files.get(AmalgamatingClassloader.PERSISTENCE_XML)));
    this.puName = settings.getPersistenceUnits().get(0).getName();
  }

  @SuppressWarnings("unchecked")
  public Dictionary getHeaders() {
    Hashtable<String, String> result = new Hashtable<String, String>();
    result.put(PersistenceBundleMonitor.SAKAI_JPA_PERSISTENCE_UNITS_BUNDLE_HEADER, puName);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Dictionary getHeaders(String arg0) {
    return getHeaders();
  }

  public URL getResource(String resourceName) {
    if (files.containsKey(resourceName)) {
      return getClass().getClassLoader().getResource(files.get(resourceName));
    }
    return null;
  }

  public int getState() {
    return state;
  }

}
