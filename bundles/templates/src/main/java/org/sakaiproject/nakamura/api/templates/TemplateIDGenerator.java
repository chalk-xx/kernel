package org.sakaiproject.nakamura.api.templates;

import java.util.HashMap;

public class TemplateIDGenerator extends HashMap<String, String> {

  private IDGenerator idGenerator;

  public TemplateIDGenerator(IDGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }

  /**
   *
   */
  private static final long serialVersionUID = 8434260923805908997L;

  @Override
  public String get(Object key) {
    String id = super.get(key);
    if (id == null) {
      id = idGenerator.nextId();
      super.put((String) key, id);
    }
    return id;
  }

}
