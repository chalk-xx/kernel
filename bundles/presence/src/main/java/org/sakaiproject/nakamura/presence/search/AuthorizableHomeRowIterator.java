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
package org.sakaiproject.nakamura.presence.search;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_HOME_RESOURCE_TYPE;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_HOME_RESOURCE_TYPE;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 *
 */
public class AuthorizableHomeRowIterator implements RowIterator {
  private RowIterator rows;

  public AuthorizableHomeRowIterator(RowIterator rows) {
    this.rows = rows;
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.jcr.RangeIterator#skip(long)
   */
  public void skip(long skipNum) {
    rows.skip(skipNum);
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.jcr.RangeIterator#getSize()
   */
  public long getSize() {
    return rows.getSize();
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.jcr.RangeIterator#getPosition()
   */
  public long getPosition() {
    return rows.getPosition();
  }

  /**
   * {@inheritDoc}
   *
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return rows.hasNext();
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
    rows.remove();
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.jcr.query.RowIterator#nextRow()
   */
  public Row nextRow() {
    Row row = rows.nextRow();
    String rowPath = null;
    try {
      rowPath = row.getPath();
      Node homeNode = getHomeNode(row.getNode());
      Row retRow = node2Row(homeNode);
      return retRow;
    } catch (RepositoryException e) {
      throw new IllegalArgumentException("Unable to find home in hierarchy [" + rowPath
          + "]");
    }
  }

  private Node getHomeNode(Node node) throws RepositoryException {
    if (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)
        && (USER_HOME_RESOURCE_TYPE.equals(node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString())
        || GROUP_HOME_RESOURCE_TYPE.equals(node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString()))) {
      return node;
    } else {
      return getHomeNode(node.getParent());
    }
  }

  private Row node2Row(final Node node) {
    Row row = new Row() {

      public Value[] getValues() throws RepositoryException {
        return null;
      }

      public Value getValue(String propertyName) throws ItemNotFoundException,
          RepositoryException {
        return node.getProperty(propertyName).getValue();
      }

      public Node getNode() throws RepositoryException {
        return node;
      }

      public Node getNode(String arg0) throws RepositoryException {
        return node;
      }

      public String getPath() throws RepositoryException {
        return node.getPath();
      }

      public String getPath(String arg0) throws RepositoryException {
        return node.getPath();
      }

      public double getScore() throws RepositoryException {
        return -1;
      }

      public double getScore(String arg0) throws RepositoryException {
        return -1;
      }
    };
    return row;
  }
}
