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
package org.sakaiproject.kernel.docproxy.disk;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.kernel.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.kernel.api.docproxy.ExternalDocumentResultMetadata;
import org.sakaiproject.kernel.util.IOUtils;
import org.sakaiproject.nakamura.api.docproxy.DocProxyConstants;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.docproxy.disk.DiskProcessor;

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

  private Node readmeNode;
  public static final String TEST_STRING = "K2 docProxy test resource";
  private DiskProcessor diskProcessor;

  @Before
  public void setUp() throws Exception {
    // Start with a new processor.
    diskProcessor = new DiskProcessor();

    // Create the default readme Node.
    readmeNode = createMock(Node.class);
    Property idProp = createMock(Property.class);
    expect(idProp.getString()).andReturn("disk:README").anyTimes();
    expect(readmeNode.getProperty(DocProxyConstants.EXTERNAL_ID)).andReturn(idProp)
        .anyTimes();

    replay(idProp, readmeNode);
  }

  @Test
  public void testGet() throws RepositoryException, UnsupportedEncodingException,
      IOException, DocProxyException {
    ExternalDocumentResult result = diskProcessor.getDocument(readmeNode, "");
    InputStream in = result.getDocumentInputStream(0);
    String content = IOUtils.readFully(in, "UTF-8");
    Assert.assertEquals(content, TEST_STRING);
  }

  @Test
  public void testUpdateDocument() throws DocProxyException, PathNotFoundException,
      RepositoryException, IOException {

    // Create a new file.
    String path = "README-copy";
    Node newNode = createFile(path, TEST_STRING);

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
    // Create a couple of files.
    createFile("test-search-1", "alfa");
    createFile("test-search-2", "beta");
    createFile("test-search-3", "charlie");

    // The disk search only matches filenames starting with a term..
    Map<String, Object> searchProperties = new HashMap<String, Object>();
    searchProperties.put("name", "test-search-");

    // Perform actual search.
    Iterator<ExternalDocumentResult> results = diskProcessor.search(searchProperties);

    // Quickly loop over the results to get a count
    int size = 0;
    while (results.hasNext()) {
      ExternalDocumentResult result = results.next();
      size++;
    }

    Assert.assertEquals(3, size);
  }

  @Test
  public void testContentType() throws PathNotFoundException,
      UnsupportedEncodingException, RepositoryException, DocProxyException {
    Node textNode = createFile("test-contentType.txt", TEST_STRING);
    ExternalDocumentResultMetadata meta = diskProcessor.getDocumentMetadata(textNode, "");
    Assert.assertEquals(meta.getContentType(), "text/plain");

    Node htmlNode = createFile("test-contentType.html",
        "<html><head><title>foo</title></head></html>");
    ExternalDocumentResultMetadata htmlMeta = diskProcessor.getDocumentMetadata(htmlNode,
        "");
    Assert.assertEquals(htmlMeta.getContentType(), "text/html");

  }

  private Node createFile(String path, String content) throws PathNotFoundException,
      RepositoryException, UnsupportedEncodingException, DocProxyException {
    ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
    return createFile(path, stream);
  }

  private Node createFile(String path, InputStream documentStream)
      throws PathNotFoundException, RepositoryException, DocProxyException {
    Node newNode = createMock(Node.class);
    Property idProp = createMock(Property.class);
    expect(idProp.getString()).andReturn("disk:" + path).anyTimes();
    expect(newNode.getProperty(DocProxyConstants.EXTERNAL_ID)).andReturn(idProp)
        .anyTimes();
    replay(idProp, newNode);
    diskProcessor.updateDocument(newNode, "", null, documentStream, 0);

    return newNode;

  }

}
