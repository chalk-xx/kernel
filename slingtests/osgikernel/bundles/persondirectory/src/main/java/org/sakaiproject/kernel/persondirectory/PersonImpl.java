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
package org.sakaiproject.kernel.persondirectory;

import org.sakaiproject.kernel.api.persondirectory.Person;

import java.util.HashMap;
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
  private Map<String, String[]> attrs = new HashMap<String, String[]>();

  public PersonImpl(String uid) {
    this.uid = uid;
  }

  public PersonImpl(Person p) {
    this.uid = p.getName();
    this.attrs = p.getAttributes();
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

  public void addAttributes(Map<String, String[]> attributes) {
    for (Map.Entry<String, String[]> attribute : attributes.entrySet()) {
      addAttribute(attribute.getKey(), attribute.getValue());
    }
  }
}
