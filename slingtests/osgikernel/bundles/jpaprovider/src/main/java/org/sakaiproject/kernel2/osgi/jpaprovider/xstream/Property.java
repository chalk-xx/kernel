package org.sakaiproject.kernel2.osgi.jpaprovider.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("property")
public class Property {

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
    {
      return false;
    }
    if (!(obj instanceof Property))
    {
      return false;
    }
    Property other = (Property) obj;
    if (!getName().equals(other.getName()))
    {
      return false;
    }
    return getValue().equals(other.getValue());
  }

  @Override
  public int hashCode() {
    return getName().hashCode() + getValue().hashCode();
  }

  private String name;
  private String value;

  public String getName() {
    return (name != null ? name : "");
  }

  public String getValue() {
    return (value != null ? value : "");
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
