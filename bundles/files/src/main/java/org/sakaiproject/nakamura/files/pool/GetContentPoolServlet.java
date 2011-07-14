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
package org.sakaiproject.nakamura.files.pool;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = { "GET" }, extensions = { "json" }, resourceTypes = { "sakai/pooled-content" })
@ServiceDocumentation(name = "GetContentPoolServlet documentation", okForVersion = "0.11",
  shortDescription = "Gets a JSON representation of a content pool item.",
  description = { "Gets a JSON representation of a content pool item.",
    "<pre>curl http://localhost:8080/p/hESoXumAT.json</pre>",
    "<pre>{\n    \"_previousBlockId\": \"UbGXYKGfEeCAXdkUrBABAw+\",\n    \"_lastModifiedBy\": \"admin\",\n    \"_previousVersion\": \"UbCF8KGfEeCAXdkUrBABAw+\",\n    \"_path\": \"hESoXumAT\",\n    \"sakai:fileextension\": \".png\",\n    \"_blockId\": \"UbGXYKGfEeCAXdkUrBABAw+\",\n    \"sakai:allowcomments\": \"true\",\n    \"sakai:pooled-content-viewer\": [\"anonymous\", \"everyone\"],\n    \"_id\": \"UchTsaGfEeCAXdkUrBABAw+\",\n    \"_bodyCreatedBy\": \"admin\",\n    \"sakai:pool-content-created-for\": \"suzy\",\n    \"sakai:pooled-content-file-name\": \"hero-zach-unmasked.png\",\n    \"_bodyCreated\": 1309276646363,\n    \"sakai:copyright\": \"creativecommons\",\n    \"_length\": 25606,\n    \"sakai:needsprocessing\": \"true\",\n    \"sakai:permissions\": \"public\",\n    \"_mimeType\": \"image/png\",\n    \"_bodyLastModifiedBy\": \"admin\",\n    \"_createdBy\": \"admin\",\n    \"_versionHistoryId\": \"UchTsKGfEeCAXdkUrBABAw+\",\n    \"sakai:showcomments\": \"true\",\n    \"sling:resourceType\": \"sakai/pooled-content\",\n    \"_created\": 1309276646351,\n    \"sakai:pooled-content-manager\": [\"suzy\"],\n    \"_bodyLastModified\": 1309276646363,\n    \"_lastModified\": 1309276646472,\n    \"_bodyLocation\": \"2011/5/-V/7P/mM/-V7PmMdM-QDHyHslMftAMF21H4s\"\n}</pre>"
  },
  bindings = @ServiceBinding(type = BindingType.TYPE, bindings = "sakai/pooled-content",
    extensions = { @ServiceExtension(name = "json") }),
  methods = {
    @ServiceMethod(name = "GET", description = "",
      response = {
        @ServiceResponse(code = HttpServletResponse.SC_OK, description = "Request has been processed successfully."),
        @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Resource could not be found."),
        @ServiceResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "Unable to process request due to a runtime error.")
      })
})
public class GetContentPoolServlet extends SlingSafeMethodsServlet implements OptingServlet {
  private static final long serialVersionUID = -382733858518678148L;
  private static final Logger LOGGER = LoggerFactory.getLogger(GetContentPoolServlet.class);

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Resource resource = request.getResource();

    // Check selectors.
    boolean isTidy = false;
    int recursion = 0;
    String[] selectors = request.getRequestPathInfo().getSelectors();
    if (selectors != null) {
      for (int i = 0; i < selectors.length; i++) {
        String selector = selectors[i];
        if ("tidy".equals(selector)) {
          isTidy = true;
        } else if (i == (selectors.length - 1)) {
          if ("infinity".equals(selector)) {
            recursion = -1;
          } else {
            try {
              recursion = Integer.parseInt(selector);
            } catch (NumberFormatException e) {
              LOGGER.warn("Invalid selector value '" + selector
                  + "'; defaulting recursion to 0");
            }
          }
        }
      }
    }

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
    writer.setTidy(isTidy);
    try {
      Content content = resource.adaptTo(Content.class);
      if ( content != null ) {
        ExtendedJSONWriter.writeContentTreeToWriter(writer, content, false,  recursion);
      } else {
        Node node = resource.adaptTo(Node.class);
        ExtendedJSONWriter.writeNodeTreeToWriter(writer, node, recursion);
      }

    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.info("Caught JSONException {}", e.getMessage());
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.info("Caught Repository {}", e.getMessage());
    }
  }

  /**
   * Do not interfere with the default servlet's handling of streaming data,
   * which kicks in if no extension has been specified was specified in the
   * request. (Sling servlet resolution uses a servlet's declared list of
   * "extensions" for score weighing, not for filtering.)
   *
   * @see org.apache.sling.api.servlets.OptingServlet#accepts(org.apache.sling.api.SlingHttpServletRequest)
   */
  public boolean accepts(SlingHttpServletRequest request) {
    return "json".equals(request.getRequestPathInfo().getExtension());
  }
}
