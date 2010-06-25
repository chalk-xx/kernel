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
package org.sakaiproject.nakamura.batch;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "WidgetsServlet", description = "Gives a list of all the known widgets in the system.", shortDescription = "List all the widgets", methods = { @ServiceMethod(parameters = {}, response = {
    @ServiceResponse(code = 200, description = {
        "Will output a JSON object with all the widgets in the system.",
        "This servlet will only check the preconfigured locations. These can be modified in the felix admin console panel. The folder should be the toplevel folder that contains the widgets. Each subfolder should represent a widget and should contain a 'config.json' file.",
        "In the JSON response, each key represents a widgetname and will have the content of the 'config.json' file outputted in it." }),
    @ServiceResponse(code = 500, description = { "The servlet is unable to produce a proper JSON output." }) }) }, bindings = { @ServiceBinding(type = BindingType.PATH, bindings = { "/var/widgets" }) })
@SlingServlet(methods = { "GET" }, paths = { "/var/widgets" }, generateComponent = false, generateService = true)
@Component(metatype = true, immediate = true)
public class WidgetsServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -4113451154211163118L;
  private static final Logger LOGGER = LoggerFactory.getLogger(WidgetsServlet.class);

  @Property(value = { "/devwidgets" }, cardinality = 2147483647, description = "The directorynames that contain widgets. These have to be absolute paths in JCR.")
  static final String WIDGET_FOLDERS = "sakai.batch.widgets.widget_folders";
  private List<String> widgetFolders;

  @SuppressWarnings("unchecked")
  @Activate
  protected void activate(Map properties) {
    modified(properties);
  }

  @SuppressWarnings("unchecked")
  @Modified
  protected void modified(Map properties) {
    init(properties);
  }

  @SuppressWarnings("unchecked")
  private void init(Map props) {
    String[] folders = OsgiUtil.toStringArray(props.get(WIDGET_FOLDERS), new String[0]);
    widgetFolders = Arrays.asList(folders);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // The resolver that can be used to resolve widget resources (JcrNodeResource or
    // FsResource)
    ResourceResolver resolver = request.getResourceResolver();

    // We will store all the found widgets in this map.
    // The key will be the name of widget.
    Map<String, JsonValueMap> validWidgets = new HashMap<String, JsonValueMap>();
    for (String folder : widgetFolders) {
      processWidgetFolder(folder, resolver, validWidgets);
    }

    // Ensure that we're sending out proper json.
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    // Write the whole map
    ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
    try {
      writer.object();
      for (Entry<String, JsonValueMap> entry : validWidgets.entrySet()) {
        writer.key(entry.getKey());
        writer.valueMap(entry.getValue());
      }
      writer.endObject();
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to construct proper JSON.");
    }

  }

  /**
   * Processes a widget folder. Every widget (with a valid json config) will be placed in
   * the validWidgets map.
   * 
   * @param folder
   *          The absolute path in JCR (or FsResource) to process.
   * @param resolver
   *          The {@link ResourceResolver} that can be used to resolve resources.
   * @param validWidgets
   *          The hashmap where the widgets should be placed in to.
   */
  protected void processWidgetFolder(String folder, ResourceResolver resolver,
      Map<String, JsonValueMap> validWidgets) {
    Resource folderResource = resolver.getResource(folder);

    // List all the subfolders (these should all be widgets.)
    Iterator<Resource> widgets = ResourceUtil.listChildren(folderResource);
    while (widgets.hasNext()) {
      Resource widget = widgets.next();
      String widgetName = ResourceUtil.getName(widget);
      // Get the config for this widget.
      // If none is found or isn't valid JSON then it is ignored.
      String configPath = widget.getPath() + "/config.json";
      Resource config = resolver.getResource(configPath);
      if (config != null && !(config instanceof NonExistingResource)) {
        // Try to parse it to JSON.
        try {
          InputStream stream = config.adaptTo(InputStream.class);
          JsonValueMap map = new JsonValueMap(stream);
          validWidgets.put(widgetName, map);
        } catch (Exception e) {
          LOGGER.warn("Exception when trying to parse the 'config.json' for "
              + widgetName, e);
        }
      }
    }
  }

}
