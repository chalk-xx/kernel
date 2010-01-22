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
package org.sakaiproject.kernel.activity.routing;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.api.activity.ActivityConstants;
import org.sakaiproject.kernel.api.activity.ActivityRoute;
import org.sakaiproject.kernel.api.site.SiteService;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 *
 */
public class SiteActivityRouterTest extends AbstractActivityRouterTest {

  private SiteService siteService;

  @Before
  public void setUp() throws RepositoryException {
    playNode = false;
    super.setUp();
  }

  @Test
  public void testAddingSite() throws RepositoryException {

    siteService = createMock(SiteService.class);

    path = "/sites/mysite/foo";

    Node siteNode = createMock(Node.class);
    expect(siteNode.getPath()).andReturn("/sites/mysite").anyTimes();
    expect(siteService.isSite(siteNode)).andReturn(true).anyTimes();

    expect(siteService.isSite(activity)).andReturn(false).anyTimes();
    expect(activity.getParent()).andReturn(siteNode).anyTimes();
    replay(siteNode, activity, siteService);

    SiteActivityRouter router = new SiteActivityRouter();
    router.siteService = siteService;
    router.route(activity, routes);

    Assert.assertEquals(1, routes.size());
    ActivityRoute route = routes.get(0);
    Assert.assertEquals(
        "/sites/mysite/" + ActivityConstants.ACTIVITY_FEED_NAME, route
            .getDestination());
  }

  @Test
  public void testAddingNonSite() throws RepositoryException {
    siteService = createMock(SiteService.class);

    path = "/foo";

    Node rootNode = createMock(Node.class);
    expect(rootNode.getPath()).andReturn("/").anyTimes();
    expect(siteService.isSite(rootNode)).andReturn(false).anyTimes();

    Node node = createMock(Node.class);
    expect(node.getPath()).andReturn(path).anyTimes();
    expect(node.getParent()).andReturn(rootNode).anyTimes();
    expect(siteService.isSite(node)).andReturn(false).anyTimes();

    replay(rootNode, node, siteService);

    SiteActivityRouter router = new SiteActivityRouter();
    router.siteService = siteService;
    router.route(node, routes);

    Assert.assertEquals(0, routes.size());
  }

}
