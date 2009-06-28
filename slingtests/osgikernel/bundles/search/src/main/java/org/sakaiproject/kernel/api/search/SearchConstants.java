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
package org.sakaiproject.kernel.api.search;

/**
 * 
 */
public interface SearchConstants {
  /**
  *
  */
  public static final String JSON_RESULTS = "results";
  /**
  *
  */
  public static final String JSON_QUERY = "query";
  /**
  *
  */
  public static final String PARAMS_ITEMS_PER_PAGE = "items";
  /**
  *
  */
  public static final String PARAMS_PAGE = "page";
  /**
  *
  */
  public static final String TOTAL = "total";
  /**
  *
  */
  public static final String SAKAI_QUERY_LANGUAGE = "sakai:query-language";
  /**
  *
  */
  public static final String SAKAI_QUERY_TEMPLATE = "sakai:query-template";
  /**
 *
 */
  public static final String SAKAI_RESULTPROCESSOR = "sakai:resultprocessor";

  public static final String REG_PROCESSOR_NAMES = "sakai.search.processor";
  
  public static final String REG_PROVIDER_NAMES = "sakai.search.provider";
  public static final String SAKAI_PROPERTY_PROVIDER = "sakai:propertyprovider";

  /**
  *
  */
  public static final String SEARCH_RESULT_PROCESSOR = "SearchResultProcessor";
  /**
  *
  */
  public static final String SEARCH_PROPERTY_PROVIDER = "SearchPropertyProvider";
}
