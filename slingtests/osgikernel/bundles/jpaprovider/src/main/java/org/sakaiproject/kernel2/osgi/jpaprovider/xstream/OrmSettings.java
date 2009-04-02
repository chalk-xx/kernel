package org.sakaiproject.kernel2.osgi.jpaprovider.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@XStreamAlias("entity-mappings")
public class OrmSettings implements XStreamWritable {

  private static final Class<?>[] CLASSES = { OrmSettings.class, OrmEntity.class };
  
  @XStreamImplicit(itemFieldName = "entity")
  private List<OrmEntity> entities;

  public List<OrmEntity> getEntities() {
    return entities;
  }

  public static Class<?>[] getOrmClasses() {
    return CLASSES.clone();
  }

  public void setEntities(Set<OrmEntity> entities) {
    this.entities = Arrays.asList(entities.toArray(new OrmEntity[] {}));
  }

}
