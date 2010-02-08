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

import org.apache.commons.io.IOUtils;
import org.sakaiproject.kernel.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.kernel.api.docproxy.ExternalDocumentResultMetadata;
import org.sakaiproject.kernel.api.docproxy.ExternalRepositoryProcessor;
import org.sakaiproject.kernel.util.StringUtils;
import org.sakaiproject.nakamura.api.docproxy.DocProxyConstants;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * This is a proof of concept for the the External Repository processors.
 * 
 * This processor will write/read files to disk.
 */
public class DiskProcessor implements ExternalRepositoryProcessor {

  protected static final String TYPE = "disk";
  protected static final Logger LOGGER = LoggerFactory.getLogger(DiskProcessor.class);

  /**
   * {@inheritDoc}
   * 
   * @throws DocProxyException
   * 
   * @see org.sakaiproject.kernel.api.docproxy.ExternalRepositoryProcessor#getDocument(javax.jcr.Node,
   *      java.lang.String)
   */
  public ExternalDocumentResult getDocument(Node node, String path)
      throws DocProxyException {
    File f = getFile(node);
    return new DiskDocumentResult(f);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.docproxy.ExternalRepositoryProcessor#getDocumentMetadata(javax.jcr.Node,
   *      java.lang.String)
   */
  public ExternalDocumentResultMetadata getDocumentMetadata(Node node, String path)
      throws DocProxyException {
    File f = getFile(node);
    return new DiskDocumentResult(f);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.docproxy.ExternalRepositoryProcessor#getType()
   */
  public String getType() {
    return TYPE;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.docproxy.ExternalRepositoryProcessor#search(java.util.Map)
   */
  public Iterator<ExternalDocumentResult> search(Map<String, Object> searchProperties) {
    // We will search in the same directory (and subs) as the README dir.
    File defaultFile = getDefaultFile();

    String startWith = "";
    boolean matchName = false;
    if (searchProperties != null && searchProperties.get("name") != null) {
      startWith = searchProperties.get("name").toString();
      matchName = true;
    }

    final String start = startWith;
    final boolean doMatchName = matchName;
    // We don't want any files starting with a . (hidden files)
    FilenameFilter filter = new FilenameFilter() {

      public boolean accept(File dir, String name) {
        if (!name.startsWith(".")) {
          if (doMatchName) {
            if (name.startsWith(start)) {
              return true;
            } else {
              return false;
            }
          } else {
            return true;
          }
        }
        return false;
      }
    };

    // Get children.
    List<ExternalDocumentResult> results = new ArrayList<ExternalDocumentResult>();
    getChildren(defaultFile.getParentFile(), results, filter);
    return results.iterator();
  }

  /**
   * @param parentFile
   * @param results
   * @param filter
   */
  private void getChildren(File file, List<ExternalDocumentResult> results,
      FilenameFilter filter) {
    File[] files = file.listFiles(filter);
    for (File f : files) {
      if (f.isDirectory()) {
        getChildren(f, results, filter);
      }
      results.add(new DiskDocumentResult(f));
    }
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.docproxy.ExternalRepositoryProcessor#updateDocument(javax.jcr.Node,
   *      java.lang.String, java.util.Map, java.io.InputStream, long)
   */
  public Map<String, Object> updateDocument(Node node, String path,
      Map<String, Object> properties, InputStream documentStream, long streamLength)
      throws DocProxyException {
    // Get the file for this node.
    File file = getFile(node);
    path = file.getAbsolutePath();

    // Remove old file.
    if (file.exists()) {
      boolean deleted = file.delete();
      if (!deleted) {
        throw new DocProxyException(500, "Unable to update file.");
      }
    }

    // Create new file.
    File newFile = new File(path);

    // Write content to new file

    try {
      FileWriter writer = new FileWriter(newFile);
      IOUtils.copy(documentStream, writer, "UTF-8");
      writer.flush();
    } catch (IOException e) {
      throw new DocProxyException(500, "Unable to update file.");
    }
    return properties;
  }

  /**
   * Get the actual java File associated with a JCR node.
   * 
   * @param node
   *          The node in JCR.
   * @return
   * @throws DocProxyException
   *           When we were unable to get the file or read a property from the node.
   */
  protected File getFile(Node node) throws DocProxyException {
    try {
      String id = node.getProperty(DocProxyConstants.EXTERNAL_ID).getString();
      String[] parts = StringUtils.split(id, ':');
      String path = parts[1];
      return getFile(path);
    } catch (RepositoryException e) {
      throw new DocProxyException(500, "Unable to interpret node.");
    }
  }

  /**
   * @param path
   *          The absolute path on disk.
   * @return The {@link File} on that path.
   * @throws DocProxyException
   *           When the path starts with a '/' or '.' this method will throw an exception.
   */
  protected File getFile(String path) throws DocProxyException {
    if (path.startsWith("/") || path.startsWith(".")) {
      throw new DocProxyException(403,
          "This processor doesn't support path's starting with / or .");
    }
    URL url = getClass().getClassLoader().getResource(path);
    if (url == null) {
      File defaultFile = getDefaultFile();
      String newPath = defaultFile.getParent() + "/" + path;
      File file = new File(newPath);
      return file;
    }
    path = url.getPath();
    File f = new File(path);
    return f;
  }

  protected File getDefaultFile() {
    URL url = getClass().getClassLoader().getResource("README");
    File defaultFile = new File(url.getPath());
    return defaultFile;
  }

}
