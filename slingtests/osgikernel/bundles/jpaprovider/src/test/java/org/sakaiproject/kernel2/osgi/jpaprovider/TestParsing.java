package org.sakaiproject.kernel2.osgi.jpaprovider;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.OrmEntity;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.OrmSettings;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PersistenceSettings;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PersistenceUnit;

import java.util.List;
import java.util.Map;

public class TestParsing {

  @Test
  public void testProviderXmlParse() {
    PersistenceSettings settings = PersistenceBundleMonitor.parsePersistenceXml(this.getClass().getClassLoader().getResourceAsStream("persistence.xml"));
    List<PersistenceUnit> units = settings.getPersistenceUnits();
    assertEquals("Expected there to be one unit", 1, units.size());
    PersistenceUnit unit = units.get(0);
    assertEquals("Expected name to be set", "sakaiPU", unit.getName());
    List<String> persistedClasses = unit.getClasses();
    assertEquals("Expected there to be one class", 1, persistedClasses.size());
    Map<String,String> properties = unit.getProperties();
    assertEquals("Expected eclipselink user to be set", "sa", properties.get("eclipselink.jdbc.user"));
  }

  @Test
  public void testOrmXmlParse() {
    OrmSettings settings = PersistenceBundleMonitor.parseOrmXml(this.getClass().getClassLoader().getResourceAsStream("orm.xml"));
    List<OrmEntity> entities = settings.getEntities();
    assertEquals("Expected there to be one entity", 1, entities.size());
    assertEquals("Expected class value to be recorded", "org.sakaiproject.kernel2.osgi.jpaprovider.model.SystemUser", entities.get(0).getClassName());
  }
}
