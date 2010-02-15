/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.persondirectory;

import org.sakaiproject.nakamura.api.persondirectory.Person;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Basic implementation of the {@link Person} interface. Adds edit capabilities
 * such as adding attributes and setting the name of the {@link Person}.
 *
 * @author Carl Hall
 */
public class PersonImpl implements Person {
  private String uid;
  private Map<String, String[]> attrs;

  /**
   * Create a person with a just a user ID.
   *
   * @param uid
   */
  public PersonImpl(String uid) {
    this.uid = uid;
    attrs = new HashMap<String, String[]>();
  }

  /**
   * Create a person with all attributes from another person.
   *
   * @param p
   */
  public PersonImpl(Person p) {
    uid = p.getName();
    attrs = new HashMap<String, String[]>(p.getAttributes());
  }

  public Set<String> getAttributeNames() {
    return attrs.keySet();
  }

  public String getAttributeValue(String attributeName) {
    String retval = null;
    if (attrs.containsKey(attributeName)) {
      String[] vals = attrs.get(attributeName);
      if (vals != null && vals.length > 0) {
        retval = attrs.get(attributeName)[0];
      }
    }
    return retval;
  }

  public String[] getAttributeValues(String attributeName) {
    String[] retval = null;
    if (attrs.containsKey(attributeName)) {
      retval = attrs.get(attributeName);
    }
    return retval;
  }

  public Map<String, String[]> getAttributes() {
    return attrs;
  }

  public String getName() {
    return uid;
  }

  public void addAttribute(String name, String... values) {
    if (attrs.containsKey(name)) {
      // found values associated to the attribute name. merge the new values
      // with the current ones
      String[] currVals = attrs.get(name);
      String[] newVals = new String[currVals.length + values.length];

      // start with the current values
      int i = 0;
      for (String val : currVals) {
        newVals[i++] = val;
      }

      // append the new values
      for (String val : values) {
        newVals[i++] = val;
      }

      // put the merged values into the map
      attrs.put(name, newVals);
    } else {
      attrs.put(name, values);
    }
  }

  /**
   * Add attributes to a person.
   *
   * @param attributes
   *          The attributes to be added to the person.
   */
  public void addAttributes(Map<String, String[]> attributes) {
    addAttributes(attributes, (String[]) null);
  }

  /**
   * Add attributes to a person but only those listed as part of the attribute
   * filter.
   *
   * @param attributes
   *          The attributes to add to the person.
   * @param attributeFilter
   *          The attributes names to add. If any attributes in
   *          <code>attributes</code> are not in this list, they will not be
   *          added.
   */
  public void addAttributes(Map<String, String[]> attributes, String... attributeFilter) {
    HashSet<String> filter = null;
    if (attributeFilter != null && attributeFilter.length > 0) {
      filter = new HashSet<String>();
      for (String attrFilter : attributeFilter) {
        filter.add(attrFilter);
      }
    }

    for (Map.Entry<String, String[]> attribute : attributes.entrySet()) {
      String key = attribute.getKey();
      if (filter == null || filter.contains(key)) {
        String[] value = attribute.getValue();
        addAttribute(key, value);
      }
    }
  }
}
