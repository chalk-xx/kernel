package org.sakaiproject.kernel2.osgi.jpaprovider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;

public class DummyBundleContext implements BundleContext {

  public void addBundleListener(BundleListener arg0) {

  }

  public void addFrameworkListener(FrameworkListener arg0) {
  }

  public void addServiceListener(ServiceListener arg0) {
  }

  public void addServiceListener(ServiceListener arg0, String arg1) throws InvalidSyntaxException {
  }

  public Filter createFilter(String arg0) throws InvalidSyntaxException {
    return null;
  }

  public ServiceReference[] getAllServiceReferences(String arg0, String arg1)
      throws InvalidSyntaxException {
    return null;
  }

  public Bundle getBundle() {
    return null;
  }

  public Bundle getBundle(long arg0) {
    return null;
  }

  public Bundle[] getBundles() {
    return new Bundle[] {};
  }

  public File getDataFile(String arg0) {
    return null;
  }

  public String getProperty(String arg0) {
    return null;
  }

  public Object getService(ServiceReference arg0) {
    return null;
  }

  public ServiceReference getServiceReference(String arg0) {
    return null;
  }

  public ServiceReference[] getServiceReferences(String arg0, String arg1)
      throws InvalidSyntaxException {
    return null;
  }

  public Bundle installBundle(String arg0) throws BundleException {
    return null;
  }

  public Bundle installBundle(String arg0, InputStream arg1) throws BundleException {
    return null;
  }

  @SuppressWarnings("unchecked")
  public ServiceRegistration registerService(String[] arg0, Object arg1, Dictionary arg2) {
    return null;
  }

  @SuppressWarnings("unchecked")
  public ServiceRegistration registerService(String arg0, Object arg1, Dictionary arg2) {
    return null;
  }

  public void removeBundleListener(BundleListener arg0) {
  }

  public void removeFrameworkListener(FrameworkListener arg0) {
  }

  public void removeServiceListener(ServiceListener arg0) {
  }

  public boolean ungetService(ServiceReference arg0) {
    return false;
  }

}
