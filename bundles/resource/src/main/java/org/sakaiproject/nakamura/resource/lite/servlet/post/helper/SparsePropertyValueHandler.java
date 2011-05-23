/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sakaiproject.nakamura.resource.lite.servlet.post.helper;

import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.resource.DateParser;
import org.sakaiproject.nakamura.api.resource.lite.SparseRequestProperty;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Sets a Property on the given Node, in some cases with a specific type and value. For
 * example, "lastModified" with an empty value is stored as the current Date.
 */
public class SparsePropertyValueHandler {

  /**
   * String constant for type name as used in serialization.
   */
  public static final String TYPENAME_STRING = "String";

  /**
   * String constant for type name as used in serialization.
   */
  public static final String TYPENAME_BINARY = "Binary";

  /**
   * String constant for type name as used in serialization.
   */
  public static final String TYPENAME_LONG = "Long";

  /**
   * String constant for type name as used in serialization.
   */
  public static final String TYPENAME_DOUBLE = "Double";

  /**
   * String constant for type name as used in serialization.
   *
   * @since JCR 2.0
   */
  public static final String TYPENAME_DECIMAL = "Decimal";

  /**
   * String constant for type name as used in serialization.
   */
  public static final String TYPENAME_DATE = "Date";

  /**
   * String constant for type name as used in serialization.
   */
  public static final String TYPENAME_BOOLEAN = "Boolean";

  /**
   * String constant for type name as used in serialization.
   */
  public static final String TYPENAME_NAME = "Name";

  /**
   * String constant for type name as used in serialization.
   */
  public static final String TYPENAME_PATH = "Path";

  /**
   * String constant for type name as used in serialization.
   */
  public static final String TYPENAME_REFERENCE = "Reference";

  /**
   * String constant for type name as used in serialization.
   *
   * @since JCR 2.0
   */
  public static final String TYPENAME_WEAKREFERENCE = "WeakReference";

  /**
   * String constant for type name as used in serialization.
   *
   * @since JCR 2.0
   */
  public static final String TYPENAME_URI = "URI";

  /*
   * String constant for type name as used in serialization.
   */
  public static final String TYPENAME_UNDEFINED = "undefined";

  /**
   * the post processor
   */
  private final List<Modification> changes;

  private final DateParser dateParser;

  /**
   * Constructs a propert value handler
   */
  public SparsePropertyValueHandler(DateParser dateParser, List<Modification> changes) {
    this.dateParser = dateParser;
    this.changes = changes;
  }



  /**
   * Removes the property with the given name from the parent node if it exists and if
   * it's not a mandatory property.
   *
   * @param parent
   *          the parent node
   * @param name
   *          the name of the property to remove
   * @return path of the property that was removed or <code>null</code> if it was not
   *         removed
   * @throws RepositoryException
   *           if a repository error occurs.
   */
  private String removePropertyIfExists(Content content, String name) {
    if (content.hasProperty(name)) {
      content.removeProperty(name);
      return content.getPath() + "@" + name;
    }
    return null;
  }

  /**
   * set property without processing, except for type hints
   *
   * @param parent
   *          the parent node
   * @param prop
   *          the request property
   * @throws RepositoryException
   *           if a repository error occurs.
   */
  public void setProperty(Content content, SparseRequestProperty prop) {
    // no explicit typehint

    String type = prop.getTypeHint();
    String[] values = prop.getStringValues();

    if (values == null) {
      // remove property
      final String removePath = removePropertyIfExists(content, prop.getName());
      if (removePath != null) {
        changes.add(Modification.onDeleted(removePath));
      }
    } else if (values.length == 0) {
      // do not create new prop here, but clear existing
      // remove property
      final String removePath = removePropertyIfExists(content, prop.getName());
      if (removePath != null) {
        changes.add(Modification.onDeleted(removePath));
      }
    } else if (values.length == 1) {
      if (values[0].length() == 0) {
        // remove property
        final String removePath = removePropertyIfExists(content, prop.getName());
        if (removePath != null) {
          changes.add(Modification.onDeleted(removePath));
        }
      } else {
        content.setProperty(prop.getName(),fromRequest(type,values));
        changes.add(Modification.onModified(prop.getParentPath() + "@" + prop.getName()));
      }
    } else {
      content.setProperty(prop.getName(),fromRequest(type, values));
      changes.add(Modification.onModified(prop.getParentPath() + "@" + prop.getName()));
    }
  }

  /**
   * TODO Not currently used but kept here while we work on Sparse port.
   */
  private Object fromRequest(String type, String[] values) {
    if ( type == null ) {
      if ( values.length == 1 ) {
        return values[0];
      } 
      return values;
    }
    if (type.equals(TYPENAME_STRING)) {
      if ( values.length == 1 ) {
        return values[0];
      } 
      return values;
    } else if (type.equals(TYPENAME_BINARY)) {
      return null;
    } else if (type.equals(TYPENAME_BOOLEAN)) {
      boolean[] b = new boolean[values.length];
      for (int i = 0; i < values.length; i++) {
        b[i] = Boolean.parseBoolean(values[i]);
      }
      if ( values.length == 1 ) {
        return b[0];
      } 
      return b;
      
    } else if (type.equals(TYPENAME_LONG)) {
      long[] b = new long[values.length];
      for (int i = 0; i < values.length; i++) {
        b[i] = Long.parseLong(values[i]);
      }
      if ( values.length == 1 ) {
        return b[0];
      } 
      return b;
    } else if (type.equals(TYPENAME_DOUBLE)) {
      double[] b = new double[values.length];
      for (int i = 0; i < values.length; i++) {
        b[i] = Double.parseDouble(values[i]);
      }
      if ( values.length == 1 ) {
        return b[0];
      } 
      return b;
    } else if (type.equals(TYPENAME_DECIMAL)) {
      BigDecimal[] b = new BigDecimal[values.length];
      for (int i = 0; i < values.length; i++) {
        b[i] = new BigDecimal(values[i]);
      }
      if ( values.length == 1 ) {
        return b[0];
      } 
      return b;
    } else if (type.equals(TYPENAME_DATE)) {
      Calendar[] b = new Calendar[values.length];
      for (int i = 0; i < values.length; i++) {
        b[i] = dateParser.parse(values[i]);
      }
      if ( values.length == 1 ) {
        return b[0];
      } 
      return b;
    }
    if ( values.length == 1 ) {
      return values[0];
    } 
    return values;
  }

}
