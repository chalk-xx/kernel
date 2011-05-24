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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.junit.runner.RunWith;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.junit.Before;
import org.junit.Test;

import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.Result;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.List;
import java.util.Collection;


@RunWith(MockitoJUnitRunner.class)
public class ContentLikeThisPropertyProviderTest {

  private ContentLikeThisPropertyProvider provider;

  @Mock
  private SlingHttpServletRequest request;

  @Mock
  private SolrSearchServiceFactory searchServiceFactory;

  @Mock
  private RequestParameter param;

  @Mock
  private SolrSearchResultSet rs;


  @Before
  public void setUp() throws Exception {
    provider = new ContentLikeThisPropertyProvider(searchServiceFactory);

    when(param.getString()).thenReturn("/p/12345");
    when(request.getRequestParameter("contentPath")).thenReturn(param);

    when(searchServiceFactory.getSearchResultSet(eq(request), any(Query.class)))
      .thenReturn(rs);
  }


  @Test
  public void testContentLikeThis () {
    Result result1 = mock(Result.class);
    Map<String, Collection<Object>> props1 = Maps.newHashMap();
    props1.put("id", Sets.newHashSet((Object) "prop1"));
    when(result1.getProperties()).thenReturn(props1);

    Result result2 = mock(Result.class);
    Map<String, Collection<Object>> props2 = Maps.newHashMap();
    props2.put("id", Sets.newHashSet((Object) "prop2"));
    when(result2.getProperties()).thenReturn(props2);

    Result result3 = mock(Result.class);
    Map<String, Collection<Object>> props3 = Maps.newHashMap();
    props3.put("id", Sets.newHashSet((Object) "prop3"));
    when(result3.getProperties()).thenReturn(props3);

    List<Result> results = Lists.newArrayList(result1, result2, result3);

    when(rs.getSize()).thenReturn((long)3);
    when(rs.getResultSetIterator()).thenReturn(results.iterator());

    Map<String, String> propertiesMap = Maps.newHashMap();

    provider.loadUserProperties(request, propertiesMap);

    assertEquals(" AND id:(\"prop1\"^400 OR \"prop2\"^300 OR \"prop3\"^200)",
                 propertiesMap.get("_contentQuery"));
  }

}
