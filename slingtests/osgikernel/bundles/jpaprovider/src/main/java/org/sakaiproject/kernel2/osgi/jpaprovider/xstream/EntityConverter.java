package org.sakaiproject.kernel2.osgi.jpaprovider.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class EntityConverter implements Converter {

  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
    OrmEntity entity = (OrmEntity) value;
    writer.startNode("entity");
    writer.addAttribute("class", entity.getClassName());
    writer.endNode();
  }

  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    OrmEntity entity = new OrmEntity();
    entity.setClassName(reader.getAttribute("class"));
    return entity;
  }

  @SuppressWarnings("unchecked")
  public boolean canConvert(Class clazz) {
    return clazz.equals(OrmEntity.class);
  }

}
