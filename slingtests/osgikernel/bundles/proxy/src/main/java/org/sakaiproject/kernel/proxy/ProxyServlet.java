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
package org.sakaiproject.kernel.proxy;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceExtension;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceParameter;
import org.sakaiproject.kernel.api.doc.ServiceResponse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Servlet;

/**
 * This servlet will proxy through to other websites and fetch its data.
 * 
 * @offscr.component immediate="true" label="%cropit.get.operation.name"
 *                   description="%cropit.get.operation.description"
 * @offscr.service interface="javax.servlet.Servlet"
 * @offscr.property name="sling.servlet.paths" value="/system/proxy"
 * @offscr.property name="sling.servlet.methods" value="POST"
 * @offscr.property name="sling.servlet.extensions" value="json"
 */
// FIXME: remove this servlet at some point.
@Service(value = Servlet.class)
@SlingServlet(generateComponent = true, generateService = true, extensions = { "json" }, methods = { "POST" }, paths = { "/system/proxy" })
@ServiceDocumentation(name = "ProxyServlet", shortDescription = "TBD", description = "Please use the content proxy service by placing content in /var/proxy/* rather than using this method, which has now been disabled.", methods = { @ServiceMethod(name = "POST", description = "Please use the content proxy service by placing content in /var/proxy/* rather than using this method, which has now been disabled.", parameters = {
		@ServiceParameter(name = "user", description = ""),
		@ServiceParameter(name = "url", description = ""),
		@ServiceParameter(name = "method", description = ""),
		@ServiceParameter(name = "post", description = ""),
		@ServiceParameter(name = "timeout", description = ""),
		@ServiceParameter(name = "password", description = "") }, response = { @ServiceResponse(code = 400, description = "Please use the content proxy service by placing content in /var/proxy/* rather than using this method, which has now been disabled.") }) }, bindings = { @ServiceBinding(type = BindingType.PATH, bindings = "/system/proxy", extensions = { @ServiceExtension(name = "json") }) })
public class ProxyServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 2642777605571473702L;
  private static final String PARAMETER_USER = "user";
  private static final String PARAMETER_URL = "url";
  private static final String PARAMETER_METHOD = "method";
  private static final String PARAMETER_POST = "post";
  private static final String PARAMETER_TIMEOUT = "timeout";
  private static final String PARAMETER_PASSWORD = "password";

  private static final String AUTHORIZATION = "Authorization";
  private static final String LOCATION = "Location";

  /**
   * Perform the actual request. {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @SuppressWarnings(value={"deprecation"})
  protected void doPost(SlingHttpServletRequest req, SlingHttpServletResponse resp)
      throws IOException {
    
    if ( true ) {
      resp.sendError(400, "Please use the content proxy service by placing content in /var/proxy/* rather than using this method, which has now been disabled.");
      return;
    }
    
    URL url;
    url = null;
    String user = null, password = null, method = "GET", post = null;
    int timeout = 0;

    Map<String, String> headers = new HashMap<String, String>();
    for (Object anEntrySet : req.getParameterMap().entrySet()) {
      Map.Entry<?, ?> header = (Map.Entry<?, ?>) anEntrySet;
      String key = (String) header.getKey();
      String value = ((String[]) header.getValue())[0];
      if (PARAMETER_USER.equals(key)) {
        user = value;
      } else if (PARAMETER_PASSWORD.equals(key)) {
        password = value;
      } else if (PARAMETER_TIMEOUT.equals(key)) {
        timeout = Integer.parseInt(value);
      } else if (PARAMETER_METHOD.equals(key)) {
        method = value;
      } else if (PARAMETER_POST.equals(key)) {
        post = URLDecoder.decode(value);
      } else if (PARAMETER_URL.equals(key)) {
        url = new URL(value);
      } else {
        headers.put(key, value);
      }
    }

    if (url != null) {

      String digest = null;
      if (user != null && password != null) {
        digest = "Basic "
            + new String(Base64.encodeBase64((user + ":" + password).getBytes()));
      }

      boolean foundRedirect = false;
      do {

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if (digest != null) {
          urlConnection.setRequestProperty(AUTHORIZATION, digest);
        }
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setUseCaches(false);
        urlConnection.setInstanceFollowRedirects(false);
        urlConnection.setRequestMethod(method);
        if (timeout > 0) {
          urlConnection.setConnectTimeout(timeout);
        }

        // set headers
        for (Entry<String, String> header : headers.entrySet()) {
          urlConnection.setRequestProperty(header.getKey(), header.getValue());
        }

        // send post
        if (post != null) {
          OutputStreamWriter outRemote = new OutputStreamWriter(urlConnection
              .getOutputStream());
          outRemote.write(post);
          outRemote.flush();
        }

        // get content type
        String contentType = urlConnection.getContentType();
        if (contentType != null) {
          resp.setContentType(contentType);
        }

        // get reponse code
        int responseCode = urlConnection.getResponseCode();

        if (responseCode == 302) {
          // follow redirects
          String location = urlConnection.getHeaderField(LOCATION);
          url = new URL(location);
          foundRedirect = true;
        } else {
          resp.setStatus(responseCode);
          BufferedInputStream in;
          if (responseCode == 200 || responseCode == 201) {
            in = new BufferedInputStream(urlConnection.getInputStream());
          } else {
            in = new BufferedInputStream(urlConnection.getErrorStream());
          }

          // send output to client
          BufferedOutputStream out = new BufferedOutputStream(resp.getOutputStream());
          int c;
          while ((c = in.read()) >= 0) {
            out.write(c);
          }
          out.flush();
        }
      } while (foundRedirect);

    } else {
      resp.sendError(SlingHttpServletResponse.SC_BAD_REQUEST,
          "Please specify a URL in the " + PARAMETER_URL + " parameter");
    }

    return;

  }

}