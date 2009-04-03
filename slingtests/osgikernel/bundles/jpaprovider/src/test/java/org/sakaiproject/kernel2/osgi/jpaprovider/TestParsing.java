package org.sakaiproject.kernel2.osgi.jpaprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.OrmEntity;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.OrmSettings;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PersistenceSettings;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.PersistenceUnit;
import org.sakaiproject.kernel2.osgi.jpaprovider.xstream.XStreamWritable;

import java.util.List;
import java.util.Map;
/*xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd"
  version="1.0" xmlns="http://java.sun.com/xml/ns/persistence"*/
public class TestParsing {

  private void checkNamespaceAppears(XStreamWritable xstreamObject)
  {
    assertNotNull("Expected xsi ref", xstreamObject.getXsiLocation());
    assertNotNull("Expected schemaLocation", xstreamObject.getSchemaLocation());
    assertNotNull("Expected version", xstreamObject.getVersion());
    assertNotNull("Expected namespace", xstreamObject.getNamespace());    
  }
  
  @Test
  public void testProviderXmlParse() {
    PersistenceSettings settings = PersistenceSettings.parse(this.getClass().getClassLoader().getResourceAsStream("persistence1.xml"));
    List<PersistenceUnit> units = settings.getPersistenceUnits();
    checkNamespaceAppears(settings);
    assertEquals("Expected there to be one unit", 1, units.size());
    PersistenceUnit unit = units.get(0);
    assertEquals("Expected name to be set", "default", unit.getName());
    List<String> persistedClasses = unit.getClasses();
    assertEquals("Expected there to be two classes", 2, persistedClasses.size());
    Map<String,String> properties = unit.getProperties();
    assertEquals("Expected testproperty to be set", "testvalue", properties.get("testproperty"));
  }

  @Test
  public void testOrmXmlParse() {
    OrmSettings settings = OrmSettings.parse(this.getClass().getClassLoader().getResourceAsStream("orm1.xml"));
    checkNamespaceAppears(settings);
    List<OrmEntity> entities = settings.getEntities();
    assertEquals("Expected there to be two entities", 2, entities.size());
    assertEquals("Expected class value to be recorded", "org.sakaiproject.kernel2.osgi.jpaprovider.model.SystemUser", entities.get(0).getClassName());
  }
}
