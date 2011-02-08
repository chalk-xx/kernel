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
package org.sakaiproject.nakamura.user.lite.resource;

import org.apache.sling.api.resource.ValueMap;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * ValueMap implementation for Authorizable Resources
 */
public class LiteAuthorizableValueMap implements ValueMap {


    private Authorizable authorizable;


    private Map<String, Object> properties;

    public LiteAuthorizableValueMap(Authorizable authorizable) {
        this.authorizable = authorizable;
        this.properties = authorizable.getSafeProperties();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        if (type == null) {
            return (T) get(name);
        }

        return convertToType(name, type);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, T defaultValue) {
        if (defaultValue == null) {
            return (T) get(name);
        }

        // special handling in case the default value implements one
        // of the interface types supported by the convertToType method
        Class<T> type = (Class<T>) normalizeClass(defaultValue.getClass());

        T value = get(name, type);
        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    public boolean containsKey(Object key) {
       return get(key) != null;
    }

    public boolean containsValue(Object value) {
        return properties.containsValue(value);
    }

    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return properties.entrySet();
    }

    public Object get(Object key) {
      if ( "rep:userId".equals(key) ) {
        return authorizable.getId();
      }
      if ( "rep:principalName".equals(key) ) {
        return authorizable.getId();
      }
      return properties.get(key);
    }

    public Set<String> keySet() {
        return properties.keySet();
    }

    public int size() {
        return properties.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Collection<Object> values() {
        return properties.values();
    }




    /**
     * Reads the authorizable map completely and returns the string
     * representation of the cached properties.
     */
    @Override
    public String toString() {
        return properties.toString();
    }

    // ---------- Unsupported Modification methods

    public Object remove(Object arg0) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Object put(String arg0, Object arg1) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends String, ? extends Object> arg0) {
        throw new UnsupportedOperationException();
    }

    // ---------- Implementation helper

    @SuppressWarnings("unchecked")
    private <T> T convertToType(String name, Class<T> type) {
        T result = null;

          
            if (properties.containsKey(name)) {
                Object value = properties.get(name);

                if (value == null) {
                    return null;
                }

                boolean array = type.isArray();

                    if (array) {
                      String[] values = (String[]) value;
                      
                        result = (T) convertToArray(values,
                            type.getComponentType());
                    } else {
                        result = convertToType(-1, value, type);
                    }
            }

        // fall back to nothing
        return result;
    }

    private <T> T[] convertToArray(Object[] jcrValues, Class<T> type) {
      
        List<T> values = new ArrayList<T>();
        for (int i = 0; i < jcrValues.length; i++) {
            T value = convertToType(i, jcrValues[i], type);
            if (value != null) {
                values.add(value);
            }
        }

        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, values.size());

        return values.toArray(result);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertToType(int index, Object value, Class<T> type) {

        if (String.class == type) {
            return (T) value;
        } else if (Byte.class == type) {
          return (T) Byte.valueOf((byte)StorageClientUtils.toLong(value));
        } else if (Short.class == type) {
          return (T) Short.valueOf((short)StorageClientUtils.toLong(value));
        } else if (Integer.class == type) {
          return (T) Integer.valueOf((int)StorageClientUtils.toLong(value));
        } else if (Long.class == type) {
          return (T) Long.valueOf(StorageClientUtils.toLong(value));
        } else if (Float.class == type) {
            return (T) new Float((Double)value);
        } else if (Double.class == type) {
            return (T) value;
        } else if (Boolean.class == type) {
            return (T) value;
        } else if (Date.class == type) {
            return (T) value;
        } else if (Calendar.class == type) {
            return (T) value;
        }

        // fallback in case of unsupported type
        return null;
    }

    private Class<?> normalizeClass(Class<?> type) {
        if (Calendar.class.isAssignableFrom(type)) {
            type = Calendar.class;
        } else if (Date.class.isAssignableFrom(type)) {
            type = Date.class;
        }
        return type;
    }

}