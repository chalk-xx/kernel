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
package org.sakaiproject.nakamura.discussion.searchresults;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONObject;
import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.sakaiproject.nakamura.api.discussion.DiscussionConstants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

/**
 *
 */
public class DiscussionThreadedProcessorTest extends AbstractEasyMockTest {

  private DiscussionThreadedSearchBatchResultProcessor processor;
  private PresenceService presenceService;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest#setUp()
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    processor = new DiscussionThreadedSearchBatchResultProcessor();
    processor.searchServiceFactory = createNiceMock(SolrSearchServiceFactory.class);
    presenceService = createNiceMock(PresenceService.class);
    processor.presenceService = presenceService;
  }

  @Test
  public void testProcess() throws Exception {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);

    Session session = createMock(Session.class);

    ProfileService profileService = createNiceMock(ProfileService.class);
    processor.profileService = profileService;

    AccessControlManager accessControlManager = createNiceMock(AccessControlManager.class);
    expect(session.getAccessControlManager()).andReturn(accessControlManager).anyTimes();
    @SuppressWarnings("unused")
    User adminUser = new User(ImmutableMap.of(User.ID_FIELD, (Object) "admin"));
    @SuppressWarnings("unused")
    User anonUser = new User(ImmutableMap.of(User.ID_FIELD, (Object) "anonymous"));
    expect(profileService.getCompactProfileMap((Authorizable)EasyMock.anyObject(), (javax.jcr.Session)EasyMock.anyObject())).andReturn(
        ValueMap.EMPTY).anyTimes();
    AuthorizableManager authMgr = createNiceMock(AuthorizableManager.class);
    expect(session.getAuthorizableManager()).andReturn(authMgr).anyTimes();
    ResourceResolver resolver = createNiceMock(ResourceResolver.class);
    expect(resolver.adaptTo(Session.class)).andReturn(session).anyTimes();
    expect(request.getResourceResolver()).andReturn(resolver).anyTimes();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Writer w = new PrintWriter(baos);
    ExtendedJSONWriter writer = new ExtendedJSONWriter(w);

    // 4 nodes
    // a
    // - b
    // - d
    // - c

    Content contentA = new Content("/msg/a", null);
    contentA.setProperty(MessageConstants.PROP_SAKAI_ID, "a");
    contentA.setProperty(MessageConstants.PROP_SAKAI_FROM, "admin");

    Content contentB = new Content("/msg/b", null);
    contentB.setProperty(MessageConstants.PROP_SAKAI_ID, "b");
    contentB.setProperty(DiscussionConstants.PROP_REPLY_ON, "a");
    contentB.setProperty(MessageConstants.PROP_SAKAI_FROM, "anonymous");
    contentB.setProperty(DiscussionConstants.PROP_EDITEDBY, "admin");

    Content contentC = new Content("/msg/c", null);
    contentC.setProperty(MessageConstants.PROP_SAKAI_ID, "c");
    contentC.setProperty(DiscussionConstants.PROP_REPLY_ON, "a");
    contentC.setProperty(MessageConstants.PROP_SAKAI_FROM, "admin");

    Content contentD = new Content("/msg/d", null);
    contentD.setProperty(MessageConstants.PROP_SAKAI_ID, "d");
    contentD.setProperty(DiscussionConstants.PROP_REPLY_ON, "b");
    contentD.setProperty(MessageConstants.PROP_SAKAI_FROM, "admin");

    List<Result> results = Lists.newArrayList();
    results.add(mockResult(contentA));
    results.add(mockResult(contentB));
    results.add(mockResult(contentC));
    results.add(mockResult(contentD));

    expect(resolver.getResource("/msg/a")).andReturn(mockResource(contentA));
    expect(resolver.getResource("/msg/b")).andReturn(mockResource(contentB));
    expect(resolver.getResource("/msg/c")).andReturn(mockResource(contentC));
    expect(resolver.getResource("/msg/d")).andReturn(mockResource(contentD));

    replay();
    processor.writeResults(request, writer, results.iterator());
    w.flush();

    String s = baos.toString("UTF-8");

    JSONObject json = new JSONObject(s);
    assertEquals(json.getJSONArray("replies").length(), 2);
    assertEquals(json.getJSONArray("replies").getJSONObject(0).getJSONArray("replies")
        .length(), 1);
    assertEquals("a", json.getJSONObject("post").get("sakai:id"));
    assertEquals("b", json.getJSONArray("replies").getJSONObject(0).getJSONObject("post")
        .get("sakai:id"));
  }

  private Result mockResult(Content content) {
    Result r = createNiceMock(Result.class);
    expect(r.getPath()).andReturn(content.getPath());
    return r;
  }

  private Resource mockResource(Content content) {
    Resource r = createNiceMock(Resource.class);
    expect(r.adaptTo(Content.class)).andReturn(content);
    return r;
  }
}
