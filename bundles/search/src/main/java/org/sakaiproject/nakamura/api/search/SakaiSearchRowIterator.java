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

import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_EXCLUDE_TREE;

import java.util.Arrays;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * This class wraps a {@link RowIterator}. When you request the nextRow it will check if
 * it can return the next {@link Row}. It checks if a {@link Row} that represents a
 * {@link Node} is in a blacklisted tree. If the {@link Node} is in a blacklisted tree, it
 * will skip it and fetch the next one. This iterator always lazy loads the next row.
 */
public class SakaiSearchRowIterator implements RowIterator {

  private RowIterator iterator;
  private long position;
  private Row nextRow;
  private String[] blacklistedPaths;

  /**
   * @param iterator
   *          The iterator that should be wrapped.
   */
  public SakaiSearchRowIterator(RowIterator iterator) {
    this.iterator = iterator;
    this.position = -1;
    loadNextRow();
  }

  /**
   * 
   * @param iterator
   *          The iterator that should be wrapped.
   * @param blacklistedPaths
   *          An array of paths that should be ignored.
   */
  public SakaiSearchRowIterator(RowIterator iterator, String[] blacklistedPaths) {
    this.iterator = iterator;
    this.position = -1;
    if (blacklistedPaths != null) {
      Arrays.sort(blacklistedPaths);
      this.blacklistedPaths = blacklistedPaths;
    }
    loadNextRow();
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jcr.query.RowIterator#nextRow()
   */
  public Row nextRow() {
    if (nextRow == null) {
      throw new NoSuchElementException();
    }
    Row r = nextRow;
    loadNextRow();
    return r;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jcr.RangeIterator#getPosition()
   */
  public long getPosition() {
    return position;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jcr.RangeIterator#getSize()
   */
  public long getSize() {
    // Without running over the entire result set and filtering out the Rows that should
    // not be included we don't know the exact size.
    return -1;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jcr.RangeIterator#skip(long)
   */
  public void skip(long skipNum) {
    while (skipNum > 0) {
      nextRow();
      skipNum--;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return (nextRow != null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Iterator#next()
   */
  public Object next() {
    return nextRow();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    throw new UnsupportedOperationException("remove");
  }

  /**
   * @param row
   * @return
   */
  protected boolean isValid(Row row) {
    try {
      Node node = row.getNode();
      return isValid(node);
    } catch (RepositoryException e) {
      return false;
    }
  }

  /**
   * Checks if this node should be included in the search results.
   * 
   * @param node
   * @return Wether or not the node should be included in the search results.
   */
  protected boolean isValid(Node node) {
    try {
      String path = node.getPath();
      if ("/".equals(path)) {
        return true;
      } else if (node.hasProperty(SAKAI_EXCLUDE_TREE)) {
        return !node.getProperty(SAKAI_EXCLUDE_TREE).getBoolean();
      } else {
        if (blacklistedPaths != null) {
          for (String blackPath : blacklistedPaths) {
            if (path.startsWith(blackPath)) {
              return false;
            }
          }
        }
        return isValid(node.getParent());
      }
    } catch (RepositoryException e) {
      return false;
    }
  }

  /**
   * Loads the next available row.
   */
  protected void loadNextRow() {
    nextRow = null;
    while (iterator.hasNext()) {
      Row row = iterator.nextRow();
      if (isValid(row)) {
        position++;
        nextRow = row;
        break;
      }
    }
  }

}
