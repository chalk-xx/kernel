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

import org.sakaiproject.nakamura.api.jcr.JCRConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

public class CropItProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CropItProcessor.class);

  /**
   * 
   * @param session
   *          The JCR session
   * @param x
   *          Where to start cutting on the x-axis.
   * @param y
   *          Where to start cutting on the y-axis.
   * @param width
   *          The width of the image to cut out. If <=0 then the entire image width will
   *          be used.
   * @param height
   *          The height of the image to cut out. If <=0 then the entire image width will
   *          be used.
   * @param dimensions
   *          A List of Dimension who contain the width and height for each dimension the
   *          image should be scaled into.
   * @param img
   *          The location of the image to scale.
   * @param save
   *          The location where to save all the scaled instances.
   * @return returns an array with all the location of the scaled instances.
   * @throws ImageException
   */
  public static String[] crop(Session session, int x, int y, int width, int height,
      List<Dimension> dimensions, String img, String save) throws ImageException {

    InputStream in = null;
    ByteArrayOutputStream out = null;

    // The array that will contain all the cropped and resized images.
    String[] arrFiles = new String[dimensions.size()];

    try {

      Node imgNode = (Node) session.getItem(img);

      if (imgNode != null) {

        String sImg = "";
        if (imgNode.hasNode(JCRConstants.JCR_CONTENT)) {
          sImg = imgNode.getName();
          imgNode = imgNode.getNode(JCRConstants.JCR_CONTENT);
        } else if (imgNode.hasProperty(JCRConstants.JCR_DATA)) {
          sImg = imgNode.getParent().getPath();
        }
        else {
          throw new ImageException(500, "Invalid image");
        }


        // get the MIME type of the image
        String sType = getMimeTypeForNode(imgNode, sImg);

        // check if this is a valid image
        if (sType.equalsIgnoreCase("image/png") || sType.equalsIgnoreCase("image/jpg")
            || sType.equalsIgnoreCase("image/bmp") || sType.equalsIgnoreCase("image/gif")
            || sType.equalsIgnoreCase("image/jpeg")) {

          // Read the image
          in = imgNode.getProperty(JCRConstants.JCR_DATA).getStream();

          BufferedImage imgBuf = ImageIO.read(in);

          // Set the correct width & height.
          width = (width <= 0) ? imgBuf.getWidth() : width;
          height = (height <= 0) ? imgBuf.getHeight() : height;

          if (x + width > imgBuf.getWidth()) {
            width = imgBuf.getWidth() - x;
          }
          if (y + height > imgBuf.getHeight()) {
            height = imgBuf.getHeight() - y;
          }

          // Cut the desired piece out of the image.
          BufferedImage subImage = imgBuf.getSubimage(x, y, width, height);

          // Loop the dimensions and create and save an image for each
          // one.
          for (int i = 0; i < dimensions.size(); i++) {

            Dimension d = dimensions.get(i);

            // get dimension size
            int iWidth = d.width;
            int iHeight = d.height;

            iWidth = (iWidth <= 0) ? imgBuf.getWidth() : iWidth;
            iHeight = (iHeight <= 0) ? imgBuf.getHeight() : iHeight;

            // Create the image.
            out = scaleAndWriteToStream(iWidth, iHeight, subImage, sType, sImg);

            String sPath = save + iWidth + "x" + iHeight + "_" + sImg;
            // Save new image to JCR.
            saveImageToJCR(sPath, sType, out, imgNode, session);

            out.close();
            arrFiles[i] = sPath;
          }
        } else {
          // This is not a valid image.
          LOGGER.error("Unknown image type: " + sType);
          throw new ImageException(406, "Invalid filetype: " + sType);
        }
      } else {
        throw new ImageException(400, "No image file found.");
      }

    } catch (PathNotFoundException e) {
      throw new ImageException(400, "Could not find image.");
    } catch (RepositoryException e) {
      LOGGER.warn("Unable to crop image.");
      e.printStackTrace();
      throw new ImageException(500, "Unable to crop image.");
    } catch (IOException e) {
      LOGGER.warn("Unable to read image in order to crop it.");
      e.printStackTrace();
      throw new ImageException(500, "Unable to read image in order to crop it.");
    } finally {
      // close the streams
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          LOGGER.debug("Exception closing inputstream.");
          e.printStackTrace();
        }
      }
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          LOGGER.debug("Exception closing outputstream.");
          e.printStackTrace();
        }
      }
    }
    return arrFiles;
  }

  /**
   * Will save a stream of an image to the JCR.
   * 
   * @param path
   *          The JCR path to save the image in.
   * @param mimetype
   *          The Mime type of the node that will be saved.
   * @param out
   *          The stream you wish to save.
   * @throws ImageException
   */
  public static void saveImageToJCR(String path, String mimetype,
      ByteArrayOutputStream out, Node baseNode, Session session) throws ImageException {

    // Save image into the jcr
    ByteArrayInputStream bais = null;
    try {
      path = PathUtils.normalizePath(path);
      Node node = JcrUtils.deepGetOrCreateNode(session, path, "nt:file");

      // convert stream to inputstream
      bais = new ByteArrayInputStream(out.toByteArray());
      Node contentNode = null;
      if (node.hasNode(JCRConstants.JCR_CONTENT)) {
        contentNode = node.getNode(JCRConstants.JCR_CONTENT);
      } else {
        contentNode = node.addNode(JCRConstants.JCR_CONTENT, JCRConstants.NT_RESOURCE);
      }
      contentNode.setProperty(JCRConstants.JCR_DATA, bais);
      contentNode.setProperty(JCRConstants.JCR_MIMETYPE, mimetype);
      contentNode.setProperty(JCRConstants.JCR_LASTMODIFIED, Calendar.getInstance());

      if (session.hasPendingChanges()) {
        session.save();
      }
    } catch (RepositoryException e) {
      LOGGER.warn("Repository exception: " + e.getMessage());
      e.printStackTrace();
      throw new ImageException(500, "Unable to save image to JCR.");
    } finally {
      if (bais != null) {
        try {
          bais.close();
        } catch (IOException e) {
          LOGGER.warn("Unable to close inputstream.");
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * This method will scale an image to a desired width and height and shall output the
   * stream of that scaled image.
   * 
   * @param width
   *          The desired width of the scaled image.
   * @param height
   *          The desired height of the scaled image.
   * @param img
   *          The image that you want to scale
   * @param sType
   *          The mime type of the image.
   * @param sImg
   *          Filename of the image
   * @return Returns an outputstream of the scaled image.
   * @throws IOException
   */
  public static ByteArrayOutputStream scaleAndWriteToStream(int width, int height,
      BufferedImage img, String sType, String sImg) throws IOException {
    ByteArrayOutputStream out = null;
    try {

      Map<String, Integer> mapExtensionsToRGBType = new HashMap<String, Integer>();
      mapExtensionsToRGBType.put("image/jpg", BufferedImage.TYPE_INT_RGB);
      mapExtensionsToRGBType.put("image/jpeg", BufferedImage.TYPE_INT_RGB);
      mapExtensionsToRGBType.put("image/gif", BufferedImage.TYPE_INT_ARGB);
      mapExtensionsToRGBType.put("image/png", BufferedImage.TYPE_INT_ARGB_PRE);
      mapExtensionsToRGBType.put("image/bmp", BufferedImage.TYPE_INT_RGB);

      Integer type = BufferedImage.TYPE_INT_RGB;
      if (mapExtensionsToRGBType.containsKey(sType)) {
        type = mapExtensionsToRGBType.get(sType);
      }
      
      Image imgScaled = getScaledInstance(img, width, height, type);

      BufferedImage biScaled = toBufferedImage(imgScaled, type);

      // Convert image to a stream
      out = new ByteArrayOutputStream();

      String sIOtype = sType.split("/")[1];

      // If it's a gif try to write it as a jpg
      if (sType.equalsIgnoreCase("image/gif")) {
        // TODO: This does not do anything. commenting out, what was the intention ? (Findbugs found it)
        // sImg = sImg.replaceAll("\\.gif", ".png");
        sIOtype = "png";
      }

      ImageIO.write(biScaled, sIOtype, out);
    } finally {
      if (out != null) {
        out.close();
      }
    }

    return out;
  }

  /**
   * Tries to fetch the mime type for a node. If the node lacks on, the mimetype will be
   * determined via the extension.
   * 
   * @param imgToCrop
   *          Node of the image.
   * @param sImg
   *          Filename of the image.
   * @return
   * @throws PathNotFoundException
   * @throws ValueFormatException
   * @throws RepositoryException
   */
  public static String getMimeTypeForNode(Node imgToCrop, String sImg)
      throws PathNotFoundException, ValueFormatException, RepositoryException {
    String sType = "";

    // Standard list of images we support.
    Map<String, String> mapExtensionsToMimes = new HashMap<String, String>();
    mapExtensionsToMimes.put("jpg", "image/jpeg");
    mapExtensionsToMimes.put("gif", "image/gif");
    mapExtensionsToMimes.put("png", "image/png");
    mapExtensionsToMimes.put("bmp", "image/bmp");

    // check the MIME type out of JCR
    if (imgToCrop.hasProperty(JCRConstants.JCR_MIMETYPE)) {
      Property mimeTypeProperty = imgToCrop.getProperty(JCRConstants.JCR_MIMETYPE);
      if (mimeTypeProperty != null) {
        sType = mimeTypeProperty.getString();
      }
    }
    // If we couldn't find it in the JCR we will check the extension
    if (sType.equals("")) {
      String ext = getExtension(sImg);
      if (mapExtensionsToMimes.containsKey(ext)) {
        sType = mapExtensionsToMimes.get(ext);
      }
      // default = text/plain".. This means we won't handle this image..
      else {
        sType = "text/plain";
      }
    }

    return sType;
  }

  /**
   * Returns the extension of a filename. If no extension is found, the entire filename is
   * returned.
   * 
   * @param img
   * @return
   */
  public static String getExtension(String img) {
    String[] arr = img.split("\\.");
    return arr[arr.length - 1];
  }

  /**
   * Takes an Image and converts it to a BufferedImage.
   * 
   * @param image
   *          The image you want to convert.
   * @param type
   *          The type of the image you want it to convert to. ex:
   *          BufferedImage.TYPE_INT_ARGB)
   * @return
   */
  public static BufferedImage toBufferedImage(Image image, int type) {
    int w = image.getWidth(null);
    int h = image.getHeight(null);
    BufferedImage result = new BufferedImage(w, h, type);
    Graphics2D g = result.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return result;
  }

  /**
   * Image scaling routine as prescribed by
   * http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html.
   * Image.getScaledInstance() is not very efficient or fast and this leverages the
   * graphics classes directly for a better and faster image scaling algorithm.
   * 
   * @param img
   * @param targetWidth
   * @param targetHeight
   * @return
   */
  protected static BufferedImage getScaledInstance(BufferedImage img, int targetWidth,
      int targetHeight, int type) {
    BufferedImage ret = (BufferedImage) img;

    // Use multi-step technique: start with original size, then
    // scale down in multiple passes with drawImage()
    // until the target size is reached
    int w = img.getWidth();
    int h = img.getHeight();

    while (w > targetWidth || h > targetHeight) {
      // Bit shifting by one is faster than dividing by 2.
      w >>= 1;
      if (w < targetWidth) {
        w = targetWidth;
      }

      // Bit shifting by one is faster than dividing by 2.
      h >>= 1;
      if (h < targetHeight) {
        h = targetHeight;
      }

      BufferedImage tmp = new BufferedImage(w, h, type);
      Graphics2D g2 = tmp.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING,
          RenderingHints.VALUE_RENDER_QUALITY);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      g2.drawImage(ret, 0, 0, w, h, null);
      g2.dispose();

      ret = tmp;
    }

    return ret;
  }
}
