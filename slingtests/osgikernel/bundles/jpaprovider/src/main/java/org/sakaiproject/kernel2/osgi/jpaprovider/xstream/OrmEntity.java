package org.sakaiproject.kernel2.osgi.jpaprovider.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("entity")
public class OrmEntity {

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
    {
      return false;
    }
    if (!(obj instanceof OrmEntity))
    {
      return false;
    }
    OrmEntity other = (OrmEntity) obj;
    return other.getClassName().equals(getClassName());
  }

  @Override
  public int hashCode() {
    return getClassName().hashCode();
  }

  private String className;

  public void setClassName(String className) {
    this.className = className;
  }

  public String getClassName() {
    return (className != null ? className : "");
  }
}
