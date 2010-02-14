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
package org.sakaiproject.nakamura.chat;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.core.query.lucene.SingleColumnQueryResult;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockProperty;
import org.apache.sling.commons.testing.jcr.MockPropertyIterator;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchUtil;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.VersionException;

@RunWith(MockitoJUnitRunner.class)
public class ChatMessageSearchResultProcessorTest {

  private ChatMessageSearchResultProcessor processor;
  @Mock
  private SlingHttpServletRequest request;
  private JSONWriter write;
  @Mock
  private Aggregator aggregator;
  @Mock
  private Row row;
  @Mock
  private ResourceResolver resolver;
  @Mock
  private Session session;
  @Mock
  private Node rowNode;
  private String testPath = "/chat/123";
  private Value path;
  private StringWriter stringWriter;

  @Before
  public void setUp() throws Exception {
    processor = new ChatMessageSearchResultProcessor();
    stringWriter = new StringWriter();
    write = new JSONWriter(stringWriter);
    path = new MockValue(testPath);
  }

  @Test
  public void aSingleChatResult() throws Exception {
    // given
    bareMinimumSetupOfSearchResultProcessorCollaborators();
    chatResultWithProperty(MessageConstants.PROP_SAKAI_READ, Boolean.FALSE);

    // when
    processor.writeNode(request, write, aggregator, row);

    // then
    verify(rowNode).setProperty(MessageConstants.PROP_SAKAI_READ, true);
    verify(rowNode).save();

    assertEquals(stringWriter.toString(),
        "{\"id\":\"-Ab12dc\",\"nationality\":\"USA\",\"state\":\"Texas\"}");

  }

  @Test
  public void simpleCaseOutputAsExpected() throws Exception {
    // given
    bareMinimumSetupOfSearchResultProcessorCollaborators();

    // when
    processor.writeNode(request, write, aggregator, row);

    // then
    assertEquals(stringWriter.toString(),
        "{\"id\":\"-Ab12dc\",\"nationality\":\"USA\",\"state\":\"Texas\"}");
  }

  @Test
  public void testSelectsSingleToProperty() throws Exception {
    // given
    bareMinimumSetupOfSearchResultProcessorCollaborators();
    chatResultWithProperty("sakai:to", "chat:zach,foo,bar,baz");
    chatResultCanReturnAJcrSession();
    sessionThatCanReturnThisItem("/_user/public/9d/e8/c4/48/zach/authprofile");

    // when
    processor.writeNode(request, write, aggregator, row);

    // then

    assertEquals(
        "{\"id\":\"-Ab12dc\",\"userTo\":{},\"nationality\":\"USA\",\"state\":\"Texas\"}",
        stringWriter.toString());
  }

  @Test
  public void testWeWillWriteTheUserFromProperty() throws Exception {
    // given
    bareMinimumSetupOfSearchResultProcessorCollaborators();
    chatResultWithProperty("sakai:from", "ieb");
    chatResultCanReturnAJcrSession();
    sessionThatCanReturnThisItem("/_user/public/ea/89/fa/4f/ieb/authprofile");

    // when
    processor.writeNode(request, write, aggregator, row);

    // then
    assertEquals(
        "{\"id\":\"-Ab12dc\",\"userFrom\":{},\"nationality\":\"USA\",\"state\":\"Texas\"}",
        stringWriter.toString());
  }

  @Test
  public void isSearchFunctionalityGeneric() throws Exception {
    // given
    Query testQuery = mock(Query.class);
    RowIterator resultRows = mock(RowIterator.class);
    SingleColumnQueryResult testResult = mock(SingleColumnQueryResult.class);
    when(testResult.getTotalSize()).thenReturn(0);
    when(testQuery.execute()).thenReturn(testResult);
    when(testResult.getRows()).thenReturn(resultRows);

    // when
    SearchResultSet controlResultSet = SearchUtil.getSearchResultSet(request, testQuery);
    SearchResultSet testResultSet = processor.getSearchResultSet(request, testQuery);

    // then
    // We could use a proper equals() method for AbstractSearchResultSet here
    assertEquals(testResultSet.getSize(), controlResultSet.getSize());
  }

  @Test
  public void confirmThatWeAddToOurAggregator() throws Exception {
    // given
    bareMinimumSetupOfSearchResultProcessorCollaborators();
    aggregator = mock(Aggregator.class);

    // when
    processor.writeNode(request, write, aggregator, row);

    // then
    verify(aggregator).add(rowNode);
  }

  private void bareMinimumSetupOfSearchResultProcessorCollaborators()
      throws RepositoryException {
    requestThatReturnsAResolver();
    resolverThatReturnsASession();
    sessionThatCanReturnChatResultNode();
    aggregator = null;
    rowWithAJcrPath();
    chatResultWithId("-Ab12dc");
    chatResultWithSomeCannedProperties();
  }

  private void requestThatReturnsAResolver() {
    when(request.getResourceResolver()).thenReturn(resolver);
  }

  private void resolverThatReturnsASession() {
    when(resolver.adaptTo(Session.class)).thenReturn(session);
  }

  private void sessionThatCanReturnChatResultNode() throws PathNotFoundException,
      RepositoryException {
    when(session.getItem(testPath)).thenReturn(rowNode);
  }

  private void rowWithAJcrPath() throws ItemNotFoundException, RepositoryException {
    when(row.getValue("jcr:path")).thenReturn(path);
  }

  private void chatResultWithId(String id) throws RepositoryException {
    when(rowNode.getName()).thenReturn(id);
  }

  private void chatResultCanReturnAJcrSession() throws RepositoryException {
    when(rowNode.getSession()).thenReturn(session);
  }

  private void chatResultWithProperty(String key, String value)
      throws PathNotFoundException, RepositoryException {
    Property newProperty = new MockProperty(key);
    newProperty.setValue(value);
    when(rowNode.hasProperty(key)).thenReturn(Boolean.TRUE);
    when(rowNode.getProperty(key)).thenReturn(newProperty);
  }

  private void chatResultWithProperty(String key, Boolean value)
      throws PathNotFoundException, RepositoryException {
    Property newProperty = new MockProperty(key);
    newProperty.setValue(value);
    when(rowNode.hasProperty(key)).thenReturn(Boolean.TRUE);
    when(rowNode.getProperty(key)).thenReturn(newProperty);
  }

  private void chatResultWithSomeCannedProperties() throws RepositoryException {
    when(rowNode.getProperties()).thenReturn(
        new MockPropertyIterator(mockPropsIterator()));
  }

  private Iterator<Property> mockPropsIterator() throws ValueFormatException,
      VersionException, LockException, ConstraintViolationException, RepositoryException {
    Collection<Property> props = new ArrayList<Property>();
    Property nationProperty = new MockProperty("nationality");
    nationProperty.setValue("USA");
    Property homeStateProperty = new MockProperty("state");
    homeStateProperty.setValue("Texas");
    props.add(nationProperty);
    props.add(homeStateProperty);
    return props.iterator();
  }

  private void sessionThatCanReturnThisItem(String jcrPath) throws PathNotFoundException,
      RepositoryException {
    when(session.getItem(jcrPath)).thenReturn(new MockNode(jcrPath));

  }

}
