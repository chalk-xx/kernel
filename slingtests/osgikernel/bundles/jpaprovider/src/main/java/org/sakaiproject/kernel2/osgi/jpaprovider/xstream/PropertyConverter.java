package org.sakaiproject.kernel2.osgi.jpaprovider.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class PropertyConverter implements Converter {

  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
    Property property = (Property) value;
    writer.addAttribute("name", property.getName());
    writer.addAttribute("value", property.getValue());
  }

  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    Property property = new Property();
    property.setName(reader.getAttribute("name"));
    property.setValue(reader.getAttribute("value"));
    return property;
  }

  @SuppressWarnings("unchecked")
  public boolean canConvert(Class clazz) {
    return clazz.equals(Property.class);
  }

}
