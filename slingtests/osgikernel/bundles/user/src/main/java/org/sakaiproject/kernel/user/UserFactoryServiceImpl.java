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
package org.sakaiproject.kernel.user;

import static org.sakaiproject.kernel.api.user.UserConstants.DEFAULT_HASH_LEVELS;

import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.configuration.ConfigurationListener;
import org.sakaiproject.kernel.api.configuration.ConfigurationService;
import org.sakaiproject.kernel.api.configuration.KernelConstants;
import org.sakaiproject.kernel.api.user.UserConstants;
import org.sakaiproject.kernel.api.user.UserFactoryService;
import org.sakaiproject.kernel.util.MapUtils;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Provides paths for user information. In the past this implementation created users, but
 * that is done by sling now.
 * 
 * @scr.component immediate="true" label="Sakai User Factory Service"
 *                description="Provides User related paths" name="org.sakaiproject.kernel.api.user.UserFactoryService"
 * @scr.property name="service.description" value="Implementation of the User Service that provides setup properties for all kernel bundles."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.sakaiproject.kernel.api.user.UserFactoryService"
 * @scr.reference name="configurationService" interface="org.sakaiproject.kernel.api.configuration.ConfigurationService"
 *                bind="bindConfigurationService" unbind="unbindConfigurationService"
 * 
 * 
 */
public class UserFactoryServiceImpl implements UserFactoryService, ConfigurationListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserFactoryServiceImpl.class);
  private String userEnvironmentBase;
  private Map<String, String> userTemplateMap;
  private String defaultTemplate;
  long entropy = System.currentTimeMillis();
  private String sharedPrivatePathBase;
  private String defaultProfileTemplate;
  private Map<String, String> profileTemplateMap;
  private String privatePathBase;

  private ConfigurationService configurationService;
  

  /**
   * 
   */
  public UserFactoryServiceImpl() {
  }

  /**
   *
   */
  protected void activate(ComponentContext ctx) {
    configurationService.addListener(this);
    update(configurationService.getProperties());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.user.UserFactoryService#getUserEnvPath(java.lang.String)
   */
  public String getUserEnvPath(String uuid) {
    return getUserEnvironmentBasePath(uuid) + KernelConstants.USERENV;
  }

  /**
   * @param uuid
   * @return
   */
  public String getUserEnvironmentBasePath(String uuid) {
    String prefix = PathUtils.getUserPrefix(uuid,DEFAULT_HASH_LEVELS);
    return PathUtils.normalizePath(userEnvironmentBase + "/" + prefix);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.user.UserFactoryService#getUserEnvTemplate(java.lang.String)
   */
  public String getUserEnvTemplate(String userType) {
    if (userType == null) {
      return defaultTemplate;
    }
    String template = userTemplateMap.get(userType);
    if (template == null) {
      return defaultTemplate;
    }
    return template;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.user.UserFactoryService#getUserPathPrefix(java.lang.String)
   */
  public String getUserPathPrefix(String uuid) {
    return PathUtils.getUserPrefix(uuid,DEFAULT_HASH_LEVELS);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.user.UserFactoryService#getUserProfilePath(java.lang.String)
   */
  public String getUserProfilePath(String uuid) {

    return getUserSharedPrivatePath(uuid) + "/" + UserConstants.AUTH_PROFILE;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.user.UserFactoryService#getUserProfileTempate(java.lang.String)
   */
  public String getUserProfileTempate(String userType) {
    if (userType == null) {
      return defaultProfileTemplate;
    }
    String template = profileTemplateMap.get(userType);
    if (template == null) {
      return defaultProfileTemplate;
    }
    return template;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.user.UserFactoryService#getUserPrivatePath(java.lang.String)
   */
  public String getUserPrivatePath(String uuid) {
    LOGGER.info("User private Path ["+privatePathBase+"] ["+uuid+"]" );
    return PathUtils.normalizePath("_user/private" + "/" + PathUtils.getUserPrefix(uuid,DEFAULT_HASH_LEVELS));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.user.UserFactoryService#getUserSharedPrivatePath(java.lang.String)
   */
  public String getUserSharedPrivatePath(String uuid) {
    return PathUtils.normalizePath("_user/public" + "/" + PathUtils.getUserPrefix(uuid,DEFAULT_HASH_LEVELS));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.user.UserFactoryService#getMessagesPath(java.lang.String)
   */
  public String getMessagesPath(String id) {
    return getUserPrivatePath(id) + KernelConstants.MESSAGES;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.user.UserFactoryService#getNewMessagePath(java.lang.String)
   */
  public String getNewMessagePath(String id) {
    return getMessagesPath(id) + PathUtils.getMessagePath();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.configuration.ConfigurationListener#update(java.util.Map)
   */
  public void update(Map<String, String> config) {
    if (config.containsKey(KernelConstants.JCR_DEFAULT_TEMPLATE)) {
      defaultTemplate = config.get(KernelConstants.JCR_DEFAULT_TEMPLATE);
    }
    if (config.containsKey(KernelConstants.JCR_PROFILE_DEFAUT_TEMPLATES)) {
      defaultProfileTemplate = config.get(KernelConstants.JCR_PROFILE_DEFAUT_TEMPLATES);
    }
    if (config.containsKey(KernelConstants.JCR_USERENV_BASE)) {
      userEnvironmentBase = config.get(KernelConstants.JCR_USERENV_BASE);
    }
    if (config.containsKey(KernelConstants.PRIVATE_PATH_BASE)) {

      privatePathBase = config.get(KernelConstants.PRIVATE_PATH_BASE);
      LOGGER.info("Loaded Private space as "+privatePathBase);
    }
    if (config.containsKey(KernelConstants.PRIVATE_SHARED_PATH_BASE)) {
      sharedPrivatePathBase = config.get(KernelConstants.PRIVATE_SHARED_PATH_BASE);
      LOGGER.info("LoadedShared space as "+sharedPrivatePathBase);
    }
    if (config.containsKey(KernelConstants.JCR_USERENV_TEMPLATES)) {

      userTemplateMap = MapUtils.convertToImmutableMap(config
          .get(KernelConstants.JCR_USERENV_TEMPLATES));
    }
    if (config.containsKey(KernelConstants.JCR_PROFILE_TEMPLATES)) {
      profileTemplateMap = MapUtils.convertToImmutableMap(config
          .get(KernelConstants.JCR_PROFILE_TEMPLATES));
    }
  }

  protected void bindConfigurationService(ConfigurationService configurationService) {
    if (this.configurationService != null) {
      this.configurationService.removeListener(this);
    }
    this.configurationService = configurationService;
    update(this.configurationService.getProperties());
    this.configurationService.addListener(this);
  }

  protected void unbindConfigurationService(ConfigurationService configurationService) {
    configurationService.removeListener(this);
    this.configurationService = null;
  }
}
