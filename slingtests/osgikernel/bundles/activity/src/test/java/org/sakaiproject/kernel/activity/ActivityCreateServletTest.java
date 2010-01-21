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
package org.sakaiproject.kernel.activity;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.fail;
import static org.sakaiproject.kernel.api.activity.ActivityConstants.ACTIVITY_STORE_NAME;
import static org.sakaiproject.kernel.api.activity.ActivityConstants.PARAM_APPLICATION_ID;
import static org.sakaiproject.kernel.api.activity.ActivityConstants.PARAM_TEMPLATE_ID;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Test;
import org.sakaiproject.kernel.api.activity.ActivityConstants;
import org.sakaiproject.kernel.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 *
 */
public class ActivityCreateServletTest extends AbstractEasyMockTest {
  @Test
  public void testDummyTest() {
  }

  @Test
  public void testRequestPathInfo() {
    ActivityCreateServlet servlet = new ActivityCreateServlet();
    final String path = "/foo/bar/activity/2010/01/21/10/111223235453";

    final RequestPathInfo requestPathInfo = new RequestPathInfo() {

      public String getSuffix() {
        return "suffix";
      }

      public String[] getSelectors() {
        String[] selectors = new String[] { "foo", "activity", "bar" };
        return selectors;
      }

      public String getSelectorString() {
        return ".foo.activity.bar";
      }

      public String getResourcePath() {
        return path;
      }

      public String getExtension() {
        return "html";
      }
    };

    RequestPathInfo pathInfo = servlet.createRequestPathInfo(requestPathInfo, path);
    Assert.assertEquals(pathInfo.getSelectorString(), ".foo.bar");
    Assert.assertEquals(pathInfo.getExtension(), "html");
    Assert.assertEquals(pathInfo.getResourcePath(), requestPathInfo.getResourcePath());
    Assert.assertEquals(pathInfo.getSuffix(), "suffix");
    Assert.assertEquals(pathInfo.getSelectors()[0], "foo");
    Assert.assertEquals(pathInfo.getSelectors()[1], "bar");
  }

  // FIXME, Test Broken @Test
  public void testRequiredParameters() {
    final String fakeNodePath = "/path/to/parent/node";
    final String activityStorePath = fakeNodePath + "/" + ACTIVITY_STORE_NAME;
    // final Pattern pattern = Pattern.compile("^" + activityStorePath
    // + "(/.{2})?(/.{2})?(/.{2})?(/.{2})?(/.+_.+_.+_.+_.+)?");
    // final Map<String, Node> activityStoreHashPaths = new HashMap<String, Node>();
    ActivityCreateServlet acs = new ActivityCreateServlet();

    SlingHttpServletRequest request = createMock("request",
        SlingHttpServletRequest.class);
    this.addStringRequestParameter(request, PARAM_APPLICATION_ID, "sakai.chat");
    this.addStringRequestParameter(request, PARAM_TEMPLATE_ID, "1234");
    expect(request.getRemoteUser()).andReturn("lance");

    SlingHttpServletResponse response = createMock("response",
        SlingHttpServletResponse.class);

    Resource resource = createMock("slingResource", Resource.class);
    expect(request.getResource()).andReturn(resource);
    Node slingNode = createMock("slingNode", Node.class);
    expect(resource.adaptTo(Node.class)).andReturn(slingNode);
    Session session = createMock(Session.class);

    try {
      expect(slingNode.getSession()).andReturn(session);
      expect(slingNode.hasNode(ACTIVITY_STORE_NAME)).andReturn(false)
          .anyTimes(); // assume does not exist
      expect(slingNode.getPath()).andReturn(fakeNodePath);
      expect(session.itemExists(activityStorePath)).andReturn(false);
      expect(session.itemExists(fakeNodePath)).andReturn(true);
      expect(session.getItem(fakeNodePath)).andReturn(slingNode);
      Node activityStoreNode = createMock("activityStoreNode", Node.class);
      expect(session.getItem(activityStorePath)).andReturn(activityStoreNode);
      expect(slingNode.addNode(ACTIVITY_STORE_NAME)).andReturn(
          activityStoreNode);
      Property sakaiActivityStoreSlingResourceType = createMock(
          "sakaiActivityStoreSlingResourceType", Property.class);
      expect(
          activityStoreNode.setProperty(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
              ActivityConstants.ACTIVITY_STORE_RESOURCE_TYPE)).andReturn(
          sakaiActivityStoreSlingResourceType);
      expect(activityStoreNode.getPath()).andReturn(activityStorePath);
      expect(session.itemExists(isA(String.class))).andAnswer(
          new IAnswer<Boolean>() {
            public Boolean answer() throws Throwable {
              String path = getCurrentArguments()[0].toString();
              System.out.println("session.itemExists: " + path);
              if (activityStorePath.equals(path)) {
                return true;
              }
              return false;
            }
          }).anyTimes();

      expect(activityStoreNode.hasNode(isA(String.class))).andAnswer(
          new IAnswer<Boolean>() {
            public Boolean answer() throws Throwable {
              String path = getCurrentArguments()[0].toString();
              System.out.println("activityStoreNode.hasNode: " + path);
              return false;
            }
          }).anyTimes();

      expect(activityStoreNode.addNode(isA(String.class))).andAnswer(
          new IAnswer<Node>() {
            public Node answer() throws Throwable {
              String path = getCurrentArguments()[0].toString();
              System.out.println("activityStoreNode.addNode: " + path);
              Node node = createMock("node " + path, Node.class);
              expect(node.hasNode(isA(String.class))).andReturn(false);
              // expect();
              return node;
            }
          }).anyTimes();

      replay();

      acs.doPost(request, response);
    } catch (RepositoryException e) {
      fail();
    } catch (ServletException e) {
      fail();
    } catch (IOException e) {
      fail();
    }
    verify();
  }

}
