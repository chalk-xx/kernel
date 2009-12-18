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

package org.sakaiproject.kernel.importer;

import org.apache.commons.codec.binary.Base64;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.kernel.api.cluster.ClusterTrackingService;
import org.sakaiproject.kernel.api.files.FileUtils;
import org.sakaiproject.kernel.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Not thread safe (i.e. you must create a new {@link SiteArchiveContentHandler}
 * for each SAX parse).
 */
public class SiteArchiveContentHandler extends DefaultHandler {
  public static final Logger LOG = LoggerFactory
      .getLogger(SiteArchiveContentHandler.class);
  private final String[] supportedVersions = { "Sakai 1.0" };
  private Base64 base64 = new Base64();
  private String basePath;
  private ZipFile zip;
  private Session session;
  private SlingRepository slingRepository;
  private ClusterTrackingService clusterTrackingService;
  private String date;
  private String server;
  private String source;
  private String system;
  private Map<String, Resource> resources = new HashMap<String, Resource>();
  private String currentResourceId;

  public SiteArchiveContentHandler(String basePath, ZipFile zip,
      Session session, SlingRepository slingRepository,
      ClusterTrackingService clusterTrackingService) {
    if (basePath == null || "".equals(basePath)) {
      throw new IllegalArgumentException("Illegal basePath");
    }
    if (zip == null) {
      throw new IllegalArgumentException("Illegal ZipFile");
    }
    if (session == null) {
      throw new IllegalArgumentException("Illegal Session");
    }
    if (slingRepository == null) {
      throw new IllegalArgumentException("Illegal SlingRepository");
    }
    if (clusterTrackingService == null) {
      throw new IllegalArgumentException("Illegal ClusterTrackingService");
    }
    if (basePath.endsWith("/")) { // strip trailing slash
      int lastSlash = basePath.lastIndexOf("/");
      this.basePath = basePath.substring(0, lastSlash);
    } else {
      this.basePath = basePath;
    }
    this.zip = zip;
    this.session = session;
    this.slingRepository = slingRepository;
    this.clusterTrackingService = clusterTrackingService;
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
   *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
   */
  @Override
  public void startElement(String uri, String localName, String qName,
      Attributes attributes) throws SAXException {
    if ("archive".equalsIgnoreCase(qName)) {
      date = attributes.getValue("date");
      server = attributes.getValue("server");
      source = attributes.getValue("source");
      system = attributes.getValue("system");
      boolean supportedVersion = false;
      for (String version : supportedVersions) {
        if (version.equalsIgnoreCase(system)) {
          supportedVersion = true;
        }
      }
      if (!supportedVersion) {
        throw new Error("not a supported version: " + system);
      }
    }
    if ("collection".equalsIgnoreCase(qName)
        || "resource".equalsIgnoreCase(qName)) {
      // grab the resource's attributes
      Resource resource = new Resource();
      for (int i = 0; i < attributes.getLength(); i++) {
        resource.attributes.put(attributes.getLocalName(i).toLowerCase(),
            attributes.getValue(i));
      }
      currentResourceId = resource.getId();
      resources.put(currentResourceId, resource);
    }
    if ("property".equalsIgnoreCase(qName)) {
      Resource collection = resources.get(currentResourceId);
      final String name = attributes.getValue("name");
      if (attributes.getValue("value") != null
          && !"".equals(attributes.getValue("value"))) {
        String value = null;
        if (attributes.getValue("enc").equalsIgnoreCase("BASE64")) {
          value = new String(base64.decode(attributes.getValue("value")));
        } else {
          value = attributes.getValue("value");
        }
        collection.properties.put(name, value);
      }
    }
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
   *      java.lang.String, java.lang.String)
   */
  @Override
  public void endElement(String uri, String localName, String qName)
      throws SAXException {
    if ("collection".equalsIgnoreCase(qName)
        || "resource".equalsIgnoreCase(qName)) {
      makeResource(resources.get(currentResourceId));
    }
  }

  private void makeResource(Resource resource) {
    if (resource == null) {
      throw new IllegalArgumentException("Illegal Resource");
    }
    final String destination = basePath + "/" + resource.getRelativeId();
    if ("org.sakaiproject.content.types.folder".equalsIgnoreCase(resource
        .getType())) {
      makeNode(destination);
    } else if ("org.sakaiproject.content.types.fileUpload"
        .equalsIgnoreCase(resource.getType())
        || "org.sakaiproject.content.types.TextDocumentType"
            .equalsIgnoreCase(resource.getType())
        || "org.sakaiproject.content.types.HtmlDocumentType"
            .equalsIgnoreCase(resource.getType())
    // || "org.sakaiproject.content.types.urlResource"
    // .equalsIgnoreCase(resource.getType())
    ) {
      copyFile(resource.attributes.get("body-location"), destination,
          resource.attributes.get("content-type"));
      // StringBuilder sb = new StringBuilder();
      // for (String key : resource.properties.keySet()) {
      // sb.append(key);
      // sb.append("=");
      // sb.append(resource.properties.get(key));
      // sb.append("\n");
      // }
    } else {
      LOG.error("Mising handler for type: " + resource.getType());
    }
  }

  private Node makeNode(String path) {
    if (path.endsWith("/")) { // strip trailing slash
      path = path.substring(0, path.lastIndexOf("/"));
    }
    Node node = null;
    try {
      node = JcrUtils.deepGetOrCreateNode(session, path);
    } catch (RepositoryException e) {
      throw new Error(e);
    }
    return node;
  }

  private Node copyFile(String zipEntryName, String destination,
      String contentType) {
    if (destination.endsWith("/")) { // strip trailing slash
      destination = destination.substring(0, destination.lastIndexOf("/"));
    }
    // copied from FilesUploadServlet.java
    String id = clusterTrackingService.getClusterUniqueId();
    if (id.endsWith("==")) {
      id = id.substring(0, id.length() - 2);
    }
    id = id.replace('/', '_').replace('=', '-');
    // end copied from FilesUploadServlet.java
    final String fileName = destination
        .substring(destination.lastIndexOf("/") + 1);
    Node node = null;
    try {
      final InputStream in = zip.getInputStream(zip.getEntry(zipEntryName));
      node = FileUtils.saveFile(session, destination, id, in, fileName,
          contentType, slingRepository);
    } catch (RepositoryException e) {
      throw new Error(e);
    } catch (IOException e) {
      throw new Error(e);
    }
    return node;
  }

  private static class Resource {
    private Map<String, String> attributes = new HashMap<String, String>();
    private Map<String, String> properties = new HashMap<String, String>();

    public String getId() {
      return attributes.get("id");
    }

    public String getRelativeId() {
      return attributes.get("rel-id");
    }

    public String getType() {
      return attributes.get("resource-type");
    }

    @Override
    public int hashCode() {
      return this.getId().hashCode();
    }

    @Override
    public String toString() {
      return getRelativeId();
    }
  }
}
