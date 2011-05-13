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
package org.sakaiproject.nakamura.connections.search;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_TITLE_PROPERTY;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MyRelatedGroupsPropertyProviderTest {
  @Mock
  private SolrSearchServiceFactory searchServiceFactory;
  @Mock
  private Repository repo;
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private Session session;
  @Mock
  private AuthorizableManager authMgr;
  @Mock
  private Authorizable auth1;
  @Mock
  private SolrSearchResultSet rs;

  private MyRelatedGroupsPropertyProvider provider;

  @Before
  public void setUp() throws Exception {
    provider = new MyRelatedGroupsPropertyProvider(searchServiceFactory);
    when(request.getRemoteUser()).thenReturn("user1");

    when(repo.loginAdministrative()).thenReturn(session);
    when(session.getAuthorizableManager()).thenReturn(authMgr);
    when(authMgr.findAuthorizable("user1")).thenReturn(auth1);

    Group group1 = mock(Group.class);
    when(group1.getId()).thenReturn("group1");
    when(group1.getProperty(GROUP_TITLE_PROPERTY)).thenReturn("Group 1 Test");
    when(group1.getProperty("sakai:tag-uuid")).thenReturn(new String[] { "123-456" });

    when(auth1.memberOf(authMgr)).thenReturn(Sets.newHashSet(group1).iterator());

    when(searchServiceFactory.getSearchResultSet(eq(request), any(Query.class)))
        .thenReturn(rs);
  }


  @Test
  public void noGroups() {
    reset(auth1);
    Collection<Group> noGroups = Collections.emptyList();
    when(auth1.memberOf(authMgr)).thenReturn(noGroups.iterator());

    List<Result> results = Lists.newArrayList();
    when(rs.getResultSetIterator()).thenReturn(results.iterator());

    Map<String, String> propertiesMap = Maps.newHashMap();

    provider.loadUserProperties(request, propertiesMap);

    assertEquals("", propertiesMap.get("_groupQuery"));
  }


  @Test
  public void oneGroup() {
    Result result1 = mock(Result.class);
    Map<String, Collection<Object>> props1 = Maps.newHashMap();
    props1.put("id", Sets.newHashSet((Object) "prop1-id"));

    List<Result> results = Lists.newArrayList(result1);
    when(result1.getProperties()).thenReturn(props1);
    when(rs.getResultSetIterator()).thenReturn(results.iterator());

    Map<String, String> propertiesMap = Maps.newHashMap();

    provider.loadUserProperties(request, propertiesMap);

    assertEquals(" AND id:(\"prop1\\-id\"^1) AND -readers:\"user1\"",
                 propertiesMap.get("_groupQuery"));
  }


  @Test
  public void threeGroups() {

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

    System.out.println("GOT: " + propertiesMap.get("_groupQuery"));

    assertEquals(" AND id:(\"prop1\"^4 OR \"prop2\"^3 OR \"prop3\"^2) AND -readers:\"user1\"",
                 propertiesMap.get("_groupQuery"));
  }
}
