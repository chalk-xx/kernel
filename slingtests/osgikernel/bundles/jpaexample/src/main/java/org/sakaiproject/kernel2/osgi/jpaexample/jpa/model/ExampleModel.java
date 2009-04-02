package org.sakaiproject.kernel2.osgi.jpaexample.jpa.model;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ExampleModel {

  @Id
  private long id;

  @Basic
  private String property;
  
  public long getId()
  {
      return id;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public String getProperty() {
    return property;
  }

}
