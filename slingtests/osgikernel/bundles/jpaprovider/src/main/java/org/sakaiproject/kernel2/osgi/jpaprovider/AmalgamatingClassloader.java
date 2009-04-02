package org.sakaiproject.kernel2.osgi.jpaprovider;

import com.thoughtworks.xstream.XStream;

import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.OrmEntity;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.OrmSettings;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PersistenceSettings;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PersistenceUnit;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.XStreamWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AmalgamatingClassloader extends ClassLoader {

  private static final Logger LOG = LoggerFactory.getLogger(AmalgamatingClassloader.class);

  public static final String KERNEL_PERSISTENCE_XML = "META-INF/kernel-persistence.xml";
  public static final String PERSISTENCE_XML = "META-INF/persistence.xml";
  public static final String ORM_XML = "META-INF/orm.xml";

  private Map<String, PersistenceUnit> settingsMap = new HashMap<String, PersistenceUnit>();
  private PersistenceSettings baseSettings;
  private Set<OrmEntity> ormClasses = new HashSet<OrmEntity>();
  private URL persistenceXMLurl;
  private URL ormXMLurl;

  public AmalgamatingClassloader(ClassLoader classLoader) {
    super(classLoader);
    baseSettings = PersistenceBundleMonitor.parsePersistenceXml(classLoader
        .getResourceAsStream(KERNEL_PERSISTENCE_XML));
    for (PersistenceUnit unit : baseSettings.getPersistenceUnits()) {
      settingsMap.put(unit.getName(), unit);
    }
  }

  public void importPersistenceXml(URL persistence) throws IOException {
    PersistenceSettings newSettings = PersistenceBundleMonitor.parsePersistenceXml(persistence
        .openStream());
    for (PersistenceUnit unit : newSettings.getPersistenceUnits()) {
      PersistenceUnit existingUnit = settingsMap.get(unit.getName());
      if (existingUnit == null) {
        settingsMap.put(unit.getName(), unit);
      } else {
        existingUnit.addProperties(unit.getPropertiesList());
      }
    }
  }

  public void importOrmXml(URL orm) throws IOException {
    OrmSettings settings = PersistenceBundleMonitor.parseOrmXml(orm.openStream());
    for (OrmEntity entity : settings.getEntities()) {
      ormClasses.add(entity);
    }
  }

  public Enumeration<URL> getResources(final String name) throws IOException {
    Enumeration<URL> retEnum = null;
    if (PERSISTENCE_XML.equals(name)) {
      if (persistenceXMLurl == null) {
        persistenceXMLurl = constructUrl(PersistenceBundleMonitor.getPersistenceSettingsXStream(),
            baseSettings, PERSISTENCE_XML);
      }
      retEnum = new UrlEnumeration(persistenceXMLurl);
    }
    // make sure subsequent lookups for orm.xml get the merged copy of the
    // file
    else if (ORM_XML.equals(name)) {
      if (ormXMLurl == null) {
        OrmSettings settings = new OrmSettings();
        settings.setEntities(ormClasses);
        ormXMLurl = constructUrl(PersistenceBundleMonitor.getOrmSettingsXStream(), settings,
            ORM_XML);
      }
      retEnum = new UrlEnumeration(ormXMLurl);
    } else {
      retEnum = super.getResources(name);
    }
    return retEnum;
  }

  /**
   * Constructs a temporary file that merges together the requested filename as
   * it is found in different artifacts (jars). The URL to the merged file is
   * returned.
   * 
   * @param filename
   *          The file to look for in the classloader.
   * @return The merged result of the found filenames.
   * @throws IOException
   */
  private URL constructUrl(XStream xstream, XStreamWritable writable, String filename)
      throws IOException {
    LOG.debug(filename + " " + writable);

    // The base directory must be empty since JPA will scan it searching for
    // classes.
    File file = new File(System.getProperty("java.io.tmpdir") + "/sakai/" + filename);
    if (file.getParentFile().mkdirs()) {
      LOG.debug("Created " + file);
    }

    xstream.toXML(writable, new FileOutputStream(file));
    URL url = null;
    try {
      url = file.toURI().toURL();
    } catch (MalformedURLException e) {
      LOG.error("cannot convert file to URL " + e.toString());
    }
    LOG.debug("URL: " + url);
    return url;
  }

  /**
   * Enumeration for handling the return from getResources of new temp URLs.
   */
  private static class UrlEnumeration implements Enumeration<URL> {
    private URL url;

    UrlEnumeration(URL url) {
      this.url = url;
    }

    public boolean hasMoreElements() {
      return url != null;
    }

    public URL nextElement() {
      final URL url2 = url;
      url = null;
      return url2;
    }
  }

}
