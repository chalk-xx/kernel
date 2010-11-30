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
package org.sakaiproject.nakamura.docproxy;

import org.apache.sling.api.resource.ValueMap;
import org.sakaiproject.nakamura.api.docproxy.ExternalSearchResultSet;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import java.util.Iterator;

public class ExternalSearchResultSetImpl implements ExternalSearchResultSet {

  /**
   * The size of the result set.
   */
  private long size;

  /**
   * The iterator that should be used.
   */
  private Iterator<ExternalDocumentResult> resultIterator;

  /**
   * A set of properties that should be output by the ExternalDocumentSearchServlet.
   */
  private ValueMap properties;

  public ExternalSearchResultSetImpl (Iterator<ExternalDocumentResult> resultIterator, long size) {
    setResultIterator(resultIterator);
    setSize(size);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalSearchResultSet#getSize()
   */
  public long getSize() {
    return size;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalSearchResultSet#setSize(long)
   */
  public void setSize(long size) {
    this.size = size;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalSearchResultSet#getResultIterator()
   */
  public Iterator<ExternalDocumentResult> getResultIterator() {
    return resultIterator;
  }

  /**
   * This is protected because I only want the creator to be able to set, but extensions
   * can override the storage.
   */
  protected void setResultIterator(Iterator<ExternalDocumentResult> resultIterator) {
      this.resultIterator = resultIterator;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalSearchResultSet#setProperties(java.util.Dictionary)
   */
  public void setProperties(ValueMap properties) {
    this.properties = properties;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalSearchResultSet#getProperties()
   */
  public ValueMap getProperties() {
    return properties;
  }

}
