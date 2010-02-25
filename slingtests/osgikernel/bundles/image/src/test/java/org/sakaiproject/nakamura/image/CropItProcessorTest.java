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
package org.sakaiproject.nakamura.image;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.jcr.JCRConstants;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

/**
 *
 */
public class CropItProcessorTest extends AbstractEasyMockTest {

  private Session session;
  private String img = "/foo/people.png";
  private int x = 0;
  private int y = 0;
  private int width = 100;
  private int height = 100;
  private List<Dimension> dimensions;
  private String save = "/save/in/here/";
  private Node node;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    session = createMock(Session.class);
    node = createMock(Node.class);
    dimensions = new ArrayList<Dimension>();
    Dimension d = new Dimension();
    d.setSize(50, 50);
    dimensions.add(d);
    expect(session.getItem(img)).andReturn(node);
  }

  @Test
  public void testGetMimeTypeForNodeProp() throws PathNotFoundException,
      ValueFormatException, RepositoryException {
    Node node = createMock(Node.class);
    createMimeType(node, "image/jpg");
    replay();
    String result = CropItProcessor.getMimeTypeForNode(node, "test.jpg");
    assertEquals("image/jpg", result);
  }

  /**
   * @param node
   * @param string
   * @throws RepositoryException
   * @throws ValueFormatException
   */
  private void createMimeType(Node node, String mimeTypeValue)
      throws ValueFormatException, RepositoryException {
    Property mimeType = createMock(Property.class);
    expect(mimeType.getString()).andReturn(mimeTypeValue);
    expect(node.hasProperty(JCRConstants.JCR_MIMETYPE)).andReturn(true);
    expect(node.getProperty(JCRConstants.JCR_MIMETYPE)).andReturn(mimeType);
  }

  @Test
  public void testGetMimeTypeForNodeName() throws PathNotFoundException,
      ValueFormatException, RepositoryException {
    Node node = createMock(Node.class);
    expect(node.hasProperty(JCRConstants.JCR_MIMETYPE)).andReturn(false);
    String result = CropItProcessor.getMimeTypeForNode(node, "test.gif");
    assertEquals("image/gif", result);
  }

  @Test
  public void testGetMimeTypeForNodeInvalid() throws PathNotFoundException,
      ValueFormatException, RepositoryException {
    Node node = createMock(Node.class);
    expect(node.hasProperty(JCRConstants.JCR_MIMETYPE)).andReturn(false);
    String result = CropItProcessor.getMimeTypeForNode(node, "test.foo");
    assertEquals("text/plain", result);
  }

  @Test
  public void testGetScaledInstance() throws IOException {
    InputStream is = getClass().getResourceAsStream("people.png");
    BufferedImage bufImg = ImageIO.read(is);
    BufferedImage croppedImg = CropItProcessor.getScaledInstance(bufImg, 50,
        50, BufferedImage.TYPE_INT_ARGB_PRE);
    assertEquals(50, croppedImg.getWidth());
    assertEquals(50, croppedImg.getHeight());
  }

  @Test
  public void testInvalidImage() throws RepositoryException {
    expect(node.getName()).andReturn("foo.bar").anyTimes();
    expect(node.isNodeType("nt:file")).andReturn(false);
    expect(node.isNodeType("nt:resource")).andReturn(false);
    expect(node.hasNode(JCRConstants.JCR_CONTENT)).andReturn(false);
    expect(node.hasProperty(JCRConstants.JCR_DATA)).andReturn(false);
    replay();
    try {
      CropItProcessor.crop(session, x, y, width, height, dimensions, img, save);
      fail("The processor should not handle non-images.");
    } catch (ImageException e) {
      assertEquals(500, e.getCode());
    }
  }

  @Test
  public void testInvalidImageMimeType() throws RepositoryException {
    expect(node.getName()).andReturn("foo.bar").anyTimes();
    expect(node.isNodeType("nt:file")).andReturn(true);
    expect(node.hasNode(JCRConstants.JCR_CONTENT)).andReturn(true);
    expect(node.hasProperty(JCRConstants.JCR_DATA)).andReturn(false);

    Node contentNode = createMock(Node.class);
    expect(contentNode.isNodeType("nt:resource")).andReturn(true);
    createMimeType(contentNode, "image/foo");
    expect(node.getNode(JCRConstants.JCR_CONTENT)).andReturn(contentNode);

    replay();
    try {
      CropItProcessor.crop(session, x, y, width, height, dimensions, img, save);
      fail("The processor should not handle non-images.");
    } catch (ImageException e) {
      assertEquals(406, e.getCode());
    }
  }

  @Test
  public void testscaleAndWriteToStream() throws IOException {
    InputStream is = getClass().getResourceAsStream("people.png");
    BufferedImage imgBuf = ImageIO.read(is);
    BufferedImage subImage = imgBuf.getSubimage(0, 0, 100, 100);
    ByteArrayOutputStream baos = CropItProcessor.scaleAndWriteToStream(50, 50,
        subImage, "image/png", "people.png");
    InputStream scaledIs = new ByteArrayInputStream(baos.toByteArray());
    BufferedImage scaledImage = ImageIO.read(scaledIs);
    assertEquals(scaledImage.getWidth(), 50);
    assertEquals(scaledImage.getHeight(), 50);
  }

}
