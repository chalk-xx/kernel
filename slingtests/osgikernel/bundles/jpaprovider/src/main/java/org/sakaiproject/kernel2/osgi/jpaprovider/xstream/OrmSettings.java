package org.sakaiproject.kernel2.osgi.jpaprovider.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@XStreamAlias("entity-mappings")
public class OrmSettings extends XStreamWritable {

  private static final Class<?>[] CLASSES = { OrmSettings.class, OrmEntity.class };
  
  @XStreamImplicit(itemFieldName = "entity")
  private List<OrmEntity> entities;

  public List<OrmEntity> getEntities() {
    return entities;
  }

  public static Class<?>[] getOrmClasses() {
    return CLASSES.clone();
  }

  public static XStream getXStream() {
    XStream xstream = new XStream();
    xstream.processAnnotations(OrmSettings.getOrmClasses());
    setupNamespaceAliasing(xstream);
    xstream.aliasSystemAttribute("type", "class");
    xstream.registerConverter(new EntityConverter());
    return xstream;
  }
  
  public static OrmSettings parse(InputStream stream)
  {
    return (OrmSettings) getXStream().fromXML(stream); 
  }

  public void addEntity(OrmEntity entity) {
    if (entities == null)
    {
      entities = new ArrayList<OrmEntity>();
    }
    entities.add(entity);
  }

}
