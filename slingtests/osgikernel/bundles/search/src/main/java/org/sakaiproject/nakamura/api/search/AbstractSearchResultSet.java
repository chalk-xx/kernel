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
package org.sakaiproject.nakamura.api.search;

import org.apache.sling.api.resource.ValueMap;

import javax.jcr.query.RowIterator;

public class AbstractSearchResultSet implements SearchResultSet {

  /**
   * The size of the result set.
   */
  private long size;

  /**
   * The iterator that should be used.
   */
  private RowIterator rowIterator;

  /**
   * A set of properties that should be outputted by the SearchServlet.
   */
  private ValueMap properties;

  public AbstractSearchResultSet(RowIterator rowIterator, long size) {
    setRowIterator(rowIterator);
    setSize(size);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchResultSet#getSize()
   */
  public long getSize() {
    return size;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchResultSet#setSize(long)
   */
  public void setSize(long size) {
    this.size = size;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchResultSet#getRowIterator()
   */
  public RowIterator getRowIterator() {
    return rowIterator;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchResultSet#setRowIterator(javax.jcr.query.RowIterator)
   */
  public void setRowIterator(RowIterator rowIterator) {
    this.rowIterator = rowIterator;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchResultSet#setProperties(java.util.Dictionary)
   */
  public void setProperties(ValueMap properties) {
    this.properties = properties;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchResultSet#getProperties()
   */
  public ValueMap getProperties() {
    return properties;
  }

}
