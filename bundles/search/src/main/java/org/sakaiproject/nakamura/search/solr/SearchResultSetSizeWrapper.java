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

import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;

import java.util.Iterator;

/**
 * Wrap raw search results from query engines (i.e., Sparse or SQL) which do not provide
 * Solr-like support for a fuller count of matches beyond the range of the result set
 * itself.
 */
public class SearchResultSetSizeWrapper implements SolrSearchResultSet {
  private SolrSearchResultSet wrappedSearchResultSet;
  private long size;

  /**
   * @param wrappedSearchResultSet
   * @param size
   */
  public SearchResultSetSizeWrapper(SolrSearchResultSet wrappedSearchResultSet, long size) {
    this.wrappedSearchResultSet = wrappedSearchResultSet;
    this.size = size;
  }

  /**
   * @return
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet#getResultSetIterator()
   */
  public Iterator<Result> getResultSetIterator() {
    return wrappedSearchResultSet.getResultSetIterator();
  }

  public long getSize() {
    return size;
  }

}
