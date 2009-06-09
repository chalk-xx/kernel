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

package org.sakaiproject.kernel.image;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.sakaiproject.kernel.api.jcr.JCRConstants;
import org.sakaiproject.kernel.api.jcr.support.JCRNodeFactoryService;
import org.sakaiproject.kernel.api.jcr.support.JCRNodeFactoryServiceException;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

public class CropItProcessor {

  public static JCRNodeFactoryService jcrNodeFactoryService;

  /**
   * 
   * @param x
   *          Where to start cutting on the x-axis.
   * @param y
   *          Where to start cutting on the y-axis.
   * @param width
   *          The width of the image to cut out. If <=0 then the entire image width will be used.
   * @param height
   *          The height of the image to cut out.If <=0 then the entire image height will be used.
   * @param dimensions
   *          A JSONArray with the different dimensions.
   * @param urlSaveIn
   *          Where to save the new images.
   * @param nImgToCrop
   *          The node that contains the base image.
   * @param jcrNodeFactoryService
   * @return
   * @throws ImageException
   */
  public static String[] crop(int x, int y, int width, int height,
      JSONArray dimensions, String urlSaveIn, Node nImgToCrop,
      JCRNodeFactoryService jcrNodeFactoryService) throws ImageException {

    InputStream in = null;
    ByteArrayOutputStream out = null;
    
    // The array that will contain all the cropped and resized images.
    String[] arrFiles = new String[dimensions.size()];

    CropItProcessor.jcrNodeFactoryService = jcrNodeFactoryService;

    try {
      if (nImgToCrop != null) {

        String sImg = nImgToCrop.getName();

        // get the MIME type of the image
        String sType = getMimeTypeForNode(nImgToCrop, sImg);

        // check if this is a valid image
        if (sType.equalsIgnoreCase("image/png")
            || sType.equalsIgnoreCase("image/jpg")
            || sType.equalsIgnoreCase("image/bmp")
            || sType.equalsIgnoreCase("image/gif")
            || sType.equalsIgnoreCase("image/jpeg")) {

          // Read the image
          in = jcrNodeFactoryService.getInputStream(nImgToCrop.getPath());

          BufferedImage img = ImageIO.read(in);
          
          // Set the correct width & height.
          width = (width <= 0) ? img.getWidth() : width;
          height = (height <= 0) ? img.getHeight() : height; 
          
          // Cut the desired piece out of the image.
          BufferedImage subImage = img.getSubimage(x, y, width, height);

          // Loop the dimensions and create and save an image for each
          // one.
          for (int i = 0; i < dimensions.size(); i++) {

            JSONObject o = dimensions.getJSONObject(i);

            // get dimension size
            int iWidth = Integer.parseInt(o.get("width").toString());
            int iHeight = Integer.parseInt(o.get("height").toString());

            iWidth = (iWidth <= 0) ? img.getWidth() : iWidth;
            iHeight = (iHeight <= 0) ? img.getHeight() : iHeight; 

            // Create the image.
            out = scaleAndWriteToStream(iWidth, iHeight, subImage, sType, sImg);

            String sPath = urlSaveIn + iWidth + "x" + iHeight + "_" + sImg;
            // Save new image to JCR.
            SaveImageToJCR(sPath, sType, out, nImgToCrop);

            out.close();
            arrFiles[i] = sPath;
          }
        } else {
          // This is not a valid image.
          throw new ImageException("Invalid filetype.");
        }
      } else {
        throw new ImageException("No file found.");
      }

    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JCRNodeFactoryServiceException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      // close the streams
      if (in != null)
        try {
          in.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      if (out != null)
        try {
          out.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
    }
    return arrFiles;
  }

  /**
   * Generate a JSON response.
   * 
   * @param response
   *          ERROR or OK
   * @param typeOfResponse
   *          The name of the extra tag you want to add. ex: message or files
   * @param parameters
   *          The object you wish to parse.
   * @return
   */
  public static String generateResponse(String response, String typeOfResponse,
      Object parameters) {
    Map<String, Object> mapResponse = new HashMap<String, Object>();
    mapResponse.put("response", response);
    mapResponse.put(typeOfResponse, parameters);
    return JSONObject.fromObject(mapResponse).toString();
  }

  /**
   * Will save a stream of an image to the JCR.
   * 
   * @param sPath
   *          The JCR path to save the image in.
   * @param sType
   *          The Mime type of the node that will be saved.
   * @param out
   *          The stream you wish to save.
   * @throws RepositoryException
   * @throws JCRNodeFactoryServiceException
   * @throws IOException
   */
  public static void SaveImageToJCR(String sPath, String sType,
      ByteArrayOutputStream out, Node baseNode) throws RepositoryException,
      JCRNodeFactoryServiceException, IOException {
    // Save image into the jcr

    Node n = jcrNodeFactoryService.getNode(sPath);

    System.out.println(sPath);

    // This node doesn't exist yet. Create and save it.
    if (n == null) {
      n = jcrNodeFactoryService.createFile(sPath, sType);
      n.getParent().save();
    }

    // convert stream to inputstream
    ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
    try {
      jcrNodeFactoryService.setInputStream(sPath, bais, sType);
      n.setProperty(JCRConstants.JCR_MIMETYPE, sType);
      n.save();
    } finally {
      bais.close();
    }
  }

  /**
   * This method will scale an image to a desired width and height and shall
   * output the stream of that scaled image.
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
  public static ByteArrayOutputStream scaleAndWriteToStream(int width,
      int height, BufferedImage img, String sType, String sImg)
      throws IOException {
    ByteArrayOutputStream out = null;
    try {
      Image imgScaled = img.getScaledInstance(width, height,
          Image.SCALE_AREA_AVERAGING);

      Map<String, Integer> mapExtensionsToRGBType = new HashMap<String, Integer>();
      mapExtensionsToRGBType.put("image/jpg", BufferedImage.TYPE_INT_RGB);
      mapExtensionsToRGBType.put("image/jpeg", BufferedImage.TYPE_INT_RGB);
      mapExtensionsToRGBType.put("image/gif", BufferedImage.TYPE_INT_RGB);
      mapExtensionsToRGBType.put("image/png", BufferedImage.TYPE_INT_ARGB);
      mapExtensionsToRGBType.put("image/bmp", BufferedImage.TYPE_INT_RGB);

      Integer type = BufferedImage.TYPE_INT_RGB;
      if (mapExtensionsToRGBType.containsKey(sType)) {
        type = mapExtensionsToRGBType.get(sType);
      }

      BufferedImage biScaled = toBufferedImage(imgScaled, type);

      // Convert image to a stream
      out = new ByteArrayOutputStream();

      String sIOtype = sType.split("/")[1];

      // If it's a gif try to write it as a jpg
      if (sType.equalsIgnoreCase("image/gif")) {
        sImg = sImg.replaceAll("\\.gif", ".jpg");
        sIOtype = "jpg";
      }

      ImageIO.write(biScaled, sIOtype, out);
    } finally {
      if (out != null)
        out.close();
    }

    return out;
  }

  /**
   * Tries to fetch the mime type for a node. If the node lacks on, the mimetype
   * will be determined via the extension.
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
      Property mimeTypeProperty = imgToCrop
          .getProperty(JCRConstants.JCR_MIMETYPE);
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
      // default = jpg
      else {
        sType = mapExtensionsToMimes.get("jpg");
      }
    }

    return sType;
  }

  /**
   * Returns the extension of a filename. If no extension is found, the entire
   * filename is returned.
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

}
