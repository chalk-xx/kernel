package org.sakaiproject.kernel2.osgi.jpaprovider.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;

@XStreamAlias("persistence")
public class PersistenceSettings implements XStreamWritable {

  private static final Class<?>[] CLASSES = { PersistenceSettings.class, PersistenceUnit.class, Property.class };

  @XStreamImplicit(itemFieldName="persistence-unit")
  private List<PersistenceUnit> persistenceUnit;

  public List<PersistenceUnit> getPersistenceUnits() {
    return persistenceUnit;
  }

  public static Class<?>[] getPersistenceClasses() {
    return CLASSES.clone();
  }

}
