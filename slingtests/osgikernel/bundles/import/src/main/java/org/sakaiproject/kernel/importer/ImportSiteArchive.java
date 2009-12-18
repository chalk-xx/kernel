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

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.kernel.api.cluster.ClusterTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

@SlingServlet(methods = { "POST" }, resourceTypes = { "sling/servlet/default" }, selectors = { "sitearchive" })
@Properties(value = {
    @Property(name = "service.description", value = "Imports a zip file from Sakai2 SiteArchive."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class ImportSiteArchive extends SlingAllMethodsServlet {
  private static final long serialVersionUID = 1678771348231033621L;
  public static final Logger LOG = LoggerFactory
      .getLogger(ImportSiteArchive.class);

  @Reference
  private SlingRepository slingRepository;

  @Reference
  private ClusterTrackingService clusterTrackingService;

  private final SAXParserFactory spf = SAXParserFactory.newInstance();
  SAXParser parser = null;

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
   */
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    try {
      parser = spf.newSAXParser();
      resetSAXParser();
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  private void resetSAXParser() {
    parser.reset();
    spf.setValidating(false); // do not validate XML
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException {
    final Session session = request.getResourceResolver()
        .adaptTo(Session.class);
    final RequestParameter[] files = request.getRequestParameters("Filedata");
    if (files == null) {
      sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Missing Filedata parameter.", null, response);
      return;
    }
    for (RequestParameter p : files) {
      LOG.info((p.getFileName() + ": " + p.getContentType() + ": " + p
          .getSize()));
      try {
        // create temporary local file of zip contents
        final File tempZip = File.createTempFile("siteArchive", "zip");
        tempZip.deleteOnExit(); // just in case
        final InputStream in = p.getInputStream();
        final FileOutputStream out = new FileOutputStream(tempZip);
        final byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
        in.close();
        out.close();
        // process the zip file
        ZipFile zip = null;
        try {
          zip = new ZipFile(tempZip);
        } catch (ZipException e) {
          sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
              "Invalid zip file: " + p.getFileName() + ": "
                  + p.getContentType() + ": " + p.getSize(), null, response);
        }
        if (zip != null) {
          for (ZipEntry entry : Collections.list(zip.entries())) {
            if (entry.getName().startsWith("__MACOSX")
                || entry.getName().endsWith(".DS_Store")) {
              ; // skip entry
            } else {
              if ("content.xml".equals(entry.getName())) {
                LOG.info("found content.xml!");
                parser.parse(zip.getInputStream(entry),
                    new SiteArchiveContentHandler("/lance/import", zip,
                        session, slingRepository, clusterTrackingService));
                resetSAXParser();
              }
            }
          }
          zip.close();
        }
        // delete temporary file
        tempZip.delete();
      } catch (IOException e) {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
            .getLocalizedMessage(), e, response);
      } catch (SAXException e) {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
            .getLocalizedMessage(), e, response);
      }
    }
  }

  private void sendError(int errorCode, String message, Throwable exception,
      HttpServletResponse response) {
    if (!response.isCommitted()) {
      try {
        LOG.error(errorCode + ": " + message, exception);
        response.sendError(errorCode, message);
      } catch (IOException e) {
        throw new Error(e);
      }
    } else {
      LOG.error(errorCode + ": " + message, exception);
      throw new Error(message);
    }
  }
}
