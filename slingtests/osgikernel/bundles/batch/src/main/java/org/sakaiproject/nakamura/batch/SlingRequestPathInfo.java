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
package org.sakaiproject.nakamura.batch;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Sling request URI parser that provides SlingRequestPathInfo for the
 * current request, based on the path of the Resource. The values provided by
 * this depend on the Resource.getPath() value, as the ResourceResolver might
 * use all or only part of the request URI path to locate the resource (see also
 * SLING-60 ). What we're after is the remainder of the path, the part that was
 * not used to locate the Resource, and we split that part in different
 * subparts: selectors, extension and suffix.
 *
 * @see SlingRequestPathInfoTest for a number of examples.
 */
public class SlingRequestPathInfo implements RequestPathInfo {

    private final String selectorString;

    private final String[] selectors;

    private final String extension;

    private final String suffix;

    private final String resourcePath;

    private Resource resource;

    private final static String[] NO_SELECTORS = new String[0];

    public SlingRequestPathInfo(String pathToParse, ResourceResolver resourceResolver)
    {
      String[] parts = pathToParse.split("/");
      suffix = parts[parts.length - 1];

      int lastDot = suffix.lastIndexOf('.');

      if (lastDot <= 1) {
          selectorString = null;
          selectors = NO_SELECTORS;
      } else {
          // no selectors if splitting would give an empty array
          String tmpSel = suffix.substring(0, lastDot);
          int start = tmpSel.indexOf('.');
          if (start > -1) {
            selectors = tmpSel.substring(start).split("\\.");
            selectorString = (selectors.length > 0) ? tmpSel : null;
          } else {
            selectors = NO_SELECTORS;
            selectorString = null;
          }
      }

      // extension only if lastDot is not trailing
      extension = (lastDot > -1 && lastDot + 1 < suffix.length())
              ? suffix.substring(lastDot + 1)
              : null;
      resourcePath = resolveResourcePath(pathToParse, resourceResolver);
    }

    private String resolveResourcePath(String pathToParse, ResourceResolver resourceResolver) {
      if ((resource = resourceResolver.getResource(pathToParse)) != null) {
        return pathToParse;
      }
      if (extension != null) {
        pathToParse = pathToParse.substring(0, pathToParse.length() - extension.length() - 1);
        if ((resource = resourceResolver.getResource(pathToParse)) != null)
          return pathToParse;
      }
      if (selectorString != null) {
        pathToParse = pathToParse.substring(0, pathToParse.length() - selectorString.length() - 1);
        if ((resource = resourceResolver.getResource(pathToParse)) != null)
          return pathToParse;
      }
      int lastSlash = pathToParse.lastIndexOf("/");
      while (lastSlash > -1) {
        pathToParse = pathToParse.substring(0, lastSlash);
        if ((resource = resourceResolver.getResource(pathToParse)) != null) {
          return pathToParse;
        }
      }
      if ((resource = resourceResolver.getResource(pathToParse)) != null) {
        return pathToParse;
      }
      return null;
    }

    @Override
    public String toString() {
        return "SlingRequestPathInfo: path='" + resourcePath + "'"
            + ", selectorString='" + selectorString + "'" + ", extension='"
            + extension + "'" + ", suffix='" + suffix + "'";
    }

    public String getExtension() {
        return extension;
    }

    public String[] getSelectors() {
        return selectors;
    }

    public String getSelectorString() {
        return selectorString;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public Resource getResource() {
      return resource;
    }
}
