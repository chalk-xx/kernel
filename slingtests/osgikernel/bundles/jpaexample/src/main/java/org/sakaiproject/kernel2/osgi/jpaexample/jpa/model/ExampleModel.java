package org.sakaiproject.kernel2.osgi.jpaexample.jpa.model;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ExampleModel implements Serializable {

  private static final long serialVersionUID = 1L;

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
