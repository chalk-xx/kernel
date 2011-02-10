/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.sakaiproject.nakamura.api.resource.lite;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Portions of this code were borrowed from ValueMapDecorator and is why we maintain the
 * Apache licensing.
 */
/**
 * Decorates another {@link Map} to provide a basic implementation for the additional
 * methods of a {@link ValueMap}.
 * 
 * @see ValueMapDecorator
 */
public class SparseValueMapDecorator implements ValueMap {
  private final ValueMapDecorator valueMapDecorator;

  private static final Logger LOG = LoggerFactory
      .getLogger(SparseValueMapDecorator.class);

  /**
   * @param base
   *          Map to be decorated.
   */
  public SparseValueMapDecorator(Map<String, Object> base) {
    valueMapDecorator = new ValueMapDecorator(base);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#clear()
   */
  public void clear() {
    valueMapDecorator.clear();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#containsKey(java.lang.Object)
   */
  public boolean containsKey(Object key) {
    return valueMapDecorator.containsKey(key);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#containsValue(java.lang.Object)
   */
  public boolean containsValue(Object value) {
    return valueMapDecorator.containsValue(value);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#entrySet()
   */
  public Set<java.util.Map.Entry<String, Object>> entrySet() {
    return valueMapDecorator.entrySet();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#get(java.lang.Object)
   */
  public Object get(Object key) {
    return valueMapDecorator.get(key);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#isEmpty()
   */
  public boolean isEmpty() {
    return valueMapDecorator.isEmpty();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#keySet()
   */
  public Set<String> keySet() {
    return valueMapDecorator.keySet();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#put(java.lang.Object, java.lang.Object)
   */
  public Object put(String key, Object value) {
    return valueMapDecorator.put(key, value);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#putAll(java.util.Map)
   */
  public void putAll(Map<? extends String, ? extends Object> map) {
    valueMapDecorator.putAll(map);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#remove(java.lang.Object)
   */
  public Object remove(Object key) {
    return valueMapDecorator.remove(key);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#size()
   */
  public int size() {
    return valueMapDecorator.size();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#values()
   */
  public Collection<Object> values() {
    return valueMapDecorator.values();
  }

  /**
   * Overrides implementation with local sparse conversion.
   * 
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Class)
   */
  public <T> T get(String name, Class<T> type) {
    LOG.debug("get(String {}, Class<T> {})", name, type);
    return convert(get(name), type);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  public <T> T get(String name, T defaultValue) {
    LOG.debug("get(String {}, T {})", name, defaultValue);
    if (defaultValue == null) {
      return (T) get(name);
    }
    T value = get(name, (Class<T>) defaultValue.getClass());
    return value == null ? defaultValue : value;
  }

  /**
   * Converts the object to the given type.
   * 
   * @param obj
   *          object
   * @param type
   *          type
   * @return the converted object
   */
  @SuppressWarnings({ "unchecked", "deprecation" })
  private <T> T convert(Object obj, Class<T> type) {
    LOG.debug("convert(Object {}, Class<T> {})", obj, type);
    try {
      if (obj == null) {
        return null;
      } else if (type.isAssignableFrom(obj.getClass())) {
        return (T) obj;
      } else if (type.isArray()) {
        return (T) convertToArray(obj, type.getComponentType());
      } else if (type == String.class) {
        return (T) StorageClientUtils.toString(obj);
      } else if (type == Integer.class) {
        return (T) (Integer) StorageClientUtils.toInt(obj.toString());
      } else if (type == Long.class) {
        return (T) (Long) StorageClientUtils.toLong(obj.toString());
      } else if (type == Boolean.class) {
        return (T) (Boolean) StorageClientUtils.toBoolean(obj.toString());
      } else if (type == Calendar.class) {
        return (T) (Calendar) StorageClientUtils.toCalendar(obj.toString());
      } else {
        return null;
      }
    } catch (NumberFormatException e) {
      return null;
    } catch (ParseException e) {
      return null;
    }
  }

  /**
   * Converts the object to an array of the given type
   * 
   * @param obj
   *          the object or object array
   * @param type
   *          the component type of the array
   * @return and array of type T
   */
  private <T> T[] convertToArray(Object obj, Class<T> type) {
    LOG.debug("convertToArray(Object {}, Class<T> {})", obj, type);
    List<T> values = new LinkedList<T>();
    if (obj.getClass().isArray()) {
      for (Object o : (Object[]) obj) {
        values.add(convert(o, type));
      }
    } else {
      values.add(convert(obj, type));
    }
    @SuppressWarnings("unchecked")
    T[] result = (T[]) Array.newInstance(type, values.size());
    return values.toArray(result);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return super.toString() + " : " + this.valueMapDecorator.toString();
  }
}
