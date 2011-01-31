/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sakaiproject.nakamura.api.resource.lite;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.RequestProperty;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

public abstract class AbstractSparseCreateOperation extends AbstractSparsePostOperation {
  /**
   * The default node name generator
   */
  private final NodeNameGenerator defaultNodeNameGenerator;

  /**
   * utility class for generating node names
   */
  private NodeNameGenerator[] extraNodeNameGenerators;

  public AbstractSparseCreateOperation(NodeNameGenerator defaultNodeNameGenerator) {
    this.defaultNodeNameGenerator = defaultNodeNameGenerator;
  }

  public void setExtraNodeNameGenerators(NodeNameGenerator[] extraNodeNameGenerators) {
    this.extraNodeNameGenerators = extraNodeNameGenerators;
  }


  /**
   * Collects the properties that form the content to be written back to the repository.
   *
   * @throws RepositoryException
   *           if a repository error occurs
   * @throws ServletException
   *           if an internal error occurs
   */
  protected Map<String, SparseRequestProperty> collectContent(SlingHttpServletRequest request,
      HtmlResponse response, String contentPath) {

    boolean requireItemPrefix = requireItemPathPrefix(request);

    // walk the request parameters and collect the properties
    LinkedHashMap<String, SparseRequestProperty> reqProperties = new LinkedHashMap<String, SparseRequestProperty>();
    for (Map.Entry<String, RequestParameter[]> e : request.getRequestParameterMap()
        .entrySet()) {
      final String paramName = e.getKey();

      // do not store parameters with names starting with sling:post
      if (paramName.startsWith(SlingPostConstants.RP_PREFIX)) {
        continue;
      }
      // SLING-298: skip form encoding parameter
      if (paramName.equals("_charset_")) {
        continue;
      }
      // skip parameters that do not start with the save prefix
      if (requireItemPrefix && !hasItemPathPrefix(paramName)) {
        continue;
      }

      // ensure the paramName is an absolute property name
      String propPath = toPropertyPath(paramName, response);

      // @TypeHint example
      // <input type="text" name="./age" />
      // <input type="hidden" name="./age@TypeHint" value="long" />
      // causes the setProperty using the 'long' property type
      if (propPath.endsWith(SlingPostConstants.TYPE_HINT_SUFFIX)) {
        SparseRequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.TYPE_HINT_SUFFIX, contentPath);

        final RequestParameter[] rp = e.getValue();
        if (rp.length > 0) {
          prop.setTypeHintValue(rp[0].getString());
        }

        continue;
      }

      // @DefaultValue
      if (propPath.endsWith(SlingPostConstants.DEFAULT_VALUE_SUFFIX)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.DEFAULT_VALUE_SUFFIX, contentPath);

        prop.setDefaultValues(e.getValue());

        continue;
      }

      // SLING-130: VALUE_FROM_SUFFIX means take the value of this
      // property from a different field
      // @ValueFrom example:
      // <input name="./Text@ValueFrom" type="hidden" value="fulltext" />
      // causes the JCR Text property to be set to the value of the
      // fulltext form field.
      if (propPath.endsWith(SlingPostConstants.VALUE_FROM_SUFFIX)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.VALUE_FROM_SUFFIX, contentPath);

        // @ValueFrom params must have exactly one value, else ignored
        if (e.getValue().length == 1) {
          String refName = e.getValue()[0].getString();
          RequestParameter[] refValues = request.getRequestParameters(refName);
          if (refValues != null) {
            prop.setValues(refValues);
          }
        }

        continue;
      }

      // SLING-458: Allow Removal of properties prior to update
      // @Delete example:
      // <input name="./Text@Delete" type="hidden" />
      // causes the JCR Text property to be deleted before update
      if (propPath.endsWith(SlingPostConstants.SUFFIX_DELETE)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.SUFFIX_DELETE, contentPath);

        prop.setDelete(true);

        continue;
      }

      // SLING-455: @MoveFrom means moving content to another location
      // @MoveFrom example:
      // <input name="./Text@MoveFrom" type="hidden" value="/tmp/path" />
      // causes the JCR Text property to be set by moving the /tmp/path
      // property to Text.
      if (propPath.endsWith(SlingPostConstants.SUFFIX_MOVE_FROM)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.SUFFIX_MOVE_FROM, contentPath);

        // @MoveFrom params must have exactly one value, else ignored
        if (e.getValue().length == 1) {
          String sourcePath = e.getValue()[0].getString();
          prop.setRepositorySource(sourcePath, true);
        }

        continue;
      }

      // SLING-455: @CopyFrom means moving content to another location
      // @CopyFrom example:
      // <input name="./Text@CopyFrom" type="hidden" value="/tmp/path" />
      // causes the JCR Text property to be set by copying the /tmp/path
      // property to Text.
      if (propPath.endsWith(SlingPostConstants.SUFFIX_COPY_FROM)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.SUFFIX_COPY_FROM, contentPath);

        // @MoveFrom params must have exactly one value, else ignored
        if (e.getValue().length == 1) {
          String sourcePath = e.getValue()[0].getString();
          prop.setRepositorySource(sourcePath, false);
        }

        continue;
      }

      // SLING-1412: @IgnoreBlanks
      // @Ignore example:
      // <input name="./Text" type="hidden" value="test" />
      // <input name="./Text" type="hidden" value="" />
      // <input name="./Text@String[]" type="hidden" value="true" />
      // <input name="./Text@IgnoreBlanks" type="hidden" value="true" />
      // causes the JCR Text property to be set by copying the /tmp/path
      // property to Text.
      if (propPath.endsWith(SlingPostConstants.SUFFIX_IGNORE_BLANKS)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.SUFFIX_IGNORE_BLANKS, contentPath);

        if (e.getValue().length == 1) {
          prop.setIgnoreBlanks(true);
        }

        continue;
      }

      if (propPath.endsWith(SlingPostConstants.SUFFIX_USE_DEFAULT_WHEN_MISSING)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.SUFFIX_USE_DEFAULT_WHEN_MISSING, contentPath);

        if (e.getValue().length == 1) {
          prop.setUseDefaultWhenMissing(true);
        }

        continue;
      }

      // plain property, create from values
      RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath, null, contentPath);
      prop.setValues(e.getValue());
    }

    return reqProperties;
  }

  /**
   * Returns the <code>paramName</code> as an absolute (unnormalized) property path by
   * prepending the response path (<code>response.getPath</code>) to the parameter name if
   * not already absolute.
   */
  private String toPropertyPath(String paramName, HtmlResponse response) {
    if (!paramName.startsWith("/")) {
      paramName = ResourceUtil.normalize(response.getPath() + '/' + paramName);
    }

    return paramName;
  }

  /**
   * Returns the request property for the given property path. If such a request property
   * does not exist yet it is created and stored in the <code>props</code>.
   *
   * @param props
   *          The map of already seen request properties.
   * @param paramName
   *          The absolute path of the property including the <code>suffix</code> to be
   *          looked up.
   * @param suffix
   *          The (optional) suffix to remove from the <code>paramName</code> before
   *          looking it up.
   * @param contentPath TODO
   * @return The {@link RequestProperty} for the <code>paramName</code>.
   */
  private SparseRequestProperty getOrCreateRequestProperty(Map<String, SparseRequestProperty> props,
      String paramName, String suffix, String contentPath) {
    if (suffix != null && paramName.endsWith(suffix)) {
      paramName = paramName.substring(0, paramName.length() - suffix.length());
    }

    SparseRequestProperty prop = props.get(paramName);
    if (prop == null) {
      prop = new SparseRequestProperty(paramName, contentPath);
      props.put(paramName, prop);
    }

    return prop;
  }

  protected String generateName(SlingHttpServletRequest request, String basePath)
      throws StorageClientException {
    ContentManager contentManager = request.getResource().adaptTo(ContentManager.class);

    // SLING-1091: If a :name parameter is supplied, the (first) value of this parameter
    // is used unmodified as the name
    // for the new node. If the name is illegally formed with respect to JCR name
    // requirements, an exception will be
    // thrown when trying to create the node. The assumption with the :name parameter is,
    // that the caller knows what
    // he (or she) is supplying and should get the exact result if possible.
    RequestParameterMap parameters = request.getRequestParameterMap();
    RequestParameter specialParam = parameters.getValue(SlingPostConstants.RP_NODE_NAME);
    if (specialParam != null) {
      if (specialParam.getString() != null && specialParam.getString().length() > 0) {
        // If the path ends with a *, create a node under its parent, with
        // a generated node name
        basePath = basePath += "/" + specialParam.getString();

        String contentPath = removeAndValidateWorkspace(basePath);
        if (contentManager.exists(contentPath)) {
          throw new StorageClientException("Collision in node names for path=" + basePath);
        }

        return basePath;
      }
    }

    // no :name value was supplied, so generate a name
    boolean requirePrefix = requireItemPathPrefix(request);

    String generatedName = null;
    if (extraNodeNameGenerators != null) {
      for (NodeNameGenerator generator : extraNodeNameGenerators) {
        generatedName = generator.getNodeName(request, basePath, requirePrefix,
            defaultNodeNameGenerator);
        if (generatedName != null) {
          break;
        }
      }
    }
    if (generatedName == null) {
      generatedName = defaultNodeNameGenerator.getNodeName(request, basePath,
          requirePrefix, defaultNodeNameGenerator);
    }

    // If the path ends with a *, create a node under its parent, with
    // a generated node name
    basePath += "/" + generatedName;

    basePath = ensureUniquePath(contentManager, basePath);

    return basePath;
  }

  private String ensureUniquePath(ContentManager contentManager, String basePath)
      throws StorageClientException {

    String path = removeAndValidateWorkspace(basePath);

    // if resulting path exists, add a suffix until it's not the case
    // anymore
    if (contentManager.exists(path)) {
      for (int idx = 0; idx < 1000; idx++) {
        String newPath = path + "_" + idx;
        if (!contentManager.exists(newPath)) {
          basePath = basePath + "_" + idx;
          path = newPath;
          break;
        }
      }
    }

    // if it still exists there are more than 1000 nodes ?
    if (contentManager.exists(path)) {
      throw new StorageClientException("Collision in generated node names for path="
          + basePath);
    }

    return basePath;
  }

}
