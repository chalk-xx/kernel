package org.sakaiproject.nakamura.http.cache;

import java.io.Serializable;


public class Operation implements  Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -5070931567527463763L;
  private int op;
  private Object[] v;
  
  public Operation(int op, Object ... values) {
    this.op = op;
    this.v = values;
  }

  public int getOperation() {
    return op;
  }

  @SuppressWarnings("unchecked")
  public <T> T get(int i) {
    return (T) v[i];
  }

}
