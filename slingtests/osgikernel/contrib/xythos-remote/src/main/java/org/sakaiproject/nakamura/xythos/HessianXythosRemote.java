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
package org.sakaiproject.nakamura.xythos;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.client.HessianProxyFactory;

import edu.nyu.XythosDocument;
import edu.nyu.XythosRemote;

/**
 * This is a bundle to provide access to Xythos on a remote server
 * 
 * This processor will write/read files to Xythos at a remote URI
 * @scr.component immediate="true" label="XythosRemote"
 *                description="Xythos Remote API implementation"
 * @scr.service interface="edu.nyu.XythosRemote"
 * @scr.property name="service.description"
 *               value="Xythos remote API"
 * @scr.property name="service.vendor" value="New York University"
 */
public class HessianXythosRemote implements XythosRemote {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(HessianXythosRemote.class);
  
  @Property(name = "xythosHost", description = "The remote host (and port) of the Xythos instance", value="http://xtest1.home.nyu.edu:8080")
  protected String xythosHost = "http://localhost:9090";
  
  protected String remotePath = "/remoting/remoting/XythosService";
  
  XythosRemote xythosService;

  
  public HessianXythosRemote() {
    try {
      HessianProxyFactory factory = new HessianProxyFactory();
      xythosService = (XythosRemote) factory.create(XythosRemote.class,
          xythosHost + remotePath, HessianXythosRemote.class.getClassLoader());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e.getMessage());
    }
  }
  
  public void createDirectory(String arg0, String arg1, String arg2, String arg3) {
    xythosService.createDirectory(arg0, arg1, arg2, arg3);
  }

  public void createGroup(String arg0, String arg1) {
   xythosService.createGroup(arg0, arg1);
  }

  public List<String> doSearch(Map<String, Object> arg0, String arg1) {
    return xythosService.doSearch(arg0, arg1);
  }

  public String findAllFilesForUser(String arg0) {
    return xythosService.findAllFilesForUser(arg0);
  }

  public Collection<Map<String, String>> findFilesWithXPath(String arg0, String arg1) {
    return xythosService.findFilesWithXPath(arg0, arg1);
  }

  public long getContentLength(String arg0, String arg1) {
    return xythosService.getContentLength(arg0, arg1);
  }

  public String getContentType(String arg0, String arg1) {
    return xythosService.getContentType(arg0, arg1);
  }

  public String getContentUri(String arg0, String arg1) {
    return xythosService.getContentUri(arg0, arg1);
  }

  public XythosDocument getDocument(String arg0, String arg1) {
    return xythosService.getDocument(arg0, arg1);
  }

  public byte[] getFileContent(String arg0, String arg1) {
    return xythosService.getFileContent(arg0, arg1);
  }

  public Map<String, Object> getFileProperties(String arg0, String arg1) {
    return xythosService.getFileProperties(arg0, arg1);
  }

  public Map<String, String> getProperties() {
    return xythosService.getProperties();
  }

  public boolean ping() {
    return xythosService.ping();
  }

  public void removeDocument(String arg0, String arg1) {
    xythosService.removeDocument(arg0, arg1);
  }

  public String saveFile(String arg0, String arg1, byte[] arg2, String arg3, String arg4,
      String arg5) {
    return xythosService.saveFile(arg0, arg1, arg2, arg3, arg4, arg5);
  }

  public void toggleMember(String arg0, String arg1) {
    xythosService.toggleMember(arg0, arg1);
  }

  public void updateFile(String arg0, byte[] arg1, Map<String, Object> arg2, String arg3) {
    xythosService.updateFile(arg0, arg1, arg2, arg3);
  }
  
  /**
   * When the component gets activated we retrieve the OSGi properties.
   *
   * @param context
   */
  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext context) {
    // Get the properties from the console.
    Dictionary props = context.getProperties();
    if (props.get("xythosHost") != null) {
      xythosHost = props.get("xythosHost").toString();
    }
  }

}
