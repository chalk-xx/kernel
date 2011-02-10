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
package org.sakaiproject.nakamura.activity.search;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.io.JSONWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.search.Aggregator;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

@RunWith(MockitoJUnitRunner.class)
public class MostActiveContentSearchBatchResultProcessorTest {

  private SlingHttpServletRequest request = mock(SlingHttpServletRequest.class,
      RETURNS_DEEP_STUBS);

  private StringWriter stringWriter;
  private JSONWriter jsonWriter;

  @Mock
  private Aggregator aggregator;

  @Mock
  private RowIterator rowIterator;

  @Mock
  private ResourceResolver resourceResolver;

  @Mock
  private Session session;

  private Map<String, Node> activityNodes;
  private Map<String, Node> resourceNodes;

  @Before
  public void setup() throws Exception {
    stringWriter = new StringWriter();
    jsonWriter = new JSONWriter(stringWriter);
    activityNodes = new HashMap<String, Node>();
    resourceNodes = new HashMap<String, Node>();
    when(request.getResourceResolver()).thenReturn(resourceResolver);
    when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
  }

  @Test
  public void dummyTest() {
    
  }

  // TODO: reenable and get working @Test
  public void testWritingStopsAfterTimeWindow() throws Exception {
    Calendar today = Calendar.getInstance();
    Node firstNode = prepareAnActivityNode("nodeId#1", today);
    Node firstResource = prepareAResourceNode("my-term-paper.pdf");
    Row firstRow = prepareAnActivityRow("/firstNode");
    activityNodes.put("/firstNode", firstNode);
    resourceNodes.put("nodeId#1", firstResource);

    Calendar yesterday = Calendar.getInstance();
    yesterday.add(Calendar.DAY_OF_MONTH, -1);
    Node secondNode = prepareAnActivityNode("nodeId#2", yesterday);
    Node secondResourceNode = prepareAResourceNode("happy-clam.png");
    Row secondRow = prepareAnActivityRow("/secondNode");
    activityNodes.put("/secondNode", secondNode);
    resourceNodes.put("nodeId#2", secondResourceNode);

    Calendar lastYear = Calendar.getInstance();
    lastYear.add(Calendar.YEAR, -1);
    Node oldNode = prepareAnActivityNode("oldNode", lastYear);
    Row oldRow = prepareAnActivityRow("/oldNode");
    activityNodes.put("/oldNode", oldNode);

    when(rowIterator.hasNext()).thenReturn(true, true, true, false);
    when(rowIterator.nextRow()).thenReturn(firstRow, secondRow, oldRow);

    loadSession(activityNodes, resourceNodes);

    MostActiveContentSearchBatchResultProcessor m = new MostActiveContentSearchBatchResultProcessor();
    m.writeNodes(request, jsonWriter, aggregator, rowIterator);

    assertEquals(
        "{\"content\":[{\"id\":\"nodeId#1\",\"name\":\"my-term-paper.pdf\",\"count\":1},{\"id\":\"nodeId#2\",\"name\":\"happy-clam.png\",\"count\":1}]}",
        stringWriter.toString());

  }

  // TODO: reenable and get working @Test
  public void testExceptionDoesNotStopTheFeed() throws Exception {
    Calendar today = Calendar.getInstance();

    Node badNode = prepareAnActivityNode("badNode", today);
    when(badNode.getProperty(anyString())).thenThrow(
        new RepositoryException("I am a bad bad node!!"));

    activityNodes.put("/goodNode", prepareAnActivityNode("goodNode#1", today));
    activityNodes.put("/badNode", badNode);
    resourceNodes.put("goodNode#1", prepareAResourceNode("a-good-joke.txt"));
    loadSession(activityNodes, resourceNodes);

    Row goodRow = prepareAnActivityRow("/goodNode");
    Row badRow = prepareAnActivityRow("/badNode");
    when(rowIterator.hasNext()).thenReturn(true, true, false);
    when(rowIterator.nextRow()).thenReturn(goodRow, badRow);

    MostActiveContentSearchBatchResultProcessor m = new MostActiveContentSearchBatchResultProcessor();
    m.writeNodes(request, jsonWriter, aggregator, rowIterator);

    assertEquals(
        "{\"content\":[{\"id\":\"goodNode#1\",\"name\":\"a-good-joke.txt\",\"count\":1}]}",
        stringWriter.toString());
  }

  // TODO: reenable and get working @Test
  public void testCanCountProperly() throws Exception {
    Node myPopularResource = prepareAResourceNode("fastest-cars.doc");
    resourceNodes.put("node#1", myPopularResource);

    Calendar today = Calendar.getInstance();
    activityNodes.put("/node1", prepareAnActivityNode("node#1", today));

    Calendar yesterday = Calendar.getInstance();
    yesterday.add(Calendar.DAY_OF_MONTH, -1);
    activityNodes.put("/node2", prepareAnActivityNode("node#1", yesterday));

    Row yesterdayRow = prepareAnActivityRow("/node2");
    Row todayRow = prepareAnActivityRow("/node1");
    when(rowIterator.hasNext()).thenReturn(true, true, false);
    when(rowIterator.nextRow()).thenReturn(yesterdayRow, todayRow);

    loadSession(activityNodes, resourceNodes);

    MostActiveContentSearchBatchResultProcessor m = new MostActiveContentSearchBatchResultProcessor();
    m.writeNodes(request, jsonWriter, aggregator, rowIterator);

    assertEquals(
        "{\"content\":[{\"id\":\"node#1\",\"name\":\"fastest-cars.doc\",\"count\":2}]}",
        stringWriter.toString());

  }

  private void loadSession(Map<String, Node> activityNodes,
      Map<String, Node> resourceNodes) throws RepositoryException,
      NoSuchAlgorithmException, UnsupportedEncodingException {
    for (String path : activityNodes.keySet()) {
      when(session.getItem(path)).thenReturn(activityNodes.get(path));
    }

      // TODO: make resolution of Pool Content work
      for (String id : resourceNodes.keySet()) {
      when(session.getNode(id)).thenReturn(
          resourceNodes.get(id));
      }
  }

  private Row prepareAnActivityRow(String path) throws RepositoryException {
    Row activityRow = mock(Row.class);
    Value pathValue = mock(Value.class);
    when(pathValue.getString()).thenReturn(path);
    when(activityRow.getValue("jcr:path")).thenReturn(pathValue);
    return activityRow;
  }

  private Node prepareAnActivityNode(String id, Calendar timestamp)
      throws RepositoryException {
    Node activityNode = mock(Node.class, RETURNS_DEEP_STUBS);
    when(activityNode.hasProperty("timestamp")).thenReturn(true);
    when(activityNode.getProperty("timestamp").getDate()).thenReturn(timestamp);
    when(activityNode.getProperty("resourceId").getString()).thenReturn(id);
    return activityNode;
  }

  private Node prepareAResourceNode(String resourceName) throws RepositoryException {
    Node resourceNode = mock(Node.class, RETURNS_DEEP_STUBS);
    when(resourceNode.getProperty("sakai:pooled-content-file-name").getString())
        .thenReturn(resourceName);
    return resourceNode;
  }
}
