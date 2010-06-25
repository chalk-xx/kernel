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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.IOUtils;
import org.sakaiproject.nakamura.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "WidgetizeServlet", shortDescription = "Fetch all the resources for a widget.", description = { "Fetch all the resources of a widget in one request." }, bindings = { @ServiceBinding(type = BindingType.TYPE, selectors = { @ServiceSelector(name = "widgetize") }, extensions = { @ServiceExtension(name = "json") }) }, methods = { @ServiceMethod(name = "GET", description = { "Fetches all the resources and specified language bundles for a widget in one request." }, parameters = { @ServiceParameter(name = "locale", description = "What locale should be used for the language bundle. This should be in the ISO3 format. ie: en_US or zh_CN.") }, response = {
    @ServiceResponse(code = 200, description = {
        "A JSON response will be streamed back. This exists out of 2 parts",
        "<ul><li>Language bundles</li><li>Widget files</li></ul>",
        "Language bundles",
        "There will be a key in the json object called 'bundles'. This key will contain an object that will contain 2 child-objects.<br />The first one will always be 'default' which is the output for the default language bundle of a widget.<br />The other one will be the one specified in the request parameter (or the server default if none has been specified.)<br /> If the language bundle could not be found an empty object will be returned.",
        "Widget files",
        "The servlet will walk down the tree and try to get the content of each resource. It will then try to get the mimetype of this file. If the mimetype is in the list of allowed mimetypes it will be outputted. This list can be modified in the felix admin console." }),
    @ServiceResponse(code = 403, description = { "The resource where this action is performed on is not a valid widget." }) }

) })
@SlingServlet(resourceTypes = { "sling/servlet/default" }, methods = { "GET" }, selectors = { "widgetize" }, extensions = { "json" }, generateService = true, generateComponent = false)
@Component(metatype = true, immediate = true)
public class WidgetizeServlet extends SlingSafeMethodsServlet {
  private static final long serialVersionUID = -8498483459709451448L;

  @Property(value = { "bundles" }, description = "The directorynames that should be ignored when outputting a widget.")
  static final String BATCH_IGNORE_NAMES = "sakai.batch.widgetize.ignore_names";

  @Property(value = { "text/plain", "text/css", "text/html", "application/json",
      "application/xml" }, description = "The mimetypes of files that should be outputted.")
  static final String BATCH_VALID_MIMETYPES = "sakai.batch.widgetize.valid_mimetypes";

  @Reference
  protected transient CacheManagerService cacheManagerService;

  private static final String LOCALE_PARAM = "locale";

  /**
   * Directories that should not be included in the output.
   */
  private List<String> skipDirectories;

  private List<String> validMimetypes;

  private Detector detector;

  @SuppressWarnings("unchecked")
  @Activate
  protected void activate(Map properties) {
    AutoDetectParser parser = new AutoDetectParser();
    detector = parser.getDetector();

    modified(properties);
  }

  @SuppressWarnings("unchecked")
  @Modified
  protected void modified(Map properties) {
    init(properties);
  }

  @SuppressWarnings("unchecked")
  private void init(Map props) {
    String[] names = OsgiUtil.toStringArray(props.get(BATCH_IGNORE_NAMES), new String[0]);
    String[] types = OsgiUtil.toStringArray(props.get(BATCH_VALID_MIMETYPES),
        new String[0]);
    skipDirectories = Arrays.asList(names);
    validMimetypes = Arrays.asList(types);
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

    // Before we do anything else, we check if this request is occurring on an actual
    // widget.
    if (!checkValidWidget(request)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
          "The current resource is not a widget.");
      return;
    }

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    RequestParameter localeParam = request.getRequestParameter(LOCALE_PARAM);
    Locale locale = Locale.getDefault();
    if (localeParam != null) {
      String[] l = StringUtils.split(localeParam.getString(), '_');
      locale = new Locale(l[0], l[1]);
    }

    Resource resource = request.getResource();
    PrintWriter pw = response.getWriter();

    // Check if we have something in the cache.
    String cacheName = ResourceUtil.getName(resource) + "_" + locale.toString();
    Cache<String> cache = cacheManagerService.getCache(cacheName, CacheScope.INSTANCE);

    String content = cache.get("content");
    if (content != null) {
      // We stream directly from the cache
      pw.write(content);
      return;
    }

    // There is nothing in the cache, create it and put it in there.
    StringWriter sw = new StringWriter();
    ExtendedJSONWriter writer = new ExtendedJSONWriter(sw);
    try {
      writer.object();
      outputWidget(resource, writer, locale);
      writer.endObject();
    } catch (JSONException e) {
      throw new ServletException(e.getMessage(), e);
    }

    sw.flush();
    content = sw.toString();
    cache.put("content", content);

    // output everything
    pw.append(content);

  }

  /**
   * @param resource
   * @param writer
   * @param b
   * @throws JSONException
   */
  protected void outputWidget(Resource resource, ExtendedJSONWriter writer, Locale locale)
      throws JSONException {

    // Output language bundles
    writer.key("bundles");
    writer.object();
    outputLanguageBundle(resource, writer, "default");
    outputLanguageBundle(resource, writer, locale.toString());
    writer.endObject();

    // Output widget files
    Iterator<Resource> children = ResourceUtil.listChildren(resource);
    while (children.hasNext()) {
      Resource child = children.next();
      String childName = ResourceUtil.getName(child);
      // Check if we can output this resource.
      if (skipDirectories.contains(childName)) {
        continue;
      }
      writer.key(childName);
      outputResource(child, writer, true);
    }

  }

  /**
   * @param resource
   * @param writer
   * @param string
   * @throws JSONException
   */
  protected void outputLanguageBundle(Resource resource, ExtendedJSONWriter writer,
      String bundle) throws JSONException {
    // Bundle files are located at
    // - </path/to/widget>/bundles/default.json
    // - </path/to/widget>/bundles/en_US.json

    String path = resource.getPath() + "/bundles/" + bundle + ".json";
    Resource bundleResource = resource.getResourceResolver().getResource(path);

    writer.key(bundle);
    if (bundleResource == null || bundleResource instanceof NonExistingResource) {
      // If no bundle is found we output an empty object.
      writer.object();
      writer.endObject();
    } else {
      outputJsonResource(bundleResource, writer);
    }
  }

  /**
   * @param resource
   * @param writer
   * @throws JSONException
   */
  protected void outputJsonResource(Resource resource, ExtendedJSONWriter writer)
      throws JSONException {
    String content = "";
    try {
      InputStream stream = resource.adaptTo(InputStream.class);

      content = IOUtils.readFully(stream, "UTF-8");
      writer.valueMap(new JsonValueMap(content));
    } catch (JSONException e) {
      writer.value(content);
    } catch (IOException e) {
      // If everything failed horribly we output an empty object.
      writer.object();
      writer.endObject();
    }
  }

  /**
   * @param resource
   * @param writer
   * @throws JSONException
   */
  protected void outputResource(Resource resource, ExtendedJSONWriter writer,
      boolean fetchChildren) throws JSONException {
    writer.object();

    InputStream stream = resource.adaptTo(InputStream.class);
    if (stream != null) {
      try {
        writer.key("content");

        // Get the mimetype of this stream
        BufferedInputStream bufStream = new BufferedInputStream(stream);
        Metadata metadata = new Metadata();
        MediaType type = detector.detect(bufStream, metadata);

        if (validMimetypes.contains(type.toString())) {

          // This Node (be it FsResource or not) is a valid file.
          // Output it.
          String content = IOUtils.readFully(bufStream, "UTF-8");
          if ("application/json".equals(type.toString())) {
            outputJsonResource(resource, writer);
          } else {
            writer.value(content);
          }
        } else {
          writer.value(false);
        }
      } catch (IOException e) {
        writer.value(false);
      }

    } else {
      if (fetchChildren) {
        Iterator<Resource> children = ResourceUtil.listChildren(resource);
        while (children.hasNext()) {
          Resource child = children.next();
          String childName = ResourceUtil.getName(child);
          // Check if we can output this resource.
          if (skipDirectories.contains(childName)) {
            continue;
          }
          writer.key(childName);
          outputResource(child, writer, true);
        }
      }
    }

    writer.endObject();
  }

  /**
   * @param request
   *          The request to check
   * @return Whether or not the request has been done on a valid Sakai widget.
   */
  protected boolean checkValidWidget(SlingHttpServletRequest request) {
    // We use resources rather than Nodes because most of the time the UI will use the
    // FsResource tool for development.
    Resource resource = request.getResource();
    ResourceResolver resolver = request.getResourceResolver();
    String widgetName = ResourceUtil.getName(resource);

    // Check for a configuration file with the same name.
    String configPath = request.getResource().getPath() + "/config.json";
    Resource configResource = resolver.getResource(configPath);

    if (configResource == null || configResource instanceof NonExistingResource) {
      return false;
    }

    try {
      // All widgets should have a configuration file with the same name as the widget.
      // According to the Widget specification:
      // http://confluence.sakaiproject.org/display/3AK/Sakai+3+Widget+Specification+Proposal
      // Get the content of this resource and parse it to json.
      StringWriter sw = new StringWriter();
      ExtendedJSONWriter jsonWriter = new ExtendedJSONWriter(sw);
      outputJsonResource(configResource, jsonWriter);
    } catch (JSONException e) {
      // If this file cannot be parsed to JSON than it isn't valid.
      return false;
    }

    // TODO have some better checks.

    return true;
  }

}
