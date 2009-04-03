package org.sakaiproject.kernel2.osgi.jpaprovider.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.io.InputStream;
import java.util.List;

@XStreamAlias("persistence")
public class PersistenceSettings extends XStreamWritable {

  private static final Class<?>[] CLASSES = { PersistenceSettings.class, PersistenceUnit.class, Property.class };

  @XStreamImplicit(itemFieldName="persistence-unit")
  private List<PersistenceUnit> persistenceUnit;

  public List<PersistenceUnit> getPersistenceUnits() {
    return persistenceUnit;
  }

  public static Class<?>[] getPersistenceClasses() {
    return CLASSES.clone();
  }

  public static XStream getXStream() {
    XStream xstream = new XStream();
    xstream.processAnnotations(PersistenceSettings.getPersistenceClasses());
    setupNamespaceAliasing(xstream);
    xstream.useAttributeFor(PersistenceUnit.class, "name");
    xstream.registerConverter(new PropertyConverter());
    return xstream;
  }

  public static PersistenceSettings parse(InputStream stream)
  {
    return (PersistenceSettings) getXStream().fromXML(stream); 
  }

}
