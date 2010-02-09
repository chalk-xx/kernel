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
package org.sakaiproject.nakamura.docproxy.disk;

import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_LOCATION;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import junit.framework.Assert;

import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.docproxy.DocProxyConstants;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata;
import org.sakaiproject.nakamura.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 *
 */
public class DiskProcessorTest {

  public static final String TEST_STRING = "K2 docProxy test resource";
  private DiskProcessor diskProcessor;
  private String currPath;
  private Node proxyNode;

  @Before
  public void setUp() throws Exception {
    // Start with a new processor.
    diskProcessor = new DiskProcessor();

    String readmePath = getClass().getClassLoader().getResource("README").getPath();
    currPath = readmePath.substring(0, readmePath.lastIndexOf("/"));

    proxyNode = new MockNode("/docproxy/disk");
    proxyNode.setProperty(REPOSITORY_LOCATION, currPath);
  }

  @Test
  public void testGet() throws RepositoryException, UnsupportedEncodingException,
      IOException, DocProxyException {
    // Create the proxy Node.
    Node proxyNode = createMock(Node.class);
    Property locationProp = createMock(Property.class);
    expect(locationProp.getString()).andReturn(currPath).anyTimes();
    expect(proxyNode.getProperty(DocProxyConstants.REPOSITORY_LOCATION)).andReturn(
        locationProp);
    replay(locationProp, proxyNode);
    ExternalDocumentResult result = diskProcessor.getDocument(proxyNode, "README");
    InputStream in = result.getDocumentInputStream(0);
    String content = IOUtils.readFully(in, "UTF-8");
    Assert.assertEquals(TEST_STRING, content);

    // Properties from .json
    Map<String, Object> props = result.getProperties();
    Assert.assertEquals("bar", props.get("foo"));
    Assert.assertEquals(123, props.get("num"));
  }

  @Test
  public void testUpdateDocument() throws DocProxyException, PathNotFoundException,
      RepositoryException, IOException {

    // Create a new file.
    String path = "README-copy";
    Node newNode = createFile(diskProcessor, proxyNode, path, TEST_STRING);

    // Get the file
    ExternalDocumentResult result = diskProcessor.getDocument(newNode, path);
    InputStream in = result.getDocumentInputStream(0);

    // Read content
    String content = IOUtils.readFully(in, "UTF-8");
    Assert.assertEquals(content, TEST_STRING);
  }

  @Test
  public void testSearch() throws PathNotFoundException, UnsupportedEncodingException,
      RepositoryException, DocProxyException {
    // Mock proxyNode
    Node proxyNode = createMock(Node.class);
    Property locationProp = createMock(Property.class);
    expect(locationProp.getString()).andReturn(currPath).anyTimes();
    expect(proxyNode.getProperty(DocProxyConstants.REPOSITORY_LOCATION)).andReturn(
        locationProp).anyTimes();
    replay(locationProp, proxyNode);

    // Create a couple of files.
    createFile(diskProcessor, proxyNode, "test-disk-search-1", "alfa");
    createFile(diskProcessor, proxyNode, "test-disk-search-2", "beta");
    createFile(diskProcessor, proxyNode, "test-disk-search-3", "charlie");

    // The disk search only matches filenames starting with a term..
    Map<String, Object> searchProperties = new HashMap<String, Object>();
    searchProperties.put("starts-with", "test-disk-search-");

    // Perform actual search.
    Iterator<ExternalDocumentResult> results = diskProcessor.search(proxyNode,
        searchProperties);

    // Quickly loop over the results to get a count
    int size = 0;
    while (results.hasNext()) {
      results.next();
      size++;
    }

    Assert.assertEquals(3, size);
  }

  @Test
  public void testContentType() throws PathNotFoundException,
      UnsupportedEncodingException, RepositoryException, DocProxyException {
    Node textNode = createFile(diskProcessor, proxyNode, "test-contentType.txt",
        TEST_STRING);
    ExternalDocumentResultMetadata meta = diskProcessor.getDocumentMetadata(textNode,
        "test-contentType.txt");
    Assert.assertEquals("text/plain", meta.getContentType());

    Node htmlNode = createFile(diskProcessor, proxyNode, "test-contentType.html",
        "<html><head><title>foo</title></head></html>");
    ExternalDocumentResultMetadata htmlMeta = diskProcessor.getDocumentMetadata(htmlNode,
        "test-contentType.html");
    Assert.assertEquals("text/html", htmlMeta.getContentType());

  }

  public static Node createFile(DiskProcessor processor, Node proxyNode, String path,
      String content) throws PathNotFoundException, RepositoryException,
      UnsupportedEncodingException, DocProxyException {
    ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
    return createFile(processor, proxyNode, path, stream);
  }

  public static Node createFile(DiskProcessor processor, Node proxyNode, String path,
      InputStream documentStream) throws PathNotFoundException, RepositoryException,
      DocProxyException {
    processor.updateDocument(proxyNode, path, null, documentStream, -1);

    return proxyNode;

  }

}
