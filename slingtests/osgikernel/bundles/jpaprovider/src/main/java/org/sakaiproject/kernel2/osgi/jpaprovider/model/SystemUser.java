package org.sakaiproject.kernel2.osgi.jpaprovider.model;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SystemUser {

  @Id
  private long id;

  @Basic
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
