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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This iterator tries to count the number of items in an Iterator on construction. If
 * there are more items than the specified maximum, the size will be -max, if there are
 * less, the size will be the number of items. All the other functions operate in the same
 * way as a normal iterator.
 */
public class CountingIterator<T> implements Iterator<T> {

  private int count;
  private Iterator<T> underlyingIterator;
  private List<T> rows = new ArrayList<T>();
  private Iterator<T> preloadedIterator;
  private long position;
  private Iterator<T> iterator;

  public CountingIterator(Iterator<T> underlyingIterator, int max) {
    this.underlyingIterator = underlyingIterator;
    count = 0;
    boolean hasMore = true;
    while (count < max ) {
      if ( underlyingIterator.hasNext() ) {
        count++;
        rows.add(underlyingIterator.next());
      } else {
        hasMore = false;
        break;
      }
    }
    if ( hasMore ) {
      count *= -1;
    }
    preloadedIterator = rows.iterator();
  }

  public void skip(long skipNum) {
    while (skipNum > 0) {
      next();
      skipNum--;
      position++;
    }
  }

  public long getSize() {
    return count;
  }

  public long getPosition() {
    return position;
  }

  public boolean hasNext() {
    if (preloadedIterator.hasNext()) {
      iterator = preloadedIterator;
    } else if (count < 0 && underlyingIterator.hasNext()) {
      iterator = underlyingIterator;
    } else {
      iterator = null;
    }
    return (iterator != null);
  }

  public T next() {
    if ( iterator == null ) {
      if ( !hasNext()) {
        throw new NoSuchElementException();
      }
    }
    T o = null;
    if (iterator != null) {
      o = iterator.next();
      position++;
      iterator = null; // force evaluation of hasNext
    }
    return o;
  }

  public void remove() {
    if ( iterator == null ) {
      if (!hasNext() ) {
        throw new NoSuchElementException();
      }
    }
    if (iterator != null) {
      iterator.remove();
      iterator = null; // force evaluation of hasNext
    }
  }
}
