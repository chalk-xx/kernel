package org.sakaiproject.kernel2.osgi.jpaprovider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PersistenceSettings;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

public class DummyJpaBundle implements Bundle {

  private String puName;
  private Map<String,String> files;
  private int state;

  public DummyJpaBundle(String puName, Map<String, String> files, int state) throws IOException
  {
    this.state = state;
    this.files = files;
    PersistenceSettings settings = PersistenceBundleMonitor.parsePersistenceXml(getClass().getClassLoader().getResourceAsStream(files.get(AmalgamatingClassloader.PERSISTENCE_XML)));
    this.puName = settings.getPersistenceUnits().get(0).getName();
  }

  @SuppressWarnings("unchecked")
  public Enumeration findEntries(String arg0, String arg1, boolean arg2) {
    return null;
  }

  public BundleContext getBundleContext() {
    return null;
  }

  public long getBundleId() {
    return 0;
  }

  public URL getEntry(String arg0) {
    return null;
  }

  @SuppressWarnings("unchecked")
  public Enumeration getEntryPaths(String arg0) {
    return null;
  }

  @SuppressWarnings("unchecked")
  public Dictionary getHeaders() {
    Hashtable<String,String> result = new Hashtable<String,String>();
    result.put("JPA-PersistenceUnits", puName);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Dictionary getHeaders(String arg0) {
    return getHeaders();
  }

  public long getLastModified() {
    return 0;
  }

  public String getLocation() {
    return null;
  }

  public ServiceReference[] getRegisteredServices() {
    return null;
  }

  public URL getResource(String resourceName) {
    if (files.containsKey(resourceName))
    {      
      return getClass().getClassLoader().getResource(files.get(resourceName));
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public Enumeration getResources(String arg0) throws IOException {
    URL url = getResource(arg0);
    if (url != null)
    {
      return new UrlEnumeration(url);
    }
    return null;
  }

  public ServiceReference[] getServicesInUse() {
    return null;
  }

  public int getState() {
    return state;
  }

  public String getSymbolicName() {
    return null;
  }

  public boolean hasPermission(Object arg0) {
    return false;
  }

  @SuppressWarnings("unchecked")
  public Class loadClass(String arg0) throws ClassNotFoundException {
    return null;
  }

  public void start() throws BundleException {
  }

  public void start(int arg0) throws BundleException {
  }

  public void stop() throws BundleException {
  }

  public void stop(int arg0) throws BundleException {
  }

  public void uninstall() throws BundleException {
  }

  public void update() throws BundleException {
  }

  public void update(InputStream arg0) throws BundleException {
  }
}
