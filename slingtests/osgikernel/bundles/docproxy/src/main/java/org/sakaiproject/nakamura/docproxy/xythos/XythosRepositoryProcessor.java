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
package org.sakaiproject.nakamura.docproxy.xythos;

import com.caucho.hessian.client.HessianProxyFactory;

import edu.nyu.XythosRemote;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata;
import org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * This is a proof of concept for the the External Repository processors.
 *
 * This processor will write/read files to Xythos at a remote URI
 */
@Component(enabled = true, immediate = true, metatype = true)
@Service(value = ExternalRepositoryProcessor.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Document Proxy implementation for Xythos Repository"),
    @Property(name = "service.note", value = "This service is in alpha") })
public class XythosRepositoryProcessor implements ExternalRepositoryProcessor {

  @Property(name = "xythosHost", description = "The remote host (and port) of the Xythos instance", value="http://localhost:9090")
  protected String xythosHost = "http://localhost:9090";

  protected String remotePath = "/remoting/remoting/XythosService";

  protected static final String TYPE = "xythos";
  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#getDocument(javax.jcr.Node, java.lang.String)
   */
  public ExternalDocumentResult getDocument(Node node, String path)
      throws DocProxyException {
    try {
      String currentUserId = node.getSession().getUserID();
      if ("anonymous".equals(currentUserId)) throw new DocProxyException(402, "anonymous user may not access Xythos");
      return getFile(path, currentUserId);
    } catch (RepositoryException e) {
      throw new DocProxyException(500, "caused by RepositoryException getting session for requested Node");
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#getDocumentMetadata(javax.jcr.Node, java.lang.String)
   */
  public ExternalDocumentResultMetadata getDocumentMetadata(Node node, String path)
      throws DocProxyException {
    try {
      String currentUserId = node.getSession().getUserID();
       if ("anonymous".equals(currentUserId)) throw new DocProxyException(402, "anonymous user may not access Xythos");
      return getFile(path, currentUserId);
    } catch (RepositoryException e) {
      throw new DocProxyException(500, "caused by RepositoryException getting session for requested Node");
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#getType()
   */
  public String getType() {
    return TYPE;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#search(javax.jcr.Node, java.util.Map)
   */
  public Iterator<ExternalDocumentResult> search(Node node,
      Map<String, Object> searchProperties) throws DocProxyException {
    try {
      String currentUserId = node.getSession().getUserID();
      Collection<ExternalDocumentResult> searchResults = new ArrayList<ExternalDocumentResult>();
      HessianProxyFactory factory = new HessianProxyFactory();
      XythosRemote xythos = (XythosRemote) factory.create(XythosRemote.class, xythosHost+remotePath, XythosRepositoryProcessor.class.getClassLoader());
      List<String> searchResultsPaths = xythos.doSearch(searchProperties, currentUserId);
      if (searchResultsPaths == null) {
        searchResultsPaths = new ArrayList<String>();
      }
      for(String path : searchResultsPaths) {
        String[] pathStems = path.split("/");
        if (pathStems.length > 2 && pathStems[2].equals("trash")) {
          continue;
        }
        searchResults.add(getFile(path, currentUserId));
      }
      return searchResults.iterator();
    } catch (MalformedURLException e) {
      throw new RuntimeException("MalformedURLException: " + e.getMessage());
    } catch (RepositoryException e) {
      throw new RuntimeException("RepositoryException: " + e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#updateDocument(javax.jcr.Node, java.lang.String, java.util.Map, java.io.InputStream, long)
   */
  public Map<String, Object> updateDocument(Node node, String path,
      Map<String, Object> properties, InputStream documentStream, long streamLength)
      throws DocProxyException {
    try {
      // MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
      if (properties == null) {
        properties = new HashMap<String, Object>();
      }

      // Collection mimeTypes = MimeUtil.getMimeTypes(documentStream);
      String contentType = new MimetypesFileTypeMap().getContentType(path.substring(path.lastIndexOf("/") + 1));
      properties.put("contentType", contentType);
      String currentUserId = node.getSession().getUserID();
      HessianProxyFactory factory = new HessianProxyFactory();

      XythosRemote xythos = (XythosRemote) factory.create(XythosRemote.class, xythosHost+remotePath, XythosRepositoryProcessor.class.getClassLoader());
      byte[] fileData = new byte[documentStream.available()];
      documentStream.read(fileData);
      xythos.updateFile("/zach/" + path, fileData, properties, currentUserId);
      return properties;
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  private XythosDocumentResult getFile(String path, String userId) {

    try {
      HessianProxyFactory factory = new HessianProxyFactory();
      XythosRemote xythos = (XythosRemote) factory.create(XythosRemote.class, xythosHost+remotePath, XythosRepositoryProcessor.class.getClassLoader());
      String contentType = xythos.getContentType(path, userId);
      long contentLength = xythos.getContentLength(path, userId);
      Map<String, Object> props = xythos.getFileProperties(path, userId);
      String uri = "/xythos" + path;
      byte[] data = xythos.getFileContent(path, userId);
      return new XythosDocumentResult(data, contentLength, contentType, props, uri);
    } catch (MalformedURLException e) {
      throw new RuntimeException("MalformedURLException: " + e.getMessage());
    }
  }

}
