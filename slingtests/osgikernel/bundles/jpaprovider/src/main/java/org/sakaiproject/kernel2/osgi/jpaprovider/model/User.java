package org.sakaiproject.kernel2.osgi.jpaprovider.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;

@Entity
public class User {

  @Transient
  @PersistenceContext(unitName = "user")

  @Id
  private long id;

  private String name;
  
  public long getId()
  {
      return id;
  }

  public String getName()
  {
      return name;
  }

  public void setName(String name)
  {
      this.name = name;
  }

}
