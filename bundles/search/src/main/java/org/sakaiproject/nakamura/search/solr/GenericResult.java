/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.search.solr;

import com.google.common.collect.Maps;

import org.sakaiproject.nakamura.api.search.solr.Result;

import java.util.Collection;
import java.util.Map;

/**
 * Generic implementation of Result.
 */
public class GenericResult implements Result {
  private String path;
  private Map<String, Collection<Object>> properties;

  public GenericResult(String path, Map<String, Collection<Object>> properties) {
    this.path = path;
    if (properties != null) {
      this.properties = properties;
    } else {
      this.properties = Maps.newHashMap();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.Result#getPath()
   */
  public String getPath() {
    return path;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.Result#getProperties()
   */
  public Map<String, Collection<Object>> getProperties() {
    return properties;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.Result#getFirstValue(java.lang.String)
   */
  public Object getFirstValue(String name) {
    Object retval = null;
    Collection<Object> vals = properties.get(name);
    if (vals != null && vals.size() > 0) {
      retval = vals.iterator().next();
    }
    return retval;
  }

}
