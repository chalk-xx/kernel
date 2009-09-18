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
package org.sakaiproject.kernel.files.search;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.kernel.api.search.SearchPropertyProvider;

import java.util.Map;

/**
 * Provides properties to process the search
 * 
 * @scr.component immediate="true" label="FileSearchPropertyProvider"
 *                description="Property provider for file searches"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.provider" value="Files"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchPropertyProvider"
 */
public class FileSearchPropertyProvider implements SearchPropertyProvider {

  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    
    // Tags    
    String tags = "";
    RequestParameter[] tagsParams = request.getRequestParameters("sakai:tags");
    if (tagsParams != null) {
      StringBuilder sb = new StringBuilder(" and (");
      
      for (RequestParameter tag : tagsParams) {
        sb.append("sakai:tags=\"");
        sb.append(tag.getString());
        sb.append("\" and ");
      }
      tags = sb.substring(0, sb.length() - 5) + ")";      
    }
    propertiesMap.put("_tags", tags);
    
    
    
    // Sorting order
    RequestParameter sortOnParam = request.getRequestParameter("sortOn");
    RequestParameter sortOrderParam = request.getRequestParameter("sortOrder");
    String sortOn = "sakai:filename";
    String sortOrder = "ascending";
    if (sortOrderParam != null && (sortOrderParam.getString().equals("ascending") || sortOrderParam.getString().equals("descending"))) {
      sortOrder = sortOrderParam.getString();
    }    
    if (sortOnParam != null) {
      sortOn = sortOnParam.getString();
    }    
    String order = " order by @" + sortOn + " " + sortOrder;
    propertiesMap.put("_order", order);
    
  }
}
