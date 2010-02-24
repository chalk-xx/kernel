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
package org.sakaiproject.nakamura.files.search;

import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

import org.apache.jackrabbit.core.query.lucene.SingleColumnQueryResult;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.MockName;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.site.SiteService;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 *
 */
public class FileSearchBatchResultProcessorTest {

  private FileSearchBatchResultProcessor processor;
  private SiteService siteService;

  @Before
  public void setUp() {
    processor = new FileSearchBatchResultProcessor();
    siteService = mock(SiteService.class);

    processor.siteService = siteService;
  }

  @Test
  public void testGetResultSet() throws SearchException, RepositoryException {
/*
    Session session = mock(Session.class);
    Row fileProperA = createRow(session, "/path/to/fileA");

    Query q = mock(Query.class);
    SingleColumnQueryResult result = mock(SingleColumnQueryResult.class);
    when(result.getTotalSize()).thenReturn(5);
    when(q.execute()).thenReturn(result);

    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);

    SearchResultSet set = processor.getSearchResultSet(request, q);
*/
  }

  /**
   * @param string
   * @return
   * @throws RepositoryException 
   * @throws ItemNotFoundException 
   */
  private Row createRow(Session session, String path) throws RepositoryException {
    Row row = mock(Row.class);
    Value pathValue = new MockValue(path);
    when(row.getValue("jcr:path")).thenReturn(pathValue);
    
    Node node = new MockNode(path);
    
    when(session.getItem(path)).thenReturn(node);
    
    return row;
  }

}
