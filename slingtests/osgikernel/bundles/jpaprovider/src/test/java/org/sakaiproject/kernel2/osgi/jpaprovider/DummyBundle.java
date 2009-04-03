package org.sakaiproject.kernel2.osgi.jpaprovider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

public class DummyBundle implements Bundle {

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
    return null;
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
    return getClass().getClassLoader().getResource(resourceName);
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
    return 0;
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
