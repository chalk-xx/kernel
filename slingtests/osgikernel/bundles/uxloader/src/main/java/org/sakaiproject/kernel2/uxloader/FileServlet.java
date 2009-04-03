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
package org.sakaiproject.kernel2.uxloader;

import org.sakaiproject.kernel.api.memory.Cache;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.sakaiproject.kernel.api.memory.CacheScope;
import org.sakaiproject.kernel.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 */
public class FileServlet extends HttpServlet {

  /**
   *
   */
  
  private static final long serialVersionUID = -1304876073995688329L;
  private static final String MIME_TYPES = "org/sakaiproject/kernel2/uxloader/mimetypes.properties";
  public static final String BASE_FILE = "base-location";
  public static final String WELCOME_FILE = "welcome-file";
  public static final String MAX_CACHE_SIZE = "max-cache-file";
  private File baseFile;
  private String baseFilePath;
  private Map<String, String> mimeTypes;
  private String welcomeFile;
  private Cache<FileCache> cache;
  private long maxCacheSize;
  

  /**
   * @param cacheManagerService
   */
  public FileServlet(CacheManagerService cacheManagerService) {
    cache = cacheManagerService.getCache("file-servlet-cache", CacheScope.INSTANCE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
   */
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    try {
      baseFile = new File(config.getInitParameter(BASE_FILE));
      baseFilePath = baseFile.getCanonicalPath();

      InputStream mimeTypeStream = this.getClass().getClassLoader().getResourceAsStream(
          MIME_TYPES);
      Properties p = new Properties();
      p.load(mimeTypeStream);
      mimeTypeStream.close();
      mimeTypes = new HashMap<String, String>();
      for (Entry<?, ?> m : p.entrySet()) {
        String[] keys = StringUtils.split((String) m.getValue(), ' ');
        for (String k : keys) {
          mimeTypes.put(k, (String) m.getKey());
        }
      }

      welcomeFile = config.getInitParameter(WELCOME_FILE);
      maxCacheSize = Integer.parseInt(config.getInitParameter(MAX_CACHE_SIZE));
    } catch (IOException e) {
      throw new ServletException(e);
    }

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    File f = new File(baseFile, req.getPathInfo());
    String cannonicalPath = f.getCanonicalPath();
    if (!cannonicalPath.startsWith(baseFilePath)) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
    if (f.isDirectory()) {
      if (welcomeFile != null) {
        File welcome = new File(f, welcomeFile);
        if (welcome.exists()) {
          sendFile(welcome, resp);
        } else {
          sendDirectory(f, resp);
        }
      } else {
        sendDirectory(f, resp);
      }
    } else {
      sendFile(f, resp);
    }
  }

  /**
   * @param f
   * @param resp
   * @throws IOException
   */
  private void sendDirectory(File f, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/html; charset=UTF-8");
    StringBuilder sb = new StringBuilder();
    sb.append("<html><head></head><body><ul>");
    for (File list : f.listFiles()) {
      sb.append("<li><a href=\"").append(list.getName()).append("\" >").append(
          list.getName()).append("</a></li>");
    }
    sb.append("</ul></body></html>");
    byte[] b = sb.toString().getBytes("UTF-8");
    resp.setContentLength(b.length);
    resp.getOutputStream().write(b);
  }

  /**
   * @param welcome
   * @param resp
   * @throws IOException
   */
  private void sendFile(File welcome, HttpServletResponse resp) throws IOException {
    long size = welcome.length();
    if ( size < maxCacheSize ) {
      String path = welcome.getAbsolutePath();
      FileCache fc = cache.get(path);
      if ( fc != null ) {
        if ( fc.getLastModified() == welcome.lastModified() ) {
          fc = null;
        }
      }
      if ( fc == null ) {
        fc = new FileCache(welcome,getContentType(welcome));
        cache.put(path, fc);
      }
      resp.setContentType(fc.getContentType());
      resp.setContentLength(fc.getContentLength());
      resp.getOutputStream().write(fc.getContent());
      return;

    }
    resp.setContentType(getContentType(welcome));
    FileInputStream fin = new FileInputStream(welcome);
    try {
      stream(fin, resp.getOutputStream());
    } finally {
      fin.close();
    }
  }

  /**
   * @param welcome
   * @return
   */
  private String getContentType(File welcome) {
    String welcomeFile = welcome.getName();
    int dot = welcomeFile.lastIndexOf('.');
    String type = "application/octet-stream";
    if (dot > 0) {
      String ext = welcomeFile.substring(dot + 1);
      String ptype = mimeTypes.get(ext);
      if (ptype != null) {
        type = ptype;
      }
    }
    return type;
  }

  /**
   * @param in
   * @param outputStream
   * @throws IOException
   */
  public static void stream(InputStream from, OutputStream to) throws IOException {
    byte[] b = new byte[4096];
    for (int i = from.read(b, 0, 4096); i >= 0; i = from.read(b, 0, 4096)) {
      if (i == 0) {
        Thread.yield();
      } else {
        to.write(b, 0, i);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServlet#doTrace(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }
}
