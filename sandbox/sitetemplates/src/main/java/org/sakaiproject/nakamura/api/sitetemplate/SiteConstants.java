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
package org.sakaiproject.nakamura.api.sitetemplate;

/**
 * Constants that will be used troughout the site templating engine.
 */
public interface SiteConstants {

  /**
   * The name for the top node that will contain all the authorizables that are part of
   * this site.
   */
  public static final String AUTHORIZABLES_SITE_NODENAME = "authorizables";

  /**
   * The name for the the node that represents a single authorizable in the {@see
   * SiteConstants.AUTHORIZABLES_SITE_NODENAME}.
   */
  public static final String AUTHORIZABLES_SITE_NODENAME_SINGLE = "authorizable";

  /*
   * Template variables.
   */

  /**
   * The resource type of a group node defined in the template.
   */
  public static final String RT_GROUPS = "sakai/template-group";

  /**
   * The property that defines what the name of a group should be when it gets created.
   */
  public static final String AUTHORIZABLES_SITE_PRINCIPAL_NAME = "sakai:template-group-principalname";

  /**
   * The property that defines what the managers of a group should be when it gets
   * created.
   */
  public static final String GROUPS_PROPERTY_MANAGERS = "sakai:template-group-managers";

  /**
   * The property that defines what the viewers of a group should be when it gets created.
   */
  public static final String GROUPS_PROPERTY_VIEWERS = "sakai:template-group-viewers";

  /**
   * The property that defines if this group will be a maintainer or not.
   */
  public static final String AUTHORIZABLES_SITE_IS_MAINTAINER = "sakai:template-group-issitemanager";

  /**
   * The resource type of an ACE node defined in the template.
   */
  public static final String RT_ACE = "sakai/template-ace";

  /**
   *
   */
  public static final String RT_SITE_AUTHORIZABLE = "sakai/site-group";

}