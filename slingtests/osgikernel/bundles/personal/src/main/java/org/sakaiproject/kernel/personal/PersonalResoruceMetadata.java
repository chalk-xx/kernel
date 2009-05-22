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
package org.sakaiproject.kernel.personal;

import org.apache.sling.api.resource.ResourceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * 
 */
public class PersonalResoruceMetadata extends ResourceMetadata {

  /**
   *
   */
  private static final long serialVersionUID = -4637955967045469605L;
  private static final Logger LOGGER = LoggerFactory.getLogger(PersonalResoruceMetadata.class);

  /**
   * 
   */
  public PersonalResoruceMetadata(ResourceMetadata resourceMetadata) {
    putAll(resourceMetadata);
  }
  
  /**
   * {@inheritDoc}
   * @see java.util.HashMap#get(java.lang.Object)
   */
  @Override
  public Object get(Object key) {
    Object o = super.get(key);
    LOGGER.info("Got {} as {} ",key,o);
    return o;
  }
  
  /**
   * {@inheritDoc}
   * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
   */
  @Override
  public Object put(String key, Object value) {
    Object o = super.put(key,value);
    LOGGER.info("Put {} as {} got {} ",new Object[]{key,value,o});
    return o;
  }
  
  /**
   * {@inheritDoc}
   * @see java.util.HashMap#keySet()
   */
  @Override
  public Set<String> keySet() {
    LOGGER.info("Getting Key Set ");
    return super.keySet();
  }
  
  /**
   * {@inheritDoc}
   * @see java.util.HashMap#values()
   */
  @Override
  public Collection<Object> values() {
    LOGGER.info("Getting Values ");
    return super.values();
  }
  
  /**
   * {@inheritDoc}
   * @see java.util.HashMap#entrySet()
   */
  @Override
  public Set<java.util.Map.Entry<String, Object>> entrySet() {
    LOGGER.info("Getting Entries ");
     return super.entrySet();
  }
}
