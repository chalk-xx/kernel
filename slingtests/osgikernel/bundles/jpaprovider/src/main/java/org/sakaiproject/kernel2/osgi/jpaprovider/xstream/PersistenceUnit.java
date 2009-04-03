package org.sakaiproject.kernel2.osgi.jpaprovider.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XStreamAlias("persistence-unit")
public class PersistenceUnit {

  private String name;
  
  @XStreamImplicit(itemFieldName = "class")
  private List<String> classes;

  @XStreamAlias(value = "properties", impl = ArrayList.class)
  private List<Property> properties;

  public List<String> getClasses() {
    return classes;
  }

  public Map<String, String> getProperties() {
    Map<String, String> result = new HashMap<String, String>();
    for (Property property : properties) {
      result.put(property.getName(), property.getValue());
    }
    return result;
  }

  public String getName() {
    return name;
  }

  public List<Property> getPropertiesList() {
    return properties;
  }

  public void addProperties(List<Property> propertiesList) {
    if (propertiesList == null)
    {
      return;
    }
    if (properties == null)
    {
      properties = new ArrayList<Property>();
    }
    for (Property prop : propertiesList)
    {
      if (!properties.contains(prop))
      {
        properties.add(prop);
      }
    }
  }

  public void addClasses(List<String> newClasses) {
    if (newClasses == null)
    {
      return;
    }
    if (classes == null)
    {
      classes = new ArrayList<String>();
    }
    classes.addAll(newClasses);
  }
  
}
