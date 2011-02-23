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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;
import org.sakaiproject.nakamura.lite.jackrabbit.SparseMapUserManager;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.jcr.RepositoryException;

/**
 *
 */
public class CropItProcessorTest {

  private Session session;
  private String img = "/foo/people.png";
  private int x = 0;
  private int y = 0;
  private int width = 100;
  private int height = 100;
  private List<Dimension> dimensions;
  private String save = "/save/in/here/";
  private Content node;

  @Before
  public void setUp() throws Exception {
    BaseMemoryRepository baseMemoryRepository = new BaseMemoryRepository();
    Repository repository = baseMemoryRepository.getRepository();
    session = repository.loginAdministrative();
    session.getContentManager().update(new Content(img, null));
    node = session.getContentManager().get(img);
    dimensions = new ArrayList<Dimension>();
    Dimension d = new Dimension();
    d.setSize(50, 50);
    dimensions.add(d);
  }

  @Test
  public void testGetScaledInstance() throws IOException, ImageReadException {
    InputStream is = getClass().getResourceAsStream("people.png");
    BufferedImage bufImg = Sanselan.getBufferedImage(is);
    BufferedImage croppedImg = CropItProcessor.getScaledInstance(bufImg, 50, 50);
    assertEquals(50, croppedImg.getWidth());
    assertEquals(50, croppedImg.getHeight());
  }

  @Test
  public void testInvalidImage() throws RepositoryException, StorageClientException, AccessDeniedException {
    node.setProperty("path", "/path/to/the/file/foo.bar");
    session.getContentManager().update(node);
    ResourceResolver resourceResolver = mock(ResourceResolver.class);
    SparseContentResource someResource = mock(SparseContentResource.class);
    when(someResource.adaptTo(Content.class)).thenReturn(node);
    JackrabbitSession jrSession = mock(JackrabbitSession.class);
    SparseMapUserManager userManager = mock(SparseMapUserManager.class);
    when(userManager.getSession()).thenReturn(session);
    when(jrSession.getUserManager()).thenReturn(userManager);
    when(resourceResolver.adaptTo(javax.jcr.Session.class)).thenReturn(jrSession);
    when(resourceResolver.getResource(anyString())).thenReturn(someResource);
    try {
      CropItProcessor.crop(resourceResolver, x, y, width, height, dimensions, img, save);
      fail("The processor should not handle non-images.");
    } catch (ImageException e) {
      assertEquals(500, e.getCode());
    }
  }

  @Test
  public void testInvalidImageMimeType() throws RepositoryException, StorageClientException, AccessDeniedException, IOException {
    ResourceResolver resourceResolver = mock(ResourceResolver.class);
    node.setProperty("path", "/path/to/foo.bar");
    node.setProperty("_bodyLocation", "2011/1/tz/fv/8x");
    node.setProperty("mimeType", "image/foo");
    session.getContentManager().update(node);
    SparseContentResource someResource = mock(SparseContentResource.class);
    when(someResource.adaptTo(Content.class)).thenReturn(node);
    JackrabbitSession jrSession = mock(JackrabbitSession.class);
    SparseMapUserManager userManager = mock(SparseMapUserManager.class);
    when(userManager.getSession()).thenReturn(session);
    when(jrSession.getUserManager()).thenReturn(userManager);
    when(resourceResolver.adaptTo(javax.jcr.Session.class)).thenReturn(jrSession);
    when(resourceResolver.getResource(anyString())).thenReturn(someResource);
    try {
      CropItProcessor.crop(resourceResolver, x, y, width, height, dimensions, img, save);
      fail("The processor should not handle non-images.");
    } catch (ImageException e) {
      assertEquals(406, e.getCode());
    }
  }

  @Test
  public void testscaleAndWriteToStream() throws IOException, ImageWriteException,
      ImageReadException {
    InputStream is = getClass().getResourceAsStream("people.png");
    BufferedImage imgBuf = Sanselan.getBufferedImage(is);
    BufferedImage subImage = imgBuf.getSubimage(0, 0, 100, 100);
    ImageInfo info = new ImageInfo("PNG", 8, null, ImageFormat.IMAGE_FORMAT_PNG, "PNG",
        256, "image/png", 1, 76, 76, 76, 76, 256, true, true, false, 2, "ZIP");
    byte[] image = CropItProcessor.scaleAndWriteToByteArray(50, 50, subImage,
        "people.png", info);
    InputStream scaledIs = new ByteArrayInputStream(image);
    BufferedImage scaledImage = ImageIO.read(scaledIs);
    assertEquals(scaledImage.getWidth(), 50);
    assertEquals(scaledImage.getHeight(), 50);
  }

}
