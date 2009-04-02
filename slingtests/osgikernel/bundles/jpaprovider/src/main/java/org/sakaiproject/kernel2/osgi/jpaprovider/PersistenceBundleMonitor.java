package org.sakaiproject.kernel2.osgi.jpaprovider;

import com.thoughtworks.xstream.XStream;

import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.EntityConverter;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.OrmSettings;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PersistenceSettings;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PersistenceUnit;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PropertyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersistenceBundleMonitor implements BundleActivator, SynchronousBundleListener {
  
  private static final Logger LOG = LoggerFactory.getLogger(PersistenceBundleMonitor.class);
  
  // these maps are used to retrieve the classloader used for different bundles
  private static Map<String, List<Bundle>> puToBundle = Collections.synchronizedMap(new HashMap<String,List<Bundle>>());
  private static Map<Bundle, String[]> bundleToPUs = Collections.synchronizedMap(new HashMap<Bundle, String[]>());

  private static ClassLoader contextClassLoader;
  
  /**
   * Add a bundle to the list of bundles managed by this persistence provider
   * The bundle is indexed so it's classloader can be accessed
   * @param bundle
   * @param persistenceUnitNames
   */
  public static void addBundle(Bundle bundle, String[] persistenceUnitNames) {
      for (int i = 0; i < persistenceUnitNames.length; i++) {
          String name = persistenceUnitNames[i];
          List<Bundle> list = puToBundle.get(name);
          if (list == null)
          {
            list = new ArrayList<Bundle>();
            puToBundle.put(name, list); 
          }
          list.add(bundle);
      }
      bundleToPUs.put(bundle, persistenceUnitNames);
  }

  /**
   * Removed a bundle from the list of bundles managed by this persistence provider
   * This typically happens on deactivation.
   * @param bundle
   */
  public static void removeBundle(Bundle bundle) {
      String[] persistenceUnitNames = bundleToPUs.remove(bundle);
      if (persistenceUnitNames != null) {
          for (int i = 0; i < persistenceUnitNames.length; i++) {
              String name = persistenceUnitNames[i];
              List<Bundle> bundles = puToBundle.get(name);
              if (bundles != null)
              {
                bundles.remove(bundle);
                if (bundles.size() == 0)
                {
                  puToBundle.remove(name);
                }
              }
          }
      }
  }
  
  /**
   * Simply add bundles to our bundle list as they start and remove them as they
   * stop
   */
  public void bundleChanged(BundleEvent event) {
    switch (event.getType()) {
    case BundleEvent.STARTING:
      registerBundle(event.getBundle());
      break;

    case BundleEvent.STOPPING:
      deregisterBundle(event.getBundle());
      break;
    }
  }

  /**
   * On start, we do two things We register a listener for bundles and we start
   * our JPA server
   */
  public void start(BundleContext context) throws Exception {
    LOG.info("Starting to monitor for persistence bundles");
    PersistenceBundleMonitor.contextClassLoader = Thread.currentThread().getContextClassLoader();
    context.addBundleListener(this);
    Bundle bundles[] = context.getBundles();
    for (int i = 0; i < bundles.length; i++) {
      Bundle bundle = bundles[i];
      registerBundle(bundle);
    }
  }

  /**
   * Store a reference to a bundle as it is started so the bundle can be
   * accessed later
   * 
   * @param bundle
   */
  private void registerBundle(Bundle bundle) {
    if ((bundle.getState() & (Bundle.STARTING | Bundle.ACTIVE)) != 0) {
      try {
        String[] persistenceUnitNames = getPersistenceUnitNames(bundle);
        if (persistenceUnitNames != null) {
          PersistenceBundleMonitor.addBundle(bundle, persistenceUnitNames);
        }
      } catch (Exception e) {
        AbstractSessionLog.getLog().logThrowable(SessionLog.WARNING, e);
      }
    }
  }

  private String[] getPersistenceUnitNames(Bundle bundle) {
    String names = (String) bundle.getHeaders().get("JPA-PersistenceUnits");
    if (names != null) {
      return names.split(",");
    } else {
      return null;
    }
  }

  private void deregisterBundle(Bundle bundle) {
    PersistenceBundleMonitor.removeBundle(bundle);
  }

  public void stop(BundleContext context) throws Exception {
    context.removeBundleListener(this);
  }

  public static BundleGatheringResourceFinder getBundleResourceFinder(String emName) {
    List<Bundle> bundles = puToBundle.get(emName);
    if (bundles == null || bundles.size() == 0)
    {
      LOG.warn("No bundles found to match " + emName);
      return null;
    }
    return new BundleGatheringResourceFinder(bundles);
  }

  public static ClassLoader getAmalgamatedClassloader(String emName) throws IOException {
    BundleGatheringResourceFinder currentLoader = PersistenceBundleMonitor.getBundleResourceFinder(emName);
    if (currentLoader == null)
    {
      LOG.warn("No persistence xmls found for " + emName);
      return null;
    }

    LOG.debug("Looking for persistence.xmls");
    List<URL> persistences = currentLoader.getResources("META-INF/persistence.xml");
    List<URL> orms = currentLoader.getResources("META-INF/orm.xml");
    AmalgamatingClassloader loader = new AmalgamatingClassloader(contextClassLoader);
    for (URL persistence : persistences)
    {
       loader.importPersistenceXml(persistence);
    }
    for (URL orm : orms)
    {
      loader.importOrmXml(orm);
    }
    return loader;
  }
  
  public static XStream getPersistenceSettingsXStream()
  {
    XStream xstream = new XStream();
    xstream.processAnnotations(PersistenceSettings.getPersistenceClasses());
    xstream.useAttributeFor(PersistenceUnit.class, "name");
    xstream.registerConverter(new PropertyConverter());
    return xstream;
  }
  
  public static PersistenceSettings parsePersistenceXml(InputStream is)
  {
    return (PersistenceSettings) getPersistenceSettingsXStream().fromXML(is);
  }

  public static XStream getOrmSettingsXStream()
  {
    XStream xstream = new XStream();
    xstream.processAnnotations(OrmSettings.getOrmClasses());
    xstream.aliasSystemAttribute("type", "class");
    xstream.registerConverter(new EntityConverter());
    return xstream;
  }
  
  public static OrmSettings parseOrmXml(InputStream is)
  {
    return (OrmSettings) getOrmSettingsXStream().fromXML(is);
  }

}
