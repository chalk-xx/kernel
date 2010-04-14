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

import org.apache.commons.lang.NotImplementedException;

import java.util.Iterator;
import java.util.List;

import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * Simple utility class that allows one to create a RowIterator by feeding it a List of
 * Rows.
 */
public class RowIteratorImpl implements RowIterator {

  private Iterator<Row> iterator;

  /**
   * 
   */
  public RowIteratorImpl(List<Row> rows) {
    this.iterator = rows.iterator();
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jcr.query.RowIterator#nextRow()
   */
  public Row nextRow() {
    return iterator.next();
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jcr.RangeIterator#getPosition()
   */
  public long getPosition() {
    throw new NotImplementedException();
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jcr.RangeIterator#getSize()
   */
  public long getSize() {
    throw new NotImplementedException();
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jcr.RangeIterator#skip(long)
   */
  public void skip(long skipNum) {
    while (skipNum > 0) {
      iterator.next();
      skipNum--;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return iterator.hasNext();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Iterator#next()
   */
  public Object next() {
    return iterator.next();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    iterator.remove();
  }

}
