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
package org.apache.sling.jcr.jackrabbit.server.index;

import org.apache.jackrabbit.core.query.lucene.QueryResultImpl;

import javax.jcr.query.QueryResult;

public class QueryHitsExtractor {

  private QueryResultImpl result;

  public QueryHitsExtractor(QueryResult result) {
    this.result = getQueryResult(result);
    if (result == null) {
      throw new IllegalArgumentException(
          "Failed to get the QueryHits from this result.");
    }
  }

  private QueryResultImpl getQueryResult(QueryResult result) {
    return ((QueryResultImpl) result);
  }

  /**
   * @return Get the hit count for a query as it is returned by Lucene. Keep in mind that
   *         this will be bigger than the total amount of nodes that a user has access to
   *         in JCR. If Lucene returns -1, this will return <code>Integer.MAX_VALUE</code>
   */
  public int getHits() {
    int hits = result.getTotalSize();
    if (hits == -1) {
      return Integer.MAX_VALUE;
    }
    return hits;

  }

}
