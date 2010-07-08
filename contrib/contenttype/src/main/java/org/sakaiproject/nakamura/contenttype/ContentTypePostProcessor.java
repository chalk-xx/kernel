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
package org.sakaiproject.nakamura.contenttype;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Constants;
import org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Retrieves the content type of a requested URL.
 */
@Component
@Service
@Properties(value = {
    @Property(name = Constants.SERVICE_VENDOR, value = "Sakai Foundation"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Post processor to return content type of requested URL.") })
public class ContentTypePostProcessor implements ProxyPostProcessor {
  public static final String NAME = "contenttype";

  protected static final String CONTENT_TYPE_HEADER = "Content-Type";
  protected static final String CONTENT_TYPE_KEY = "contentType";

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor#process(org.apache.sling.api.SlingHttpServletResponse,
   *      org.sakaiproject.nakamura.api.proxy.ProxyResponse)
   */
  public void process(SlingHttpServletResponse response, ProxyResponse proxyResponse)
      throws IOException {
    String contentType = "";

    Map<String, String[]> headers = proxyResponse.getResponseHeaders();
    if (headers.containsKey(CONTENT_TYPE_HEADER)) {
      String[] header = headers.get(CONTENT_TYPE_HEADER);
      if (header != null) {
        contentType = headers.get(CONTENT_TYPE_HEADER)[0];
      }
    }

    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(CONTENT_TYPE_KEY, contentType);
      String output = jsonObject.toString();
      response.getWriter().print(output);

      response.setStatus(proxyResponse.getResultCode());
    } catch (JSONException je) {
      response.sendError(SC_INTERNAL_SERVER_ERROR, je.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor#getName()
   */
  public String getName() {
    return NAME;
  }

}
