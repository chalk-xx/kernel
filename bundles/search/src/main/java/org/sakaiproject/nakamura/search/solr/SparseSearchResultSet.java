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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;

import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 */
public class SparseSearchResultSet implements SolrSearchResultSet {
  private Iterable<Content> items;

  public SparseSearchResultSet(Iterable<Content> items) {
    this.items = items;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet#getResultSetIterator()
   */
  public Iterator<Result> getResultSetIterator() {
    final Iterator<Content> itemsIter = items.iterator();
    return new UnmodifiableIterator<Result>() {

      public boolean hasNext() {
        return itemsIter.hasNext();
      }

      public Result next() {
        Content c = itemsIter.next();
        Map<String, Collection<Object>> props = Maps.newHashMap();
        for (Entry<String, Object> prop : c.getProperties().entrySet()) {
          props.put(prop.getKey(), Lists.newArrayList(prop.getValue()));
        }
        return new GenericResult(c.getPath(), props);
      }
      
    };
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet#getSize()
   */
  public long getSize() {
    return 0;
  }

}
