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
package org.sakaiproject.kernel.resource.version;

import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestPathInfo;

/**
 * 
 */
public class VersionRequestPathInfo implements RequestPathInfo {

  private final String selectorString;

  private final String[] selectors;

  private final String extension;

  private final String suffix;

  private final String resourcePath;

  private final static String[] NO_SELECTORS = new String[0];

  public VersionRequestPathInfo(RequestPathInfo sourceRequestPathInfo) {

    resourcePath = sourceRequestPathInfo.getResourcePath();
    suffix = sourceRequestPathInfo.getSuffix();
    String pathToSplit = sourceRequestPathInfo.getSelectorString();
    int i = pathToSplit.indexOf('.', 1);
    if (i < 0) {
      pathToSplit = pathToSplit.substring(i + 1);
      selectorString = null;
      selectors = NO_SELECTORS;
      extension = null;
    } else {

      int lastDot = pathToSplit.lastIndexOf('.');

      if (lastDot <= 1) {

        // no selectors if only extension exists or selectors is empty
        selectorString = null;
        selectors = NO_SELECTORS;

      } else {

        // no selectors if splitting would give an empty array
        String tmpSel = pathToSplit.substring(1, lastDot);
        selectors = tmpSel.split("\\.");
        selectorString = (selectors.length > 0) ? tmpSel : null;

      }

      // extension only if lastDot is not trailing
      extension = (lastDot + 1 < pathToSplit.length()) ? pathToSplit
          .substring(lastDot + 1) : null;
    }
  }

  private VersionRequestPathInfo(String resourcePath, String selectorString,
      String extension, String suffix) {
    this.resourcePath = resourcePath;
    this.selectorString = selectorString;
    this.selectors = (selectorString != null) ? selectorString.split("\\.")
        : NO_SELECTORS;
    this.extension = extension;
    this.suffix = suffix;
  }

  public VersionRequestPathInfo merge(RequestPathInfo baseInfo) {
    if (getExtension() == null) {
      return new VersionRequestPathInfo(getResourcePath(), baseInfo.getSelectorString(),
          baseInfo.getExtension(), baseInfo.getSuffix());
    }

    return this;
  }

  public VersionRequestPathInfo merge(RequestDispatcherOptions options) {

    if (options != null) {

      // set to true if any option is set
      boolean needCreate = false;

      // replacement selectors
      String selectors = options.getReplaceSelectors();
      if (selectors != null) {
        needCreate = true;
      } else {
        selectors = getSelectorString();
      }

      // additional selectors
      String selectorsAdd = options.getAddSelectors();
      if (selectorsAdd != null) {
        if (selectors != null) {
          selectors += "." + selectorsAdd;
        } else {
          selectors = selectorsAdd;
        }
        needCreate = true;
      }

      // suffix replacement
      String suffix = options.getReplaceSuffix();
      if (suffix != null) {
        needCreate = true;
      } else {
        suffix = getSuffix();
      }

      if (needCreate) {
        return new VersionRequestPathInfo(getResourcePath(), selectors, getExtension(),
            suffix);
      }
    }

    return this;
  }

  @Override
  public String toString() {
    return "SlingRequestPathInfo: path='" + resourcePath + "'" + ", selectorString='"
        + selectorString + "'" + ", extension='" + extension + "'" + ", suffix='"
        + suffix + "'";
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
}
