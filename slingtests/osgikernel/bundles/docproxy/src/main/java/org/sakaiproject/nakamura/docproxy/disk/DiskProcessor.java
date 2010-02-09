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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.docproxy.DocProxyConstants;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata;
import org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor;
import org.sakaiproject.nakamura.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * This is a proof of concept for the the External Repository processors.
 * 
 * This processor will write/read files to disk, DO NOT ENABLE THIS SERVICE ON A PUBLIC
 * OX!
 */
@Component(enabled = false, immediate = true)
@Service(value = ExternalRepositoryProcessor.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Proof-of-concept implementation of the Document Proxy API."),
    @Property(name = "service.note", value = "This processor should NOT be run in production. It is extremely likely this can be abused to hack in the system.") })
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
    File f = getFile(node, path);
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
    File f = getFile(node, path);
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
    File file = getFile(node, path);

    if (documentStream != null) {
      // Write the stream
      writeStreamToFile(documentStream, file);
    }
    if (properties != null) {
      try {
        // Write the json file
        File propertiesFile = getFile(node, path + ".json");

        // Retrieve previous properties
        DiskDocumentResult result = new DiskDocumentResult(file);
        JSONObject obj = new JSONObject(result.getProperties());

        // Write/Update new ones
        for (Entry<String, Object> entry : properties.entrySet()) {
          Object val = entry.getValue();
          if (val.getClass().isArray()) {
            String[] arr = (String[]) val;
            if (arr.length == 1) {
              obj.put(entry.getKey(), arr[0]);
            } else {
              JSONArray jsonArr = new JSONArray();
              for (String s : arr) {
                jsonArr.put(s);
              }
              obj.put(entry.getKey(), jsonArr);
            }
          } else {
            obj.put(entry.getKey(), val);
          }
        }
        String json = obj.toString();
        ByteArrayInputStream jsonStream = new ByteArrayInputStream(json.getBytes("UTF-8"));
        writeStreamToFile(jsonStream, propertiesFile);
      } catch (UnsupportedEncodingException e) {
        throw new DocProxyException(500, "Unable to save properties.");
      } catch (JSONException e) {
        throw new DocProxyException(500, "Unable to retrieve properties from request.");
      }
    }

    return properties;
  }

  protected void writeStreamToFile(InputStream documentStream, File file)
      throws DocProxyException {
    // Remove old file.
    if (file.exists()) {
      boolean deleted = file.delete();
      if (!deleted) {
        throw new DocProxyException(500, "Unable to update file.");
      }
    }
    // Create new file.
    File newFile = new File(file.getAbsolutePath());
    try {
      newFile.createNewFile();
    } catch (IOException e) {
      throw new DocProxyException(500, "Unable to create new file.");
    }

    // Check if we can write.
    if (!newFile.canWrite()) {
      throw new DocProxyException(500, "No write access on file.");
    }

    // Write content to new file

    try {
      FileOutputStream out = new FileOutputStream(newFile);
      IOUtils.stream(documentStream, out);
    } catch (IOException e) {
      throw new DocProxyException(500, "Unable to update file.");
    }
  }

  /**
   * Get the actual java File associated with a JCR node.
   * 
   * @param node
   *          The node in JCR.
   * @param path
   * @return
   * @throws DocProxyException
   *           When we were unable to get the file or read a property from the node.
   */
  protected File getFile(Node node, String path) throws DocProxyException {
    try {
      String basePath = node.getProperty(DocProxyConstants.REPOSITORY_LOCATION)
          .getString();
      while (path.startsWith("/") || path.startsWith(".")) {
        path = path.substring(1);
      }

      if (basePath.endsWith("/")) {
        path = basePath + path;
      } else {
        path = basePath + "/" + path;
      }

      return getFile(path);
    } catch (RepositoryException e) {
      throw new DocProxyException(500, "Unable to read from node property.");
    }
  }

  /**
   * @param path
   *          The absolute path on disk.
   * @return The {@link File} on that path.
   */
  protected File getFile(String path) throws DocProxyException {
    File f = new File(path);
    return f;
  }

  protected File getDefaultFile() {
    URL url = getClass().getClassLoader().getResource("README");
    File defaultFile = new File(url.getPath());
    return defaultFile;
  }

}
