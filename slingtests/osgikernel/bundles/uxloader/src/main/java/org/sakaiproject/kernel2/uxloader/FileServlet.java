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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.sakaiproject.kernel.api.memory.Cache;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.sakaiproject.kernel.api.memory.CacheScope;
import org.sakaiproject.kernel.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger logger = LoggerFactory.getLogger(FileServlet.class);

	
  /**
   *
   */
  
  private static final long serialVersionUID = -1304876073995688329L;
  private static final String MIME_TYPES = "org/sakaiproject/kernel2/uxloader/mimetypes.properties";
  public static final String BASE_FILE = "base-location";
  public static final String WELCOME_FILE = "welcome-file";
  public static final String MAX_CACHE_SIZE = "max-cache-file";
  private File baseFile;
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

  private void loadMIMETypesFromFile() throws IOException {
      InputStream mimeTypeStream = this.getClass().getClassLoader().getResourceAsStream(MIME_TYPES);
	  Properties p = new Properties();
	  p.load(mimeTypeStream);
	  mimeTypeStream.close();
	  mimeTypes = new HashMap<String, String>();
	  for (Entry<?,?> m : p.entrySet()) {
		  String[] keys = StringUtils.split((String) m.getValue(), ' ');
		  for (String k : keys) {
			  mimeTypes.put(k, (String) m.getKey());
		  }
	  }
  }

  private String getContentTypeFromExtension(File welcome) {
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
   * {@inheritDoc}
   * 
   * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
   */
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    try {
      baseFile = new File(config.getInitParameter(BASE_FILE));
      welcomeFile = config.getInitParameter(WELCOME_FILE);
      maxCacheSize = Integer.parseInt(config.getInitParameter(MAX_CACHE_SIZE));
      loadMIMETypesFromFile();
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }
  
  private boolean in_our_path(File candidate) throws IOException {
	  return candidate.getCanonicalPath().startsWith(baseFile.getCanonicalPath());
  }
  
  private File computeFileToSendForDirectory(File dir) {
	  if(welcomeFile!=null) {
		  File index=new File(dir,welcomeFile);
		  if(index.exists())
			  return index;
	  }
	  return dir;
  }
  
  private File computeFileToSend(File candidate) throws IOException {
	  if(!in_our_path(candidate)) {
		  logger.warn("Not in our path!");
		  return null;
	  }
	  if(candidate.isDirectory())
		  return computeFileToSendForDirectory(candidate);
	  return candidate;
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	  logger.debug("Looking for "+baseFile+req.getPathInfo());
	  File to_send=computeFileToSend(new File(baseFile+req.getPathInfo()));
	  if(to_send==null) {
		  resp.sendError(HttpServletResponse.SC_FORBIDDEN);
		  return;
	  }
	  logger.debug("Sending "+to_send.getAbsolutePath());
	  if(to_send.isDirectory())
		  sendDirectory(to_send,resp);
	  else
		  sendFile(to_send,resp);
  }
  
  private void sendDirectory(File f, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/html; charset=UTF-8");
    StringBuilder sb = new StringBuilder();
    sb.append("<html><head></head><body><ul>");
    for (File list : f.listFiles()) {
      sb.append("<li><a href=\"");
      sb.append(StringEscapeUtils.escapeXml(list.getName()));
      sb.append("\" >");
      sb.append(StringEscapeUtils.escapeXml(list.getName()));
      sb.append("</a></li>");
    }
    sb.append("</ul></body></html>");
    byte[] b = sb.toString().getBytes("UTF-8");
    resp.setContentLength(b.length);
    resp.getOutputStream().write(b);
  }

  private boolean candidateForCache(File file) {
	  return file.length() < maxCacheSize;
  } 
  
  private FileCache getNonExpiredFileCache(File file) {
	  if(!candidateForCache(file))
		  return null;
	  String path = file.getAbsolutePath();
	  FileCache fc = cache.get(path);
	  if(fc==null)
		  return null;
	  if ( fc.getLastModified() != file.lastModified() )
		  return null;
	  return fc;
  }
  
  private void sendFile(File file, HttpServletResponse resp) throws IOException {
	  if(!file.exists()) {
		  resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		  return;
	  }	  
	  if(candidateForCache(file)) {
		  // Cacheable
		  FileCache cache_entry=getNonExpiredFileCache(file);
		  if(cache_entry==null) {
			  cache_entry = new FileCache(file,getContentTypeFromExtension(file));
			  cache.put(file.getAbsolutePath(),cache_entry);
		  }
		  resp.setContentType(cache_entry.getContentType());
		  resp.setContentLength(cache_entry.getContentLength());
		  resp.getOutputStream().write(cache_entry.getContent());
		  return;
	  } else {
		  // Not cacheable
		  resp.setContentType(getContentTypeFromExtension(file));
		  FileInputStream fin = new FileInputStream(file);
		  try {
			  IOUtils.copy(fin, resp.getOutputStream());
		  } finally {
			  fin.close();
		  }
	  }
  }
  
  @Override
  protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }
}
