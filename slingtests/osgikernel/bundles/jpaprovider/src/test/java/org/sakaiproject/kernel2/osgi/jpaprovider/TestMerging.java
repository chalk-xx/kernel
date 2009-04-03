package org.sakaiproject.kernel2.osgi.jpaprovider;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.OrmEntity;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.OrmSettings;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PersistenceSettings;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PersistenceUnit;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestMerging {

  private Bundle createDummyBundle(String puName, String persistenceXml, String ormXml) throws IOException
  {
    Map<String,String> dummyFiles1 = new HashMap<String,String>();
    dummyFiles1.put(AmalgamatingClassloader.PERSISTENCE_XML, persistenceXml);
    dummyFiles1.put(AmalgamatingClassloader.ORM_XML, ormXml);
    return new DummyJpaBundle(puName, dummyFiles1, Bundle.STARTING);
  }
  
  @Test
  public void testMergePersistences() throws Exception
  {
    PersistenceBundleMonitor monitor = new PersistenceBundleMonitor();
    monitor.start(new DummyBundleContext());
    Bundle fakeBundle = createDummyBundle("default", "persistence1.xml", "orm1.xml");
    monitor.bundleChanged(new BundleEvent(BundleEvent.STARTING, fakeBundle));
    fakeBundle = createDummyBundle("default", "persistence2.xml", "orm2.xml");
    monitor.bundleChanged(new BundleEvent(BundleEvent.STARTING, fakeBundle));
    ClassLoader amalgamatingClassloader = PersistenceBundleMonitor.getAmalgamatedClassloader("default");
    PersistenceSettings settings = PersistenceBundleMonitor.parsePersistenceXml(amalgamatingClassloader.getResourceAsStream("META-INF/persistence.xml"));
    assertEquals("Expected one persistence unit", 1, settings.getPersistenceUnits().size());
    PersistenceUnit unit = settings.getPersistenceUnits().get(0);
    Map<String, String> props = unit.getProperties();
    assertEquals("Expected testproperty to be set", "testvalue", props.get("testproperty"));
    assertEquals("Expected testproperty2 to be set", "testvalue2", props.get("testproperty2"));
    OrmSettings ormSettings = PersistenceBundleMonitor.parseOrmXml(amalgamatingClassloader.getResourceAsStream("META-INF/orm.xml"));
    List<OrmEntity> entities = ormSettings.getEntities();
    assertEquals("Expected to find 4 ORM entities", 4, entities.size());
  }
}
