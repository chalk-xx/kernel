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
package org.sakaiproject.nakamura.rules;

import org.apache.commons.io.IOExceptionWithCause;
import org.apache.commons.io.IOUtils;
import org.drools.io.Resource;
import org.drools.io.impl.BaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

/**
 * Drools Resource stored in JCR as a node.
 */
public class JCRNodeResource extends BaseResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(JCRNodeResource.class);
  private static final String CHANGE_SET_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "   <change-set xmlns=\"http://drools.org/drools-5.0/change-set\" \n"
      + "    xmlns:xs=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
      + "    xs:schemaLocation=\"http://drools.org/drools-5.0/change-set drools-change-set-5.0.xsd\" > \n";
  private static final String CHANGE_SET_POSTFIX = "</change-set>";
  private static final String LOCAL_PACKAGE_STORAGE = "rules/packages";
  private long lastRead;
  private long lastmodified;
  private String changeSetXml;

  /**
   * @param ruleSetNode
   * @throws IOException 
   * @throws RepositoryException 
   */
  public JCRNodeResource(Node ruleSetNode) throws RepositoryException, IOException {
    // FIXME: NB, we cant do this as the session may get closed by the time we want to
    // monitor it.
    // probably need to check that we can see the node and then use an admin session.
    changeSetXml = getChangeSetXML(ruleSetNode);
    
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.drools.io.InternalResource#getLastModified()
   */
  public long getLastModified() {
    return lastmodified;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.drools.io.InternalResource#getLastRead()
   */
  public long getLastRead() {
    return lastRead;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.drools.io.InternalResource#getURL()
   */
  public URL getURL() throws IOException {
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.drools.io.InternalResource#hasURL()
   */
  public boolean hasURL() {
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.drools.io.InternalResource#isDirectory()
   */
  public boolean isDirectory() {
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.drools.io.InternalResource#listResources()
   */
  public Collection<Resource> listResources() {
    return new ArrayList<Resource>();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.drools.io.Resource#getInputStream()
   */
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(changeSetXml.getBytes("UTF-8"));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.drools.io.Resource#getReader()
   */
  public Reader getReader() throws IOException {
    return new StringReader(changeSetXml);
  }

  /**
   * @return
   * @throws RepositoryException
   * @throws IOException 
   */
  private String getChangeSetXML(Node node) throws RepositoryException, IOException {
    lastRead = System.currentTimeMillis();

    StringBuilder sb = new StringBuilder();
    sb.append(CHANGE_SET_PREFIX);

    NodeIterator ni = node.getNodes();
    while (ni != null && ni.hasNext()) {
      Node n = ni.nextNode();
      if ("nt:file".equals(n.getPrimaryNodeType().getName())) {
        File f = getLocalFile(n);
        sb.append("<add><resource source=\"").append(f.toURI().toString()).append(
            "\" type=\"PKG\" /></add>\n");
      }
    }
    sb.append(CHANGE_SET_POSTFIX);
    return sb.toString();
  }

  /**
   * @param n
   * @return
   * @throws RepositoryException
   * @throws IOException
   */
  private File getLocalFile(Node n) throws RepositoryException, IOException {
    if ("nt:file".equals(n.getPrimaryNodeType().getName())) {
      String filePath = LOCAL_PACKAGE_STORAGE + n.getPath();
      long fileLastModified = n.getProperty(Property.JCR_LAST_MODIFIED).getDate()
          .getTimeInMillis();
      lastmodified = Math.max(fileLastModified, lastmodified);
      File f = new File(filePath);
      File parent = f.getParentFile();
      if (!parent.exists()) {
        f.getParentFile().mkdirs();
      }
      if (!f.exists() || fileLastModified > f.lastModified()) {
        Node contentNode = n.getNode(Node.JCR_CONTENT);
        InputStream in = contentNode.getProperty(Property.JCR_DATA).getBinary()
            .getStream();
        FileOutputStream out = new FileOutputStream(f);
        System.err.println("Copying from "+in+" to "+out);
        IOUtils.copy(in, out);
        in.close();
        out.close();
        f.setLastModified(fileLastModified);
      }
      return f;
    }
    return null;
  }

}
